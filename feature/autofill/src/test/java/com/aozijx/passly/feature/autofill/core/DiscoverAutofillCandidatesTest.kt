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
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillOperationFailure
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
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.port.AutofillSettingsSource
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DiscoverAutofillCandidatesTest {
    @Test
    fun cancellationAfterCandidateQueryClosesInteraction() =
        runBlocking {
            val fixture = fixture(afterSearch = { currentCoroutineContext().cancel() })
            val discovery = async { fixture.operation(request().copy(domain = null)) }

            try {
                discovery.await()
                fail("Discovery should propagate cancellation")
            } catch (_: CancellationException) {
                assertTrue(fixture.store.activeIds().isEmpty())
            }
        }

    @Test
    fun disabledSourceDoesNotCreateOrQueryInteraction() =
        runBlocking {
            val fixture =
                fixture(
                    settings =
                        AutofillSettings(
                            enabled = false,
                            presentation = AutofillPresentation.FLOATING,
                            credentialManagerEnabled = true,
                            requireAuthentication = true,
                            strictDomainMatching = true,
                            otpAutofillEnabled = false,
                        ),
                )

            val outcome = fixture.discoverAfterWebConfirmation(request())

            assertEquals(AutofillDiscoveryOutcome.Disabled, outcome)
            assertEquals(0, fixture.repository.searchCalls)
        }

    @Test
    fun requestWithoutCredentialFieldsIsUnsupported() =
        runBlocking {
            val fixture = fixture(hasCredentials = false)

            val outcome = fixture.operation(request())

            assertEquals(AutofillDiscoveryOutcome.UnsupportedFields, outcome)
            assertEquals(0, fixture.repository.searchCalls)
        }

    @Test
    fun lockedVaultRequiresUnlockBeforeMetadataQuery() =
        runBlocking {
            val fixture = fixture(vaultState = AuthenticationState.Locked)

            val outcome = fixture.operation(request())

            assertTrue(outcome is AutofillDiscoveryOutcome.RequiresUnlock)
            assertEquals(0, fixture.repository.searchCalls)
        }

    @Test
    fun unlockedVaultReturnsExplicitNoCandidates() =
        runBlocking {
            val fixture = fixture(candidates = emptyList())

            val outcome = fixture.discoverAfterWebConfirmation(request())

            assertTrue(outcome is AutofillDiscoveryOutcome.NoCandidates)
            assertEquals(1, fixture.repository.searchCalls)
        }

    @Test
    fun websiteCandidateSearchUsesDomainWithoutBrowserPackageAssociation() =
        runBlocking {
            val siteCandidate = candidate("site-entry")
            val request = request().copy(packageName = "com.attacker.unassociated")
            val fixture = fixture(candidates = listOf(siteCandidate))

            val outcome = fixture.operation(request)

            assertTrue(outcome is AutofillDiscoveryOutcome.RequiresWebRecipientConfirmation)
            assertEquals(0, fixture.repository.searchCalls)

            outcome as AutofillDiscoveryOutcome.RequiresWebRecipientConfirmation
            fixture.store.confirmWebRecipient(
                outcome.snapshot.id,
                AutofillGrantContext(request.packageName, "example.com"),
                1L,
            )
            val confirmed = fixture.operation(request, interactionId = outcome.snapshot.id)

            assertEquals("example.com", fixture.repository.lastQuery?.webDomain)
            assertEquals(null, fixture.repository.lastQuery?.packageName)
            assertTrue(confirmed is AutofillDiscoveryOutcome.Candidates)
            assertEquals(siteCandidate, (confirmed as AutofillDiscoveryOutcome.Candidates).candidates.single())
        }

    @Test
    fun relaxedDomainMatchingIsPassedToCandidateSearchWithoutWideningInteractionScope() =
        runBlocking {
            val fixture =
                fixture(
                    settings =
                        AutofillSettings(
                            presentation = AutofillPresentation.FLOATING,
                            enabled = true,
                            credentialManagerEnabled = true,
                            requireAuthentication = false,
                            strictDomainMatching = false,
                            otpAutofillEnabled = false,
                        ),
                )

            val outcome =
                fixture.discoverAfterWebConfirmation(
                    request().copy(domain = "login.example.com"),
                )

            assertEquals(false, fixture.repository.lastQuery?.strictDomainMatching)
            assertEquals("login.example.com", fixture.repository.lastQuery?.webDomain)
            assertTrue(outcome is AutofillDiscoveryOutcome.NoCandidates)
            assertEquals("login.example.com", (outcome as AutofillDiscoveryOutcome.NoCandidates).snapshot.scope.webDomain)
        }

    @Test
    fun relaxedCandidatePolicyDoesNotResumeAnInteractionOnAnotherHost() =
        runBlocking {
            val fixture =
                fixture(
                    settings =
                        AutofillSettings(
                            presentation = AutofillPresentation.FLOATING,
                            enabled = true,
                            credentialManagerEnabled = true,
                            requireAuthentication = false,
                            strictDomainMatching = false,
                            otpAutofillEnabled = false,
                        ),
                )
            val first =
                fixture.discoverAfterWebConfirmation(
                    request().copy(domain = "login.example.com"),
                ) as AutofillDiscoveryOutcome.NoCandidates

            val resumed = fixture.operation(request().copy(domain = "shop.example.com"), interactionId = first.snapshot.id)

            assertEquals(
                AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.SCOPE_MISMATCH),
                resumed,
            )
            assertEquals(1, fixture.repository.searchCalls)
        }

    @Test
    fun nativeCandidateSearchUsesNormalizedApplicationId() =
        runBlocking {
            val fixture = fixture()

            fixture.operation(request().copy(packageName = " COM.EXAMPLE.APP ", domain = null))

            assertEquals("com.example.app", fixture.repository.lastQuery?.packageName)
            assertEquals(null, fixture.repository.lastQuery?.webDomain)
        }

    @Test
    fun invalidOriginsAndScopesFailBeforeStartingAnInteractionOrQuery() =
        runBlocking {
            val cases =
                listOf(
                    Triple(
                        "invalid web domain",
                        request().copy(domain = "not a valid domain"),
                        AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.INVALID_SCOPE),
                    ),
                    Triple(
                        "ambiguous web origin",
                        request().copy(ambiguousWebOrigin = true),
                        AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.AMBIGUOUS_WEB_ORIGIN),
                    ),
                    Triple(
                        "blank application id",
                        request().copy(packageName = "  ", domain = null),
                        AutofillDiscoveryOutcome.Failed(AutofillOperationFailure.INVALID_SCOPE),
                    ),
                )

            cases.forEach { (name, invalidRequest, expectedOutcome) ->
                val fixture = fixture()
                assertEquals(name, expectedOutcome, fixture.operation(invalidRequest))
                assertEquals(name, 0, fixture.repository.searchCalls)
                assertTrue(name, fixture.store.activeIds().isEmpty())
            }
        }

    @Test
    fun unlockedVaultRecordsAndReturnsMetadataCandidates() =
        runBlocking {
            val candidate = candidate("entry-1")
            val fixture = fixture(candidates = listOf(candidate))

            val outcome = fixture.discoverAfterWebConfirmation(request())

            assertTrue(outcome is AutofillDiscoveryOutcome.Candidates)
            outcome as AutofillDiscoveryOutcome.Candidates
            assertEquals(listOf(candidate), outcome.candidates)
            assertEquals(setOf(EntryId("entry-1")), outcome.snapshot.presentedCandidateIds)
        }

    @Test
    fun directWebSelectionRediscoversMetadataAfterRecipientConfirmation() =
        runBlocking {
            val candidate = candidate("entry-1")
            val fixture = fixture(candidates = listOf(candidate))
            val first = fixture.discoverAfterWebConfirmation(request()) as AutofillDiscoveryOutcome.Candidates

            val resumed =
                fixture.operation(
                    request = request(),
                    interactionId = first.snapshot.id,
                    directEntryId = candidate.entry.id,
                )

            assertTrue(resumed is AutofillDiscoveryOutcome.Candidates)
            resumed as AutofillDiscoveryOutcome.Candidates
            assertEquals(first.snapshot.id, resumed.snapshot.id)
            assertEquals(candidate, resumed.candidates.single())
            assertEquals(2, fixture.repository.searchCalls)
        }

    private fun fixture(
        settings: AutofillSettings =
            AutofillSettings(
                presentation = AutofillPresentation.FLOATING,
                enabled = true,
                credentialManagerEnabled = true,
                requireAuthentication = false,
                strictDomainMatching = true,
                otpAutofillEnabled = false,
            ),
        hasCredentials: Boolean = true,
        vaultState: AuthenticationState = AuthenticationState.Authenticated(1L),
        candidates: List<AutofillCandidateSummary> = emptyList(),
        afterSearch: suspend () -> Unit = {},
    ): Fixture {
        val vault = FakeVault(vaultState)
        val authentication = NoOpAuthentication(vault)
        val store = AutofillInteractionStore()
        val lease = AutofillUnlockLease(authentication)
        val repository = RecordingCandidateRepository(candidates, afterSearch)
        val fieldMatchStrategy =
            object : FieldMatchStrategy {
                override fun match(request: AutofillRequest) =
                    MatchResult(
                        roleMap = mapOf("password" to FieldRole.PASSWORD),
                        hasCredentials = hasCredentials,
                    )
            }
        return Fixture(
            operation =
                DiscoverAutofillCandidates(
                    settingsSource = FixedSettingsSource(settings),
                    candidateRepository = repository,
                    legacyFieldMatchStrategy = fieldMatchStrategy,
                    credentialFieldMatchStrategy = fieldMatchStrategy,
                    startInteraction = StartAutofillInteraction(store, lease, vault),
                    interactionStore = store,
                    secureSessionAccessState = vault,
                    finishInteraction = FinishAutofillInteraction(store, lease),
                    logger = testLogger(),
                ),
            repository = repository,
            store = store,
        )
    }

    private suspend fun Fixture.discoverAfterWebConfirmation(request: AutofillRequest): AutofillDiscoveryOutcome {
        val confirmation = operation(request)
        if (confirmation !is AutofillDiscoveryOutcome.RequiresWebRecipientConfirmation) return confirmation
        store.confirmWebRecipient(
            confirmation.snapshot.id,
            AutofillGrantContext(request.packageName, request.domain),
            1L,
        )
        return operation(request, interactionId = confirmation.snapshot.id)
    }

    private fun request() =
        AutofillRequest(
            packageName = "com.example.app",
            domain = "example.com",
            fields =
                listOf(
                    AutofillField(
                        id = "password",
                        hints = setOf("password"),
                        inputType = null,
                        isFocused = true,
                    ),
                ),
            source = AutofillSource.AUTOFILL_SERVICE,
        )

    private fun candidate(id: String) =
        AutofillCandidateSummary(
            entry =
                EntryListItem(
                    identity =
                        EntryIdentity(
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
        val store: AutofillInteractionStore,
    )

    private class FixedSettingsSource(
        settings: AutofillSettings,
    ) : AutofillSettingsSource {
        override val autofillSettings: Flow<AutofillSettings> = flowOf(settings)
    }

    private class RecordingCandidateRepository(
        private val candidates: List<AutofillCandidateSummary>,
        private val afterSearch: suspend () -> Unit,
    ) : AutofillCandidateRepository {
        var searchCalls = 0
        var lastQuery: AutofillCandidateQuery? = null

        override suspend fun search(query: AutofillCandidateQuery): List<AutofillCandidateSummary> {
            searchCalls++
            lastQuery = query
            afterSearch()
            return candidates
        }
    }

    private class FakeVault(
        state: AuthenticationState,
    ) : SecureSessionAccessState {
        private val mutableState = MutableStateFlow(state)
        override val authenticationState: StateFlow<AuthenticationState> = mutableState

        override fun isUnlocked(): Boolean = mutableState.value is AuthenticationState.Authenticated
    }

    private class NoOpAuthentication(
        private val vault: FakeVault,
    ) : AuthenticationRequester,
        SessionLockController {
        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult = error("Authentication is not expected in discovery")

        override suspend fun lock(request: SessionLockRequest) = Unit
    }
}
