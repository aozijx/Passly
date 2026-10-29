package com.aozijx.passly.feature.autofill.credential

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationMethod
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
import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillCredentialResolution
import com.aozijx.passly.domain.autofill.model.AutofillCredentialSelection
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillInboxEnqueueOutcome
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.autofill.port.ApplicationLabelResolver
import com.aozijx.passly.domain.autofill.port.AutofillCandidateRepository
import com.aozijx.passly.domain.autofill.port.AutofillCredentialResolver
import com.aozijx.passly.domain.autofill.port.AutofillEntryWriter
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.autofill.port.MatchResult
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.query.CredentialMatch
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.MatchType
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.feature.autofill.core.AuthorizeAutofillInteraction
import com.aozijx.passly.feature.autofill.core.DiscoverAutofillCandidates
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.core.ResolveAutofillSelection
import com.aozijx.passly.feature.autofill.core.SaveAutofillCredential
import com.aozijx.passly.feature.autofill.core.StartAutofillInteraction
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.domain.entry.policy.EntryDraftMaterializer
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialManagerFlowTest {
    @Test
    fun lockedBeginGetReturnsOneSharedUnlockInteraction() = runBlocking {
        val fixture = fixture(AuthenticationState.Locked)

        val outcome = fixture.flow.beginGet(request())

        assertTrue(outcome is AutofillDiscoveryOutcome.RequiresUnlock)
        outcome as AutofillDiscoveryOutcome.RequiresUnlock
        assertEquals(AutofillInteractionState.RequiresUnlock, outcome.snapshot.state)
        assertEquals(0, fixture.repository.searchCalls)
    }

    @Test
    fun unlockRediscoveryAndSelectionReuseInteractionWithoutSecondAuthentication() = runBlocking {
        val fixture = fixture(AuthenticationState.Locked)
        val locked = fixture.flow.beginGet(request()) as AutofillDiscoveryOutcome.RequiresUnlock

        val unlock = fixture.flow.unlockAndRediscover(locked.snapshot.id, request())
        assertTrue(unlock is CredentialManagerUnlockResult.Rediscovered)
        val rediscovered = (unlock as CredentialManagerUnlockResult.Rediscovered).discovery
        assertTrue(rediscovered is AutofillDiscoveryOutcome.Candidates)
        val candidate = (rediscovered as AutofillDiscoveryOutcome.Candidates).candidates.single()

        val selected = fixture.flow.selectPassword(
            interactionId = locked.snapshot.id,
            scope = scope(),
            entryId = candidate.entry.id,
            allowedUserIds = setOf("alice"),
        )

        assertTrue(selected is CredentialManagerPasswordResult.Success)
        selected as CredentialManagerPasswordResult.Success
        assertEquals("alice", selected.username)
        assertEquals("secret", selected.password)
        assertEquals(
            AutofillSelectionReceipt(locked.snapshot.id, candidate.entry.id),
            selected.receipt,
        )
        assertEquals(1, fixture.authentication.authenticationCount)
        assertEquals(
            AutofillInteractionState.ResolvingSelection(candidate.entry.id),
            fixture.store.snapshot(locked.snapshot.id)?.state,
        )
        assertEquals(null, fixture.authentication.lastLockReason)
    }

    @Test
    fun selectionRejectsScopeMismatchBeforeSecretResolution() = runBlocking {
        val fixture = fixture(AuthenticationState.Authenticated(1L))
        val discovered = fixture.flow.beginGet(request()) as AutofillDiscoveryOutcome.Candidates

        val result = fixture.flow.selectPassword(
            interactionId = discovered.snapshot.id,
            scope = scope("com.example.other"),
            entryId = EntryId("entry-1"),
        )

        assertEquals(CredentialManagerPasswordResult.NotAvailable, result)
        assertEquals(0, fixture.resolver.resolveCalls)
    }

    @Test
    fun selectionRejectsUsernameOutsideAllowedUsers() = runBlocking {
        val fixture = fixture(AuthenticationState.Authenticated(1L))
        val discovered = fixture.flow.beginGet(request()) as AutofillDiscoveryOutcome.Candidates

        val result = fixture.flow.selectPassword(
            interactionId = discovered.snapshot.id,
            scope = scope(),
            entryId = EntryId("entry-1"),
            allowedUserIds = setOf("bob"),
        )

        assertEquals(CredentialManagerPasswordResult.NotAvailable, result)
        assertEquals(AutofillInteractionState.Cancelled, fixture.store.snapshot(discovered.snapshot.id)?.state)
    }

    @Test
    fun lockedCreateWithoutInboxKeyRejectsWithoutAuthentication() = runBlocking {
        val fixture = fixture(AuthenticationState.Locked)
        val interaction = fixture.flow.beginCreate(scope())

        val result = fixture.flow.createPassword(
            interactionId = interaction.id,
            scope = scope(),
            username = "alice",
            password = "secret",
        )

        assertEquals(CredentialManagerCreateResult.NotSaved, result)
        assertEquals(0, fixture.authentication.authenticationCount)
        assertEquals(0, fixture.commandRepository.createCalls)
        assertEquals(AutofillInteractionState.Cancelled, fixture.store.snapshot(interaction.id)?.state)
    }

    @Test
    fun lockedCreateQueuesSealedSaveWithoutAuthenticationOrDatabaseWrite() = runBlocking {
        val fixture = fixture(
            AuthenticationState.Locked,
            inboxOutcome = AutofillInboxEnqueueOutcome.Queued,
        )
        val interaction = fixture.flow.beginCreate(scope())

        val result = fixture.flow.createPassword(
            interactionId = interaction.id,
            scope = scope(),
            username = "alice",
            password = "secret",
        )

        assertEquals(CredentialManagerCreateResult.Queued, result)
        assertEquals(0, fixture.authentication.authenticationCount)
        assertEquals(0, fixture.commandRepository.createCalls)
        assertEquals(AutofillInteractionState.Completed, fixture.store.snapshot(interaction.id)?.state)
    }

    @Test
    fun cancelledCreateCleansUpInteraction() = runBlocking {
        val fixture = fixture(AuthenticationState.Locked)
        val interaction = fixture.flow.beginCreate(scope())

        fixture.flow.cancel(interaction.id)

        assertEquals(AutofillInteractionState.Cancelled, fixture.store.snapshot(interaction.id)?.state)
        assertEquals(0, fixture.commandRepository.createCalls)
    }

    @Test
    fun `create rechecks credential manager policy before persistence`() = runBlocking {
        listOf(
            AutofillSettings(enabled = false, credentialManagerEnabled = true),
            AutofillSettings(enabled = true, credentialManagerEnabled = false),
        ).forEach { policy ->
            val fixture = fixture(AuthenticationState.Authenticated(1L), policy)
            val interaction = fixture.flow.beginCreate(scope())

            val result = fixture.flow.createPassword(
                interactionId = interaction.id,
                scope = scope(),
                username = "alice",
                password = "secret",
            )

            assertEquals(CredentialManagerCreateResult.NotSaved, result)
            assertEquals(0, fixture.commandRepository.createCalls)
        }
    }

    @Test
    fun `legacy save setting does not disable credential manager creation`() = runBlocking {
        val fixture = fixture(
            AuthenticationState.Authenticated(1L),
            AutofillSettings(
                enabled = true,
                credentialManagerEnabled = true,
                savePromptsEnabled = false,
            ),
        )
        val interaction = fixture.flow.beginCreate(scope())

        val result = fixture.flow.createPassword(
            interactionId = interaction.id,
            scope = scope(),
            username = "alice",
            password = "secret",
        )

        assertEquals(CredentialManagerCreateResult.Saved, result)
        assertEquals(1, fixture.commandRepository.createCalls)
    }

    private fun fixture(
        initialState: AuthenticationState,
        policy: AutofillSettings = AutofillSettings(
            enabled = true,
            credentialManagerEnabled = true,
            requireAuthentication = false,
        ),
        inboxOutcome: AutofillInboxEnqueueOutcome = AutofillInboxEnqueueOutcome.NotProvisioned,
    ): Fixture {
        val settings = FixedSettingsSource(policy)
        val vault = FakeVault(initialState)
        val authentication = FakeAuthenticationController(vault)
        val store = AutofillInteractionStore()
        val lease = AutofillUnlockLease(authentication)
        val start = StartAutofillInteraction(store, lease, vault)
        val finish = FinishAutofillInteraction(store, lease)
        val repository = RecordingCandidateRepository(listOf(candidate()))
        val matcher = object : FieldMatchStrategy {
            override fun match(request: AutofillRequest) = MatchResult(
                roleMap = mapOf("password" to FieldRole.PASSWORD),
                hasCredentials = true,
            )
        }
        val resolver = RecordingCredentialResolver(
            AutofillCredentialResolution.Resolved(
                AutofillCredentialPayload(EntryId("entry-1"), "alice", "secret"),
            ),
        )
        val commandRepository = RecordingEntryCommandRepository()
        val saveCredential = SaveAutofillCredential(
            applicationLabelResolver = ApplicationLabelResolver { "Example" },
            createEntry = CreateEntryUseCase(
                entryCommandRepository = commandRepository,
                secureSessionAccessState = vault,
                materializer = EntryDraftMaterializer(),
                autofillEntryWriter = commandRepository,
                logger = testLogger(),
                idFactory = { EntryId("saved-entry") },
                clock = { 1L },
                monotonicClock = { 1L },
            ),
        )
        return Fixture(
            flow = CredentialManagerFlow(
                discoverCandidates = DiscoverAutofillCandidates(
                    settingsSource = settings,
                    candidateRepository = repository,
                    legacyFieldMatchStrategy = matcher,
                    credentialFieldMatchStrategy = matcher,
                    startInteraction = start,
                    interactionStore = store,
                    finishInteraction = finish,
                ),
                startInteraction = start,
                authorizeInteraction = AuthorizeAutofillInteraction(
                    authentication,
                    vault,
                    store,
                    lease,
                ),
                resolveSelection = ResolveAutofillSelection(store, resolver),
                saveCredential = saveCredential,
                saveAccessPolicy = com.aozijx.passly.feature.autofill.core.AutofillSaveAccessPolicy(vault),
                inboxQueue = com.aozijx.passly.security.autofill.inbox.AutofillInboxQueue { _, _ ->
                    inboxOutcome
                },
                finishInteraction = finish,
                interactionStore = store,
                settingsSource = settings,
            ),
            store = store,
            authentication = authentication,
            repository = repository,
            resolver = resolver,
            commandRepository = commandRepository,
        )
    }

    private fun request() = AutofillRequest(
        packageName = "com.example.app",
        domain = null,
        fields = listOf(
            AutofillField("password", setOf("PASSWORD"), null, true),
        ),
        source = AutofillSource.CREDENTIAL_MANAGER,
    )

    private fun scope(packageName: String = "com.example.app") =
        AutofillGrantContext(packageName, null)

    private fun candidate() = AutofillCandidateSummary(
        entry = EntryListItem(
            identity = EntryIdentity(
                id = EntryId("entry-1"),
                type = EntryType.LOGIN,
                timestamps = EntryTimestamps(1L),
            ),
            profile = EntryProfile(title = "Example", username = "alice"),
        ),
        match = CredentialMatch(MatchType.APPLICATION_ID, applicationId = "com.example.app"),
    )

    private data class Fixture(
        val flow: CredentialManagerFlow,
        val store: AutofillInteractionStore,
        val authentication: FakeAuthenticationController,
        val repository: RecordingCandidateRepository,
        val resolver: RecordingCredentialResolver,
        val commandRepository: RecordingEntryCommandRepository,
    )

    private class FixedSettingsSource(settings: AutofillSettings) : InteractionSettingsSource {
        override val interaction: Flow<InteractionSettings> = flowOf(
            InteractionSettings(autofill = settings),
        )
    }

    private class FakeVault(initialState: AuthenticationState) : SecureSessionAccessState {
        private val mutableState = MutableStateFlow(initialState)
        override val authenticationState: StateFlow<AuthenticationState> = mutableState
        override fun isUnlocked(): Boolean = when (mutableState.value) {
            is AuthenticationState.Authenticated,
            is AuthenticationState.AutofillAuthenticated -> true
            else -> false
        }

        fun unlock() {
            mutableState.value = AuthenticationState.AutofillAuthenticated(1L)
        }

        fun lock() {
            mutableState.value = AuthenticationState.Locked
        }
    }

    private class FakeAuthenticationController(
        private val vault: FakeVault,
    ) : AuthenticationRequester, SessionLockController {
        var authenticationCount = 0
        var lastLockReason: LockReason? = null

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult {
            authenticationCount++
            vault.unlock()
            return AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
        }

        override suspend fun lock(request: SessionLockRequest) {
            lastLockReason = request.reason
            vault.lock()
        }
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

    private class RecordingCredentialResolver(
        private val resolution: AutofillCredentialResolution,
    ) : AutofillCredentialResolver {
        var resolveCalls = 0

        override suspend fun resolve(
            selection: AutofillCredentialSelection,
        ): AutofillCredentialResolution {
            resolveCalls++
            return resolution
        }
    }

    private class RecordingEntryCommandRepository : EntryCommandRepository, AutofillEntryWriter {
        var createCalls = 0

        override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
            createCalls++
            return AppResult.Success(EntryId("saved-entry"))
        }

        override suspend fun create(entry: Entry): AppResult<EntryId> = createEntry(entry)

        override suspend fun updateEntry(id: EntryId, expectedVersion: EntryVersion, changes: EntryUpdate) =
            AppResult.Success(Unit)

        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)
    }

}
