package com.aozijx.passly.feature.vault.presentation.detail

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.platform.media.FaviconCropRequest
import com.aozijx.passly.core.platform.media.FaviconImageProcessor
import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.core.platform.packageinfo.InstalledAppMetadata
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SensitiveKeyFreshnessState
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.history.EntryRevisionDifference
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.sensitive.RevealedRevisionSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.otp.OtpResult
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import com.aozijx.passly.domain.entry.port.EntryTagQuery
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.entry.policy.DefaultEntryFieldReader
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.SecureSessionAccessPolicy
import com.aozijx.passly.feature.vault.detail.DetailSessionQuery
import com.aozijx.passly.feature.vault.detail.EditDetailEntryUseCase
import com.aozijx.passly.feature.vault.detail.ExportEntryQrUseCase
import com.aozijx.passly.feature.vault.detail.RevealEntryFieldsUseCase
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldUseCase
import com.aozijx.passly.feature.vault.entry.CopyOtpCodeUseCase
import com.aozijx.passly.feature.vault.history.CompareEntryRevisionUseCase
import com.aozijx.passly.feature.vault.history.RestoreEntryRevisionUseCase
import com.aozijx.passly.feature.vault.history.RevealRevisionFieldsUseCase
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimeFactory
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionComparisonPhase
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionPage
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionRestorePhase
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionState
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionStateProvider
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun backAndDismissDoNotCancelRestoreBeforeItRefreshesTheEntry() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.viewModel
        val store = ViewModelStore().apply { put("detail", viewModel) }
        try {
            viewModel.load(ENTRY_ID.value)
            runCurrent()

            viewModel.onAction(DetailUiAction.OpenRevisionHistory)
            runCurrent()
            viewModel.onAction(DetailUiAction.SelectRevision(REVISION_ID.value))
            runCurrent()
            viewModel.onAction(DetailUiAction.RequestRevisionRestore)
            viewModel.onAction(DetailUiAction.ConfirmRevisionRestore)
            runCurrent()

            assertTrue(fixture.revisions.restoreEntered.isCompleted)
            viewModel.onAction(DetailUiAction.RevisionBack)
            viewModel.onAction(DetailUiAction.DismissRevisionHistory)
            assertFalse(fixture.revisions.restoreCompleted)
            assertTrue(viewModel.uiState.value.revisions is DetailRevisionState.Open)
            assertEquals(
                DetailRevisionRestorePhase.Restoring,
                (((viewModel.uiState.value.revisions as DetailRevisionState.Open).page as DetailRevisionPage.Comparison)
                    .phase as DetailRevisionComparisonPhase.Ready).restore,
            )

            fixture.revisions.releasePostCommitCleanup.complete(Unit)
            runCurrent()

            assertEquals("Restored title", viewModel.uiState.value.entry?.title)
            assertTrue(fixture.revisions.restoreCompleted)
        } finally {
            store.clear()
        }
    }

    @Test
    fun entryReplacementDoesNotPublishLateRestoreIntoTheNewEntry() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.viewModel
        val store = ViewModelStore().apply { put("detail", viewModel) }
        try {
            startRestore(viewModel)
            assertTrue(fixture.revisions.restoreEntered.isCompleted)

            viewModel.load(OTHER_ENTRY_ID.value)
            runCurrent()
            fixture.revisions.releasePostCommitCleanup.complete(Unit)
            runCurrent()

            assertEquals("Other title", viewModel.uiState.value.entry?.title)
            assertTrue(fixture.revisions.restoreCompleted)
        } finally {
            store.clear()
        }
    }

    @Test
    fun sessionClosureDoesNotPublishLateRestoreIntoClearedState() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.viewModel
        val store = ViewModelStore().apply { put("detail", viewModel) }
        try {
            startRestore(viewModel)
            assertTrue(fixture.revisions.restoreEntered.isCompleted)

            viewModel.onAction(DetailUiAction.ClearSensitiveState)
            fixture.revisions.releasePostCommitCleanup.complete(Unit)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.entry)
            assertTrue(fixture.revisions.restoreCompleted)
        } finally {
            store.clear()
        }
    }

    private fun TestScope.startRestore(viewModel: DetailViewModel) {
        viewModel.load(ENTRY_ID.value)
        runCurrent()
        viewModel.onAction(DetailUiAction.OpenRevisionHistory)
        runCurrent()
        viewModel.onAction(DetailUiAction.SelectRevision(REVISION_ID.value))
        runCurrent()
        viewModel.onAction(DetailUiAction.RequestRevisionRestore)
        viewModel.onAction(DetailUiAction.ConfirmRevisionRestore)
        runCurrent()
    }

    private class Fixture {
        val access = AuthenticatedAccess()
        val entries = MutableEntries(currentEntry())
        val revisions = BlockingRevisionRepository(entries)
        private val sensitive = EmptySensitiveFieldRepository()
        private val links = EmptyEntryLinkRepository
        private val authGate = UnusedAuthorizationGate
        private val activity = NoOpActivityRecorder
        private val fieldReader: EntryFieldReader = DefaultEntryFieldReader()
        private val clipboard = SensitiveClipboardWriter { }
        private val sessionProvider = UnlockedSessionProvider()
        private val freshness = FreshnessState()
        private val installedApps = EmptyInstalledAppDirectory
        private val favicon = UnusedFaviconImageProcessor

        val viewModel = DetailViewModel(
            entryTagQuery = EmptyEntryTagQuery,
            activityQueryRepository = EmptyActivityQueryRepository,
            accessPolicy = SecureSessionAccessPolicy(access),
            exportEntryQr = ExportEntryQrUseCase(authGate, sensitive, activity, testLogger()),
            secureSessionAccessState = access,
            revealEntryFields = RevealEntryFieldsUseCase(authGate, fieldReader, sensitive, activity),
            editDetailEntry = EditDetailEntryUseCase(entries, entries, access, testLogger()),
            copyEntryFieldUseCase = CopyEntryFieldUseCase(
                authGate,
                entries,
                fieldReader,
                sensitive,
                clipboard,
                activity,
            ),
            copyOtpCodeUseCase = CopyOtpCodeUseCase(authGate, clipboard, activity),
            faviconImageProcessor = favicon,
            presentationLoader = DetailPresentationLoader(
                DetailSessionQuery(entries, sensitive, links),
                installedApps,
            ),
            entryRevisionRepository = revisions,
            compareEntryRevision = CompareEntryRevisionUseCase(revisions),
            revealRevisionFields = RevealRevisionFieldsUseCase(revisions, authGate),
            restoreEntryRevision = RestoreEntryRevisionUseCase(revisions, entries, sensitive, authGate),
            otpCodeRuntimeFactory = OtpCodeRuntimeFactory(NoOtpConfigRepository, sessionProvider, freshness),
        )
    }

    private class MutableEntries(initial: Entry) : EntryQueryRepository, EntryCommandRepository {
        private val values = mutableMapOf(initial.id to initial)

        init {
            values[OTHER_ENTRY_ID] = currentEntry(OTHER_ENTRY_ID, "Other title")
        }

        override suspend fun getById(entryId: EntryId): Entry? = values[entryId]
        override suspend fun findEntriesWithCustomIcons(): List<Entry> = emptyList()
        override suspend fun count(): Int = values.size
        override suspend fun createEntry(entry: Entry): AppResult<EntryId> = AppResult.Success(entry.id)

        override suspend fun updateEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
            changes: EntryUpdate,
        ): AppResult<Unit> {
            val current = requireNotNull(values[id])
            values[id] = current.copy(
                identity = current.identity.copy(version = expectedVersion.next()),
                profile = changes.profile ?: current.profile,
                secret = changes.secret ?: current.secret,
            )
            return AppResult.Success(Unit)
        }

        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)

        fun restoreFrom(snapshot: RedactedEntryRevision, expectedVersion: EntryVersion) {
            val current = requireNotNull(values[snapshot.metadata.entryId])
            values[current.id] = current.copy(
                identity = current.identity.copy(version = expectedVersion.next()),
                profile = snapshot.profile,
                secret = snapshot.secret,
            )
        }
    }

    private class BlockingRevisionRepository(
        private val entries: MutableEntries,
    ) : EntryRevisionRepository {
        val restoreEntered = CompletableDeferred<Unit>()
        val releasePostCommitCleanup = CompletableDeferred<Unit>()
        var restoreCompleted = false

        override suspend fun initializeHistory(): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeMetadata(entryId: EntryId): Flow<List<EntryRevisionMetadata>> =
            flowOf(listOf(revisionMetadata()))

        override suspend fun loadRedacted(
            entryId: EntryId,
            revisionId: EntryRevisionId,
        ): AppResult<RedactedEntryRevision> = AppResult.Success(restoredSnapshot())

        override suspend fun reveal(
            entryId: EntryId,
            revisionId: EntryRevisionId,
            keys: Set<SensitiveFieldKey>,
            permit: AuthorizationPermit,
        ): AppResult<List<RevealedRevisionSensitiveField>> = error("Not used")

        override suspend fun restore(
            entryId: EntryId,
            revisionId: EntryRevisionId,
            expectedVersion: EntryVersion,
            permit: AuthorizationPermit?,
        ): AppResult<EntryVersion> {
            entries.restoreFrom(restoredSnapshot(), expectedVersion)
            restoreEntered.complete(Unit)
            withContext(NonCancellable) { releasePostCommitCleanup.await() }
            restoreCompleted = true
            return AppResult.Success(expectedVersion.next())
        }
    }

    private class EmptySensitiveFieldRepository : SensitiveFieldRepository {
        override suspend fun getPresence(entryId: EntryId) = SensitiveFieldPresence(entryId, emptySet())
        override suspend fun reveal(
            entryId: EntryId,
            key: SensitiveFieldKey,
            action: com.aozijx.passly.domain.access.model.SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): RevealedSensitiveField? = error("Not used")

        override suspend fun revealMany(
            entryId: EntryId,
            keys: Set<SensitiveFieldKey>,
            action: com.aozijx.passly.domain.access.model.SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): List<RevealedSensitiveField> = error("Not used")

        override suspend fun readBundle(entryId: EntryId): EntrySecret = error("Not used")
        override suspend fun readAll(entryId: EntryId): EntrySecret = error("Not used")
    }

    private object EmptyEntryLinkRepository : EntryLinkRepository {
        override fun observeAll(): Flow<List<EntryLink>> = flowOf(emptyList())
        override fun observeLinks(entryId: EntryId): Flow<List<EntryLink>> = flowOf(emptyList())
        override suspend fun getAll(): List<EntryLink> = emptyList()
        override suspend fun getLinks(entryId: EntryId): List<EntryLink> = emptyList()
        override suspend fun upsert(link: EntryLink): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun delete(linkId: EntryLinkId): AppResult<Unit> = AppResult.Success(Unit)
    }

    private object EmptyEntryTagQuery : EntryTagQuery {
        override suspend fun findAllTags(): Set<String> = emptySet()
    }

    private object EmptyActivityQueryRepository : ActivityQueryRepository {
        override fun observeByEntryId(entryId: String): Flow<List<EntryActivity>> = flowOf(emptyList())
        override fun observeAll(): Flow<List<EntryActivity>> = flowOf(emptyList())
        override fun observeByType(activityType: ActivityType): Flow<List<EntryActivity>> = flowOf(emptyList())
    }

    private object NoOpActivityRecorder : ActivityRecorder {
        override suspend fun recordUsage(entryId: String, type: ActivityType) = AppResult.Success(Unit)
        override suspend fun deleteByEntryId(entryId: String) = Unit
        override suspend fun deleteBefore(timestamp: Long) = Unit
    }

    private object UnusedAuthorizationGate : AuthorizationGate {
        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> = error("Authorization is not used by this test")
    }

    private object EmptyInstalledAppDirectory : InstalledAppDirectory {
        override suspend fun metadataFor(packageName: String): InstalledAppMetadata? = null
        override suspend fun launchableApps(): List<InstalledAppMetadata> = emptyList()
    }

    private object NoOtpConfigRepository : OtpConfigRepository {
        override suspend fun getConfig(entryId: String): OtpConfig? = null
        override suspend fun consumeHotpCode(entryId: String) =
            OtpResult.Failure(OtpGenerationError.InvalidSecret)
    }

    private class UnlockedSessionProvider : SessionStateProvider {
        override val lockStateFlow = MutableStateFlow(SecureSessionState.UNLOCKED)
        override val lockState get() = lockStateFlow.value
    }

    private class FreshnessState : SensitiveKeyFreshnessState {
        override val generation: StateFlow<Long> = MutableStateFlow(0L)
    }

    private class AuthenticatedAccess : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))

        override fun isUnlocked() = true
    }

    private object UnusedFaviconImageProcessor : FaviconImageProcessor {
        override suspend fun discard(path: String?) = Unit
        override suspend fun discardPromotedCandidate(path: String?) = Unit
        override fun discardEditorResources(stagedPath: String?, pendingInputPath: String?, promotedCandidatePath: String?) = Unit
        override suspend fun stageUpload(uri: Uri): Result<String> = error("Not used")
        override suspend fun stageHttpsUrl(value: String): Result<String> = error("Not used")
        override suspend fun process(stagedInputPath: String, crop: FaviconCropRequest?): Result<String> = error("Not used")
        override suspend fun promote(stagedPath: String): Result<String> = error("Not used")
        override fun isStaged(path: String): Boolean = false
    }

    private companion object {
        val ENTRY_ID = EntryId("detail-entry")
        val OTHER_ENTRY_ID = EntryId("other-detail-entry")
        val REVISION_ID = EntryRevisionId("revision-1")

        fun currentEntry(id: EntryId = ENTRY_ID, title: String = "Current title") = Entry(
            identity = EntryIdentity(id, EntryType.LOGIN, EntryVersion(2), EntryTimestamps(1L)),
            profile = EntryProfile(title),
            secret = EntrySecret(LoginCredential()),
        )

        fun revisionMetadata() = EntryRevisionMetadata(
            id = REVISION_ID,
            entryId = ENTRY_ID,
            version = EntryVersion(1),
            createdAtMs = 1L,
            change = RevisionChange.VALUE_CHANGED,
        )

        fun restoredSnapshot() = RedactedEntryRevision(
            metadata = revisionMetadata(),
            type = EntryType.LOGIN,
            profile = EntryProfile("Restored title"),
            secret = EntrySecret(LoginCredential()),
        )
    }
}
