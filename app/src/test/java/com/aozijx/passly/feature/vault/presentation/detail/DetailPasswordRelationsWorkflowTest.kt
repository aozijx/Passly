package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.error.model.ValidationError
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
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.relation.RelatedPasswordEntry
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.policy.DefaultEntryFieldReader
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.feature.vault.detail.EntryDetailQueryImpl
import com.aozijx.passly.feature.vault.detail.RelatedPasswordOperationsImpl
import com.aozijx.passly.testing.RecordingTestLogger
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailPasswordRelationsWorkflowTest {
    @Test
    fun unlinkRequestMustMatchCurrentProjectionAndSession() =
        runTest {
            val fixture = unlinkFixture(backgroundScope)

            fixture.workflow.requestUnlink(EntryId("target"), EntryLinkId("stale-link"))
            assertEquals(RelatedPasswordUnlinkUiState.Closed, fixture.unlinkState)

            fixture.workSession.close()
            fixture.workflow.requestUnlink(EntryId("target"), EntryLinkId("source-target"))
            assertEquals(RelatedPasswordUnlinkUiState.Closed, fixture.unlinkState)
        }

    @Test
    fun requestedUnlinkCanBeDismissedWithoutDeleting() =
        runTest {
            val fixture = unlinkFixture(backgroundScope)

            fixture.workflow.requestUnlink(EntryId("target"), EntryLinkId("source-target"))
            assertEquals(
                RelatedPasswordUnlinkUiState.AwaitingConfirmation(
                    RelatedPasswordUnlinkTargetUiModel("target", "source-target", "target"),
                ),
                fixture.unlinkState,
            )

            fixture.workflow.dismissUnlink()

            assertEquals(RelatedPasswordUnlinkUiState.Closed, fixture.unlinkState)
            assertEquals(0, fixture.links.deleteAttempts)
        }

    @Test
    fun successfulUnlinkRefreshesCurrentRelations() =
        runTest {
            val fixture = unlinkFixture(backgroundScope)

            fixture.workflow.requestUnlink(EntryId("target"), EntryLinkId("source-target"))
            fixture.workflow.confirmUnlink()
            runCurrent()

            assertEquals(RelatedPasswordUnlinkUiState.Closed, fixture.unlinkState)
            assertEquals(1, fixture.links.deleteAttempts)
            assertEquals(listOf(EntryLinkId("source-target")), fixture.links.deletedLinkIds)
            assertEquals(1, fixture.relationUpdates)
            assertTrue(fixture.currentRelatedPasswords.isEmpty())
        }

    @Test
    fun inactiveTargetBeforeConfirmationRefreshesAndClosesDialog() =
        runTest {
            val fixture = unlinkFixture(backgroundScope)
            fixture.workflow.requestUnlink(EntryId("target"), EntryLinkId("source-target"))
            fixture.entries.activeItems = listOf(listItem("source"))
            fixture.missingEntryIds = setOf(EntryId("target"))

            fixture.workflow.confirmUnlink()
            runCurrent()

            assertEquals(RelatedPasswordUnlinkUiState.Closed, fixture.unlinkState)
            assertEquals(0, fixture.links.deleteAttempts)
            assertEquals(1, fixture.relationUpdates)
            assertTrue(fixture.currentRelatedPasswords.isEmpty())
        }

    @Test
    fun failedUnlinkKeepsConfirmationRetryable() =
        runTest {
            val fixture = unlinkFixture(backgroundScope, deleteFailure = true)
            fixture.workflow.requestUnlink(EntryId("target"), EntryLinkId("source-target"))

            fixture.workflow.confirmUnlink()
            runCurrent()

            assertEquals(
                RelatedPasswordUnlinkUiState.Failed(
                    RelatedPasswordUnlinkTargetUiModel("target", "source-target", "target"),
                ),
                fixture.unlinkState,
            )
            assertEquals(1, fixture.links.deleteAttempts)
            assertEquals(1, fixture.relationUpdates)
            assertEquals(listOf(RelatedPasswordEntry(EntryLinkId("source-target"), EntryId("target"), "target", "")), fixture.currentRelatedPasswords)

            fixture.links.failDelete = false
            fixture.workflow.confirmUnlink()
            runCurrent()

            assertEquals(RelatedPasswordUnlinkUiState.Closed, fixture.unlinkState)
            assertEquals(2, fixture.links.deleteAttempts)
            assertTrue(fixture.currentRelatedPasswords.isEmpty())
        }

    @Test
    fun duplicateUnlinkConfirmationStartsOneDelete() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val fixture = unlinkFixture(backgroundScope, deleteGate = gate)
            fixture.workflow.requestUnlink(EntryId("target"), EntryLinkId("source-target"))

            fixture.workflow.confirmUnlink()
            runCurrent()
            fixture.workflow.confirmUnlink()
            runCurrent()

            assertEquals(1, fixture.links.deleteAttempts)
            assertEquals(
                RelatedPasswordUnlinkUiState.Removing(
                    RelatedPasswordUnlinkTargetUiModel("target", "source-target", "target"),
                ),
                fixture.unlinkState,
            )
            gate.complete(Unit)
            runCurrent()
            assertEquals(RelatedPasswordUnlinkUiState.Closed, fixture.unlinkState)
        }

    @Test
    fun unlinkResultIsIgnoredAfterPageReplacementOrSessionRevocation() =
        runTest {
            val pageGate = CompletableDeferred<Unit>()
            val pageFixture = unlinkFixture(backgroundScope, deleteGate = pageGate)
            pageFixture.workflow.requestUnlink(EntryId("target"), EntryLinkId("source-target"))
            pageFixture.workflow.confirmUnlink()
            runCurrent()
            pageFixture.currentEntry = entry("replacement")
            pageFixture.workSession.open(pageFixture.currentEntry.id)
            pageFixture.workflow.close()
            pageGate.complete(Unit)
            runCurrent()
            assertEquals(0, pageFixture.relationUpdates)

            val authGate = CompletableDeferred<Unit>()
            val authFixture = unlinkFixture(backgroundScope, deleteGate = authGate)
            authFixture.workflow.requestUnlink(EntryId("target"), EntryLinkId("source-target"))
            authFixture.workflow.confirmUnlink()
            runCurrent()
            authFixture.access.authenticationState.value = AuthenticationState.Locked
            authFixture.workflow.close()
            authGate.complete(Unit)
            runCurrent()
            assertEquals(0, authFixture.relationUpdates)
        }

    @Test
    fun relationRefreshFailureIsLoggedAndLeavesRetryState() =
        runTest {
            val fixture = unlinkFixture(backgroundScope)
            fixture.failRelationRefresh = true
            fixture.workflow.requestUnlink(EntryId("target"), EntryLinkId("source-target"))

            fixture.workflow.confirmUnlink()
            runCurrent()

            assertEquals(
                RelatedPasswordUnlinkUiState.Failed(
                    RelatedPasswordUnlinkTargetUiModel("target", "source-target", "target"),
                ),
                fixture.unlinkState,
            )
            assertEquals(1, fixture.logger.events.size)
            assertTrue(fixture.logger.events.single().throwable is IllegalStateException)

            fixture.failRelationRefresh = false
            fixture.workflow.confirmUnlink()
            runCurrent()
            assertEquals(RelatedPasswordUnlinkUiState.Closed, fixture.unlinkState)
        }

    @Test
    fun candidateResultIsIgnoredAfterSameEntryDetailVisitIsReplaced() =
        runTest {
            val gate = CompletableDeferred<List<EntryListItem>>()
            val fixture = Fixture(backgroundScope, gate)
            fixture.workflow.open()
            runCurrent()

            fixture.workSession.open(fixture.currentEntry.id)
            gate.complete(listOf(listItem("source"), listItem("target")))
            runCurrent()

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
            runCurrent()

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
            runCurrent()

            assertEquals(RelatedPasswordBindingUiState.Closed, fixture.bindingState)
        }

    @Test
    fun replacingPageCancelsSuspendedBindBeforeItWritesTheRelation() =
        runTest {
            val candidates = CompletableDeferred(listOf(listItem("source"), listItem("target")))
            val linkWriteGate = CompletableDeferred<Unit>()
            val fixture = Fixture(backgroundScope, candidates, linkWriteGate)
            fixture.workflow.open()
            runCurrent()
            fixture.workflow.bind(EntryId("target"))
            runCurrent()
            assertEquals(1, fixture.links.upsertAttempts)

            fixture.currentEntry = entry("replacement")
            fixture.workSession.open(fixture.currentEntry.id)
            fixture.workflow.close()
            linkWriteGate.complete(Unit)
            runCurrent()

            assertTrue(fixture.links.upserts.isEmpty())
            assertEquals(0, fixture.relationUpdates)
        }

    @Test
    fun successfulBindRefreshesTheCurrentRelatedEntryProjection() =
        runTest {
            val fixture = Fixture(backgroundScope, completedCandidates())
            fixture.workflow.open()
            runCurrent()

            fixture.workflow.bind(EntryId("target"))
            runCurrent()

            assertEquals(RelatedPasswordBindingUiState.Closed, fixture.bindingState)
            assertEquals(1, fixture.relationUpdates)
            assertEquals(listOf(EntryId("target")), fixture.refreshedRelationIds)
        }

    @Test
    fun failedBindLeavesPickerOpenWithRetryState() =
        runTest {
            val fixture = Fixture(backgroundScope, completedCandidates(), failLinkWrite = true)
            fixture.workflow.open()
            runCurrent()

            fixture.workflow.bind(EntryId("target"))
            runCurrent()

            val ready = fixture.bindingState as RelatedPasswordBindingUiState.Ready
            assertEquals(RelatedPasswordBindingOperationState.Failed, ready.operation)
            assertEquals(0, fixture.relationUpdates)
        }

    @Test
    fun duplicateBindTapDoesNotStartAnotherLinkWrite() =
        runTest {
            val writeGate = CompletableDeferred<Unit>()
            val fixture = Fixture(backgroundScope, completedCandidates(), writeGate)
            fixture.workflow.open()
            runCurrent()

            fixture.workflow.bind(EntryId("target"))
            runCurrent()
            fixture.workflow.bind(EntryId("target"))
            runCurrent()

            assertEquals(1, fixture.links.upsertAttempts)
            writeGate.complete(Unit)
            runCurrent()
            assertEquals(1, fixture.links.upserts.size)
        }

    @Test
    fun targetRemovedAfterCandidateLoadIsRejectedBeforeLinkWrite() =
        runTest {
            val fixture = Fixture(backgroundScope, completedCandidates())
            fixture.workflow.open()
            runCurrent()
            fixture.entries.activeItems = listOf(listItem("source"))

            fixture.workflow.bind(EntryId("target"))
            runCurrent()

            val ready = fixture.bindingState as RelatedPasswordBindingUiState.Ready
            assertEquals(RelatedPasswordBindingOperationState.Failed, ready.operation)
            assertEquals(0, fixture.links.upsertAttempts)
        }

    private class Fixture(
        scope: kotlinx.coroutines.CoroutineScope,
        candidateGate: CompletableDeferred<List<EntryListItem>>,
        linkWriteGate: CompletableDeferred<Unit>? = null,
        failLinkWrite: Boolean = false,
        initialLinks: List<EntryLink> = emptyList(),
        deleteGate: CompletableDeferred<Unit>? = null,
        deleteFailure: Boolean = false,
    ) {
        val access = TestAccess()
        var currentEntry = entry("source")
        val links = TestLinks(linkWriteGate, failLinkWrite, initialLinks, deleteGate, deleteFailure)
        val entries = TestEntries(candidateGate)
        val logger = RecordingTestLogger()
        var relationUpdates = 0
        var refreshedRelationIds: List<EntryId> = emptyList()
        var currentRelatedPasswords: List<RelatedPasswordEntry> = emptyList()
        var missingEntryIds: Set<EntryId> = emptySet()
        var failRelationRefresh = false
        val workSession =
            DetailWorkSession(
                hasAccess = { access.hasFullSecureSessionAccess() },
                editEntry = { _, _ -> error("Entry editing is not part of this test") },
                fieldReader = DefaultEntryFieldReader(),
                present = { error("Entry presentation is not part of this test") },
                onMutation = {},
                onReveal = { _, _ -> },
            )
        var bindingState: RelatedPasswordBindingUiState = RelatedPasswordBindingUiState.Closed
            private set
        var unlinkState: RelatedPasswordUnlinkUiState = RelatedPasswordUnlinkUiState.Closed
            private set
        val workflow =
            DetailPasswordRelationsWorkflow(
                scope = scope,
                operations = RelatedPasswordOperationsImpl(entries, links, testLogger()),
                access = access,
                workSession = workSession,
                currentEntry = { currentEntry },
                currentBindingState = { bindingState },
                currentRelatedPasswords = { currentRelatedPasswords },
                currentUnlinkState = { unlinkState },
                logger = logger,
                presentationLoader =
                    presentationLoader(
                        links = links,
                        onRelationEntryIdsRequested = {
                            refreshedRelationIds = it
                            if (failRelationRefresh) throw IllegalStateException("relation refresh failed")
                        },
                        isEntryMissing = { it in missingEntryIds },
                    ),
                publish = { mutation ->
                    when (mutation) {
                        is DetailMutation.PasswordBindingStateChanged -> bindingState = mutation.state
                        is DetailMutation.PasswordUnlinkStateChanged -> unlinkState = mutation.state
                        is DetailMutation.RelationsChanged -> {
                            relationUpdates++
                            currentRelatedPasswords = mutation.relations.relatedPasswords
                        }
                        else -> Unit
                    }
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
        var activeItems: List<EntryListItem>? = null
        override val availableCategories: Flow<List<String>> = emptyFlow()
        override val deletedEntries: Flow<List<EntryListItem>> = emptyFlow()

        override suspend fun getActiveByType(entryType: EntryType): List<EntryListItem> =
            (activeItems ?: gate.await()).filter { it.entryType == entryType && it.deletedAt == null }
    }

    private class TestLinks(
        private val writeGate: CompletableDeferred<Unit>? = null,
        private val failWrite: Boolean = false,
        initialLinks: List<EntryLink> = emptyList(),
        private val deleteGate: CompletableDeferred<Unit>? = null,
        deleteFailure: Boolean = false,
    ) : EntryLinkRepository {
        val upserts = mutableListOf<EntryLink>()
        var upsertAttempts = 0
        private val storedLinks = initialLinks.toMutableList()
        var failDelete = deleteFailure
        var deleteAttempts = 0
        val deletedLinkIds = mutableListOf<EntryLinkId>()

        override fun observeAll(): Flow<List<EntryLink>> = emptyFlow()

        override fun observeLinks(entryId: EntryId): Flow<List<EntryLink>> = emptyFlow()

        override suspend fun getAll(): List<EntryLink> = storedLinks.toList()

        override suspend fun getLinks(entryId: EntryId): List<EntryLink> =
            storedLinks.filter { it.sourceEntryId == entryId || it.targetEntryId == entryId }

        override suspend fun upsert(link: EntryLink): AppResult<Unit> {
            upsertAttempts++
            writeGate?.await()
            if (failWrite) return AppResult.Failure(ValidationError())
            upserts += link
            storedLinks += link
            return AppResult.Success(Unit)
        }

        override suspend fun delete(linkId: EntryLinkId): AppResult<Unit> {
            deleteAttempts++
            deleteGate?.await()
            if (failDelete) return AppResult.Failure(ValidationError())
            deletedLinkIds += linkId
            storedLinks.removeAll { it.id == linkId }
            return AppResult.Success(Unit)
        }
    }

    private companion object {
        fun unlinkFixture(
            scope: kotlinx.coroutines.CoroutineScope,
            deleteGate: CompletableDeferred<Unit>? = null,
            deleteFailure: Boolean = false,
        ): Fixture {
            val link =
                EntryLink.create(
                    id = EntryLinkId("source-target"),
                    sourceEntryId = EntryId("source"),
                    targetEntryId = EntryId("target"),
                    relationType = EntryRelationType.RELATED_TO,
                    createdAt = 1L,
                )
            return Fixture(
                scope = scope,
                candidateGate = completedCandidates(),
                initialLinks = listOf(link),
                deleteGate = deleteGate,
                deleteFailure = deleteFailure,
            ).apply {
                currentRelatedPasswords =
                    listOf(RelatedPasswordEntry(link.id, EntryId("target"), "target", ""))
            }
        }

        fun presentationLoader(
            links: TestLinks,
            onRelationEntryIdsRequested: (List<EntryId>) -> Unit,
            isEntryMissing: (EntryId) -> Boolean = { false },
        ): DetailPresentationLoader =
            DetailPresentationLoader(
                sessionQuery =
                    EntryDetailQueryImpl(
                        entryQueryRepository =
                            object : EntryQueryRepository {
                                override suspend fun getById(entryId: EntryId): Entry? =
                                    when (entryId) {
                                        EntryId("source") -> entry("source")
                                        EntryId("target") -> entry("target")
                                        else -> null
                                    }?.takeUnless { isEntryMissing(it.id) }

                                override suspend fun getByIds(entryIds: List<EntryId>): List<Entry> {
                                    onRelationEntryIdsRequested(entryIds)
                                    return entryIds.mapNotNull { id ->
                                        when (id) {
                                            EntryId("source") -> entry("source")
                                            EntryId("target") -> entry("target")
                                            else -> null
                                        }?.takeUnless { isEntryMissing(it.id) }
                                    }
                                }
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
                        entryLinkRepository = links,
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

        fun completedCandidates() = CompletableDeferred(listOf(listItem("source"), listItem("target")))
    }
}
