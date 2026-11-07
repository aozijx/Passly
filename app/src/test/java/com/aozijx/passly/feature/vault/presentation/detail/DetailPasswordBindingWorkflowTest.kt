package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.feature.vault.detail.DetailSessionQuery
import com.aozijx.passly.feature.vault.detail.RelatedPasswordBinding
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.RelatedPasswordBindingUiState
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailPasswordBindingWorkflowTest {
    @Test
    fun candidateResultIsIgnoredAfterSameEntryDetailVisitIsReplaced() =
        runTest {
            val gate = CompletableDeferred<List<EntryListItem>>()
            val fixture = Fixture(backgroundScope, gate)
            fixture.workflow.open()
            runCurrent()

            fixture.workSession.open(fixture.currentEntry.id)
            gate.complete(listOf(listItem("source"), listItem("target")))
            advanceUntilIdle()

            assertEquals(RelatedPasswordBindingUiState.Loading, fixture.bindingState)
        }

    @Test
    fun candidateResultIsIgnoredAfterSecureSessionIsRevoked() =
        runTest {
            val gate = CompletableDeferred<List<EntryListItem>>()
            val fixture = Fixture(backgroundScope, gate)
            fixture.workflow.open()
            runCurrent()

            fixture.access.authenticationState.value = AuthenticationState.Locked
            gate.complete(listOf(listItem("source"), listItem("target")))
            advanceUntilIdle()

            assertEquals(RelatedPasswordBindingUiState.Loading, fixture.bindingState)
        }

    @Test
    fun dismissingPickerCancelsPendingCandidateRequestAndClosesState() =
        runTest {
            val gate = CompletableDeferred<List<EntryListItem>>()
            val fixture = Fixture(backgroundScope, gate)
            fixture.workflow.open()
            runCurrent()

            fixture.workflow.dismiss()
            gate.complete(listOf(listItem("source"), listItem("target")))
            advanceUntilIdle()

            assertEquals(RelatedPasswordBindingUiState.Closed, fixture.bindingState)
        }

    private class Fixture(
        scope: kotlinx.coroutines.CoroutineScope,
        candidateGate: CompletableDeferred<List<EntryListItem>>,
    ) {
        val access = TestAccess()
        var currentEntry = entry("source")
        val workSession =
            DetailWorkSession(
                hasAccess = { access.hasFullSecureSessionAccess() },
                editEntry = { _, _ -> error("Entry editing is not part of this test") },
                present = { error("Entry presentation is not part of this test") },
                onMutation = {},
                onReveal = { _, _ -> },
            )
        var bindingState: RelatedPasswordBindingUiState = RelatedPasswordBindingUiState.Closed
            private set
        val workflow =
            DetailPasswordBindingWorkflow(
                scope = scope,
                binding = RelatedPasswordBinding(TestEntries(candidateGate), TestLinks(), testLogger()),
                access = access,
                workSession = workSession,
                currentEntry = { currentEntry },
                currentBindingState = { bindingState },
                presentationLoader = presentationLoader(),
                publish = { mutation ->
                    if (mutation is DetailMutation.PasswordBindingStateChanged) bindingState = mutation.state
                },
            )

        init {
            workSession.open(currentEntry.id)
        }
    }

    private class TestAccess : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))

        override fun isUnlocked(): Boolean = authenticationState.value is AuthenticationState.Authenticated
    }

    private class TestEntries(
        private val gate: CompletableDeferred<List<EntryListItem>>,
    ) : EntryListQueryRepository {
        override val availableCategories: Flow<List<String>> = emptyFlow()
        override val deletedEntries: Flow<List<EntryListItem>> = emptyFlow()

        override suspend fun getActiveByType(entryType: EntryType): List<EntryListItem> = gate.await()
    }

    private class TestLinks : EntryLinkRepository {
        override fun observeAll(): Flow<List<EntryLink>> = emptyFlow()

        override fun observeLinks(entryId: EntryId): Flow<List<EntryLink>> = emptyFlow()

        override suspend fun getAll(): List<EntryLink> = emptyList()

        override suspend fun getLinks(entryId: EntryId): List<EntryLink> = emptyList()

        override suspend fun upsert(link: EntryLink): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun delete(linkId: EntryLinkId): AppResult<Unit> = AppResult.Success(Unit)
    }

    private companion object {
        fun presentationLoader(): DetailPresentationLoader =
            DetailPresentationLoader(
                sessionQuery =
                    DetailSessionQuery(
                        entryQueryRepository =
                            object : EntryQueryRepository {
                                override suspend fun getById(entryId: EntryId): Entry? = null

                                override suspend fun getByIds(entryIds: List<EntryId>): List<Entry> = emptyList()
                            },
                        sensitiveFieldRepository =
                            object : SensitiveFieldRepository {
                                override suspend fun getPresence(entryId: EntryId) = SensitiveFieldPresence(entryId, emptySet())

                                override suspend fun reveal(
                                    entryId: EntryId,
                                    key: SensitiveFieldKey,
                                    action: SensitiveAccessAction,
                                    permit: AuthorizationPermit,
                                ): RevealedSensitiveField? = error("Sensitive fields are not part of this test")

                                override suspend fun revealMany(
                                    entryId: EntryId,
                                    keys: Set<SensitiveFieldKey>,
                                    action: SensitiveAccessAction,
                                    permit: AuthorizationPermit,
                                ): List<RevealedSensitiveField> = error("Sensitive fields are not part of this test")

                                override suspend fun readBundle(entryId: EntryId): EntrySecret = EntrySecret()
                            },
                        entryLinkRepository = TestLinks(),
                    ),
                installedAppDirectory =
                    object : InstalledAppDirectory {
                        override suspend fun metadataFor(packageName: String) = null

                        override suspend fun launchableApps() =
                            emptyList<com.aozijx.passly.core.platform.packageinfo.InstalledAppMetadata>()
                    },
            )

        fun listItem(id: String) =
            EntryListItem(
                identity = EntryIdentity(EntryId(id), EntryType.LOGIN, timestamps = EntryTimestamps(1L)),
                profile = EntryProfile(title = id),
            )

        fun entry(id: String) =
            Entry(
                identity = EntryIdentity(EntryId(id), EntryType.LOGIN, timestamps = EntryTimestamps(1L)),
                profile = EntryProfile(title = id),
                secret = EntrySecret(credential = LoginCredential()),
            )
    }
}
