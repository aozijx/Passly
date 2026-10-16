package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.AuthenticationRequester
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillCandidateQuery
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.AutofillCandidateRepository
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.autofill.port.MatchResult
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.CredentialMatch
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.MatchType
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverAutofillCandidatesTest {
    @Test
    fun disabledSourceDoesNotCreateOrQueryInteraction() = runBlocking {
        val fixture = fixture(settings = AutofillSettings(enabled = false))

        val outcome = fixture.operation(request())

        assertEquals(AutofillDiscoveryOutcome.Disabled, outcome)
        assertEquals(0, fixture.repository.searchCalls)
    }

    @Test
    fun requestWithoutCredentialFieldsIsUnsupported() = runBlocking {
        val fixture = fixture(hasCredentials = false)

        val outcome = fixture.operation(request())

        assertEquals(AutofillDiscoveryOutcome.UnsupportedFields, outcome)
        assertEquals(0, fixture.repository.searchCalls)
    }

    @Test
    fun lockedVaultRequiresUnlockBeforeMetadataQuery() = runBlocking {
        val fixture = fixture(vaultState = AuthenticationState.Locked)

        val outcome = fixture.operation(request())

        assertTrue(outcome is AutofillDiscoveryOutcome.RequiresUnlock)
        assertEquals(0, fixture.repository.searchCalls)
    }

    @Test
    fun unlockedVaultReturnsExplicitNoCandidates() = runBlocking {
        val fixture = fixture(candidates = emptyList())

        val outcome = fixture.operation(request())

        assertTrue(outcome is AutofillDiscoveryOutcome.NoCandidates)
        assertEquals(1, fixture.repository.searchCalls)
    }

    @Test
    fun legacyDiscoveryCarriesSavePreferenceIndependentlyFromCredentialManager() = runBlocking {
        val fixture = fixture(
            settings = AutofillSettings(
                requireAuthentication = false,
                savePromptsEnabled = false,
                credentialManagerEnabled = false,
            ),
        )

        val outcome = fixture.operation(request())

        assertTrue(outcome is AutofillDiscoveryOutcome.NoCandidates)
        outcome as AutofillDiscoveryOutcome.NoCandidates
        assertEquals(false, outcome.context.savePromptsEnabled)
        assertEquals(1, fixture.repository.searchCalls)
    }

    @Test
    fun unlockedVaultRecordsAndReturnsMetadataCandidates() = runBlocking {
        val candidate = candidate("entry-1")
        val fixture = fixture(candidates = listOf(candidate))

        val outcome = fixture.operation(request())

        assertTrue(outcome is AutofillDiscoveryOutcome.Candidates)
        outcome as AutofillDiscoveryOutcome.Candidates
        assertEquals(listOf(candidate), outcome.candidates)
        assertEquals(setOf(EntryId("entry-1")), outcome.snapshot.presentedCandidateIds)
    }

    private fun fixture(
        settings: AutofillSettings = AutofillSettings(requireAuthentication = false),
        hasCredentials: Boolean = true,
        vaultState: AuthenticationState = AuthenticationState.Authenticated(1L),
        candidates: List<AutofillCandidateSummary> = emptyList(),
    ): Fixture {
        val vault = FakeVault(vaultState)
        val authentication = NoOpAuthentication(vault)
        val store = AutofillInteractionStore()
        val lease = AutofillUnlockLease(authentication)
        val repository = RecordingCandidateRepository(candidates)
        val fieldMatchStrategy = object : FieldMatchStrategy {
            override fun match(request: AutofillRequest) = MatchResult(
                roleMap = mapOf("password" to FieldRole.PASSWORD),
                hasCredentials = hasCredentials,
            )
        }
        return Fixture(
            operation = DiscoverAutofillCandidates(
                settingsSource = FixedSettingsSource(settings),
                candidateRepository = repository,
                legacyFieldMatchStrategy = fieldMatchStrategy,
                credentialFieldMatchStrategy = fieldMatchStrategy,
                startInteraction = StartAutofillInteraction(store, lease, vault),
                interactionStore = store,
                finishInteraction = FinishAutofillInteraction(store, lease),
            ),
            repository = repository,
        )
    }

    private fun request() = AutofillRequest(
        packageName = "com.example.app",
        domain = "example.com",
        fields = listOf(
            AutofillField(
                id = "password",
                hints = setOf("password"),
                inputType = null,
                isFocused = true,
            ),
        ),
        source = AutofillSource.AUTOFILL_SERVICE,
    )

    private fun candidate(id: String) = AutofillCandidateSummary(
        entry = EntryListItem(
            identity = EntryIdentity(
                id = EntryId(id),
                type = EntryType.LOGIN,
                timestamps = EntryTimestamps(1L),
            ),
            profile = EntryProfile(title = "Example", username = "user"),
        ),
        match = CredentialMatch(MatchType.WEB_DOMAIN, domain = "example.com"),
    )

    private data class Fixture(
        val operation: DiscoverAutofillCandidates,
        val repository: RecordingCandidateRepository,
    )

    private class FixedSettingsSource(settings: AutofillSettings) : InteractionSettingsSource {
        override val interaction: Flow<InteractionSettings> = flowOf(InteractionSettings(autofill = settings))
    }

    private class RecordingCandidateRepository(
        private val candidates: List<AutofillCandidateSummary>,
    ) : AutofillCandidateRepository {
        var searchCalls = 0

        override suspend fun search(query: AutofillCandidateQuery): List<AutofillCandidateSummary> {
            searchCalls++
            return candidates
        }
    }

    private class FakeVault(state: AuthenticationState) : SecureSessionAccessState {
        private val mutableState = MutableStateFlow(state)
        override val authenticationState: StateFlow<AuthenticationState> = mutableState
        override fun isUnlocked(): Boolean = mutableState.value is AuthenticationState.Authenticated
    }

    private class NoOpAuthentication(
        private val vault: FakeVault,
    ) : AuthenticationRequester, SessionLockController {
        override suspend fun authenticate(request: AuthenticationRequest, input: AuthInput): AuthenticationResult =
            error("Authentication is not expected in discovery")

        override suspend fun lock(request: SessionLockRequest) = Unit
    }
}
