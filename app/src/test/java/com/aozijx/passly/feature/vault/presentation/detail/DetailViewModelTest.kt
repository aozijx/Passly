package com.aozijx.passly.feature.vault.presentation.detail

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.core.error.model.Conflict
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
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.history.EntryRevisionDifference
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.sensitive.RevealedRevisionSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.otp.OtpResult
import com.aozijx.passly.domain.entry.policy.DefaultEntryFieldReader
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import com.aozijx.passly.domain.entry.port.EntryTagQuery
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.detail.DetailSessionQuery
import com.aozijx.passly.feature.vault.entry.EditEntryUseCase
import com.aozijx.passly.feature.vault.detail.ExportEntryQrUseCase
import com.aozijx.passly.feature.vault.detail.LoadEditableEntryUseCase
import com.aozijx.passly.feature.vault.detail.RevealEntryFieldsUseCase
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldUseCase
import com.aozijx.passly.feature.vault.entry.CopyOtpCodeUseCase
import com.aozijx.passly.feature.vault.history.CompareEntryRevisionUseCase
import com.aozijx.passly.feature.vault.history.RestoreEntryRevisionUseCase
import com.aozijx.passly.feature.vault.history.RevealRevisionFieldsUseCase
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimeFactory
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionComparisonPhase
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionListState
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionPage
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionRestorePhase
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionState
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionLockedException
import com.aozijx.passly.runtime.session.SessionStateProvider
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    @Test
    fun `edit preparation failure is reported and can be retried`() =
        runTest {
            var failure: Throwable? = null
            val fixture =
                Fixture(
                    sensitiveFieldRepository = sensitiveFieldsThatFailWhenRequested { failure },
                )
            val store = ViewModelStore().apply { put("detail", fixture.viewModel) }
            try {
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()
                failure = IllegalStateException("temporary read failure")

                assertEquals(null, fixture.viewModel.prepareEntryForEdit())
                assertEquals(DetailEffect.EditPreparationFailed(ENTRY_ID), fixture.viewModel.effects.first())

                failure = null
                assertTrue(fixture.viewModel.prepareEntryForEdit() is EntryEditorSeed.Editable)
            } finally {
                store.clear()
            }
        }

    @Test
    fun `edit preparation propagates cancellation`() =
        runTest {
            val cancelled = CancellationException("cancelled read")
            var failure: Throwable? = null
            val fixture =
                Fixture(
                    sensitiveFieldRepository = sensitiveFieldsThatFailWhenRequested { failure },
                )
            val store = ViewModelStore().apply { put("detail", fixture.viewModel) }
            try {
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()
                failure = cancelled

                val thrown =
                    try {
                        fixture.viewModel.prepareEntryForEdit()
                        null
                    } catch (failure: CancellationException) {
                        failure
                    }

                assertSame(cancelled, thrown)
            } finally {
                store.clear()
            }
        }

    @Test
    fun `edit preparation propagates fatal errors`() =
        runTest {
            val fatal = LinkageError("fatal read failure")
            var failure: Throwable? = null
            val fixture =
                Fixture(
                    sensitiveFieldRepository = sensitiveFieldsThatFailWhenRequested { failure },
                )
            val store = ViewModelStore().apply { put("detail", fixture.viewModel) }
            try {
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()
                failure = fatal

                val thrown =
                    try {
                        fixture.viewModel.prepareEntryForEdit()
                        null
                    } catch (failure: LinkageError) {
                        failure
                    }

                assertSame(fatal, thrown)
            } finally {
                store.clear()
            }
        }

    @Test
    fun `returning to OTP detail clears old code while reloading current config`() =
        runTest {
            val reloadGate = CompletableDeferred<Unit>()
            val repository =
                MutableOtpConfigRepository(
                    config = otpConfig("JBSWY3DPEHPK3PXP"),
                    blockReadNumber = 2,
                    readGate = reloadGate,
                )
            val fixture = Fixture(initialEntry = otpEntry(), otpConfigRepository = repository)
            val store = ViewModelStore()
            try {
                store.put("detail", fixture.viewModel)
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()
                val oldCode =
                    fixture.viewModel.otpState.value as? OtpCodeState.Ready
                        ?: error("Expected the initial OTP code")

                repository.config = otpConfig("MFRGGZDFMZTWQ2LK")
                fixture.viewModel.refreshOtpConfiguration()
                runCurrent()

                assertEquals(null, fixture.viewModel.otpState.value)
                assertEquals(2, repository.readCount)

                reloadGate.complete(Unit)
                runCurrent()

                val refreshedCode = fixture.viewModel.otpState.value as OtpCodeState.Ready
                assertTrue(oldCode.code != refreshedCode.code)
            } finally {
                store.clear()
            }
        }

    @Test
    fun `successful OTP revision restore clears and reloads detail code`() =
        runTest {
            val reloadGate = CompletableDeferred<Unit>()
            val repository =
                MutableOtpConfigRepository(
                    config = otpConfig("JBSWY3DPEHPK3PXP"),
                    blockReadNumber = 2,
                    readGate = reloadGate,
                )
            val fixture =
                Fixture(
                    initialEntry = otpEntry(),
                    otpConfigRepository = repository,
                    revisionSnapshot = otpRevisionSnapshot("MFRGGZDFMZTWQ2LK"),
                )
            fixture.revisions.onRestoreCommitted = { repository.config = otpConfig("MFRGGZDFMZTWQ2LK") }
            val store = ViewModelStore().apply { put("detail", fixture.viewModel) }
            try {
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()
                val oldCode = fixture.viewModel.otpState.value as OtpCodeState.Ready
                fixture.viewModel.onAction(DetailUiAction.OpenRevisionHistory)
                runCurrent()
                fixture.viewModel.onAction(DetailUiAction.SelectRevision(REVISION_ID.value))
                runCurrent()
                fixture.viewModel.onAction(DetailUiAction.RequestRevisionRestore)
                fixture.viewModel.onAction(DetailUiAction.ConfirmRevisionRestore)
                runCurrent()

                fixture.revisions.releasePostCommitCleanup.complete(Unit)
                runCurrent()

                assertTrue(fixture.revisions.restoreCompleted)
                assertEquals(null, fixture.viewModel.otpState.value)
                assertEquals(2, repository.readCount)

                reloadGate.complete(Unit)
                runCurrent()

                val restoredCode = fixture.viewModel.otpState.value as OtpCodeState.Ready
                assertTrue(oldCode.code != restoredCode.code)
            } finally {
                reloadGate.complete(Unit)
                fixture.revisions.releasePostCommitCleanup.complete(Unit)
                store.clear()
            }
        }

    @Test
    fun `failed OTP revision restore keeps the current code and config load`() =
        runTest {
            val repository =
                MutableOtpConfigRepository(
                    config = otpConfig("JBSWY3DPEHPK3PXP"),
                    blockReadNumber = Int.MAX_VALUE,
                    readGate = CompletableDeferred(),
                )
            val fixture = Fixture(initialEntry = otpEntry(), otpConfigRepository = repository)
            fixture.revisions.failRestore = true
            val store = ViewModelStore().apply { put("detail", fixture.viewModel) }
            try {
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()
                val originalCode = fixture.viewModel.otpState.value as OtpCodeState.Ready
                fixture.viewModel.onAction(DetailUiAction.OpenRevisionHistory)
                runCurrent()
                fixture.viewModel.onAction(DetailUiAction.SelectRevision(REVISION_ID.value))
                runCurrent()
                fixture.viewModel.onAction(DetailUiAction.RequestRevisionRestore)
                fixture.viewModel.onAction(DetailUiAction.ConfirmRevisionRestore)
                runCurrent()

                assertTrue(originalCode.code == (fixture.viewModel.otpState.value as OtpCodeState.Ready).code)
                assertEquals(1, repository.readCount)
                assertEquals(1, fixture.revisions.restoreCalls)
            } finally {
                store.clear()
            }
        }

    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun packagePickerRecoversAfterTransientDirectoryFailure() =
        runTest {
            var queries = 0
            val fixture =
                Fixture(
                    installedApps =
                        installedAppsDirectory {
                            if (++queries == 1) throw IllegalStateException("package query failed")
                            listOf(InstalledAppMetadata("Mail", "com.example.mail"))
                        },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()
                model.onAction(DetailUiAction.LoadPackagePickerApps)
                runCurrent()
                assertTrue(model.uiState.value.packagePickerAppsLoadFailed)
                model.onAction(DetailUiAction.LoadPackagePickerApps)
                runCurrent()

                assertEquals(2, queries)
                assertTrue(model.uiState.value.packagePickerAppsLoaded)
                assertFalse(model.uiState.value.packagePickerAppsLoadFailed)
                assertEquals(
                    "com.example.mail",
                    model.uiState.value.packagePickerApps
                        .single()
                        .packageName,
                )
            } finally {
                store.clear()
            }
        }

    @Test
    fun latePickerFailureCannotChangeTheReplacementEntryState() =
        runTest {
            val failureStarted = CompletableDeferred<Unit>()
            val releaseFailure = CompletableDeferred<Unit>()
            val fixture =
                Fixture(
                    installedApps =
                        installedAppsDirectory {
                            failureStarted.complete(Unit)
                            withContext(NonCancellable) { releaseFailure.await() }
                            throw IllegalStateException("old entry query failed")
                        },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()
                model.onAction(DetailUiAction.LoadPackagePickerApps)
                runCurrent()
                failureStarted.await()

                model.load(OTHER_ENTRY_ID.value)
                runCurrent()
                releaseFailure.complete(Unit)
                runCurrent()

                assertEquals(
                    "Other title",
                    model.uiState.value.entry
                        ?.title,
                )
                assertFalse(model.uiState.value.packagePickerAppsLoadFailed)
            } finally {
                store.clear()
            }
        }

    @Test
    fun cancelledPickerLoadDoesNotBecomeFailureState() =
        runTest {
            val loadStarted = CompletableDeferred<Unit>()
            val fixture =
                Fixture(
                    installedApps =
                        installedAppsDirectory {
                            loadStarted.complete(Unit)
                            awaitCancellation()
                        },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()
                model.onAction(DetailUiAction.LoadPackagePickerApps)
                runCurrent()
                loadStarted.await()

                model.load(OTHER_ENTRY_ID.value)
                runCurrent()

                assertFalse(model.uiState.value.packagePickerAppsLoadFailed)
            } finally {
                store.clear()
            }
        }

    @Test
    fun packagePickerFatalErrorEscapesRecoveryBoundary() {
        val fatal = AssertionError("package query failed fatally")
        val thrown =
            assertThrows(AssertionError::class.java) {
                runTest {
                    val model =
                        Fixture(
                            installedApps = installedAppsDirectory { throw fatal },
                        ).viewModel
                    val store = ViewModelStore().apply { put("detail", model) }
                    try {
                        model.load(ENTRY_ID.value)
                        runCurrent()
                        model.onAction(DetailUiAction.LoadPackagePickerApps)
                        runCurrent()
                    } finally {
                        store.clear()
                    }
                }
            }

        assertSame(fatal, thrown)
    }

    @Test
    fun activityHistoryFailureCanBeRetriedAfterAnEmission() =
        runTest {
            var subscriptions = 0
            val activity =
                EntryActivity(
                    entryId = ENTRY_ID.value,
                    activityType = ActivityType.AUTOFILL,
                    createdAt = 10L,
                )
            val fixture =
                Fixture(
                    activityQueryRepository =
                        object : ActivityQueryRepository {
                            override fun observeByEntryId(entryId: String): Flow<List<EntryActivity>> =
                                flow {
                                    if (++subscriptions == 1) {
                                        emit(listOf(activity))
                                        throw IllegalStateException("history query failed")
                                    }
                                    emit(emptyList())
                                }

                            override fun observeAll(): Flow<List<EntryActivity>> = flowOf(emptyList())

                            override fun observeByType(activityType: ActivityType): Flow<List<EntryActivity>> = flowOf(emptyList())
                        },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()
                assertEquals(listOf(activity), model.uiState.value.history)
                assertEquals(DetailActivityHistoryState.Failed, model.uiState.value.activityHistory)
                assertEquals(1, subscriptions)

                model.onAction(DetailUiAction.RetryActivityHistory)
                runCurrent()

                assertEquals(2, subscriptions)
                assertEquals(emptyList<EntryActivity>(), model.uiState.value.history)
                assertEquals(DetailActivityHistoryState.Ready, model.uiState.value.activityHistory)
            } finally {
                store.clear()
            }
        }

    @Test
    fun revisionMetadataFailureCanBeRetried() =
        runTest {
            var subscriptions = 0
            val fixture =
                Fixture(
                    revisionMetadataFlow = {
                        if (++subscriptions == 1) {
                            flow { throw IllegalStateException("revision query failed") }
                        } else {
                            flowOf(listOf(revisionMetadata()))
                        }
                    },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()
                model.onAction(DetailUiAction.OpenRevisionHistory)
                runCurrent()

                val failed = model.uiState.value.revisions as DetailRevisionState.Open
                assertTrue(failed.revisions is DetailRevisionListState.Failed)

                model.onAction(DetailUiAction.RetryRevisionHistory)
                runCurrent()

                val ready = model.uiState.value.revisions as DetailRevisionState.Open
                assertEquals(2, subscriptions)
                assertTrue(ready.revisions is DetailRevisionListState.Ready)
            } finally {
                store.clear()
            }
        }

    @Test
    fun activityHistorySessionLockIsNotReportedAsQueryFailure() =
        runTest {
            val fixture =
                Fixture(
                    activityQueryRepository =
                        object : ActivityQueryRepository {
                            override fun observeByEntryId(entryId: String): Flow<List<EntryActivity>> =
                                flow { throw SessionLockedException() }

                            override fun observeAll(): Flow<List<EntryActivity>> = flowOf(emptyList())

                            override fun observeByType(activityType: ActivityType): Flow<List<EntryActivity>> = flowOf(emptyList())
                        },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()

                assertEquals(DetailActivityHistoryState.Loading, model.uiState.value.activityHistory)
            } finally {
                store.clear()
            }
        }

    @Test
    fun lateActivityHistoryFailureCannotAffectReplacementEntry() =
        runTest {
            val oldObserverStarted = CompletableDeferred<Unit>()
            val releaseOldObserver = CompletableDeferred<Unit>()
            val fixture =
                Fixture(
                    activityQueryRepository =
                        object : ActivityQueryRepository {
                            override fun observeByEntryId(entryId: String): Flow<List<EntryActivity>> =
                                flow {
                                    if (entryId == ENTRY_ID.value) {
                                        oldObserverStarted.complete(Unit)
                                        withContext(NonCancellable) { releaseOldObserver.await() }
                                        throw IllegalStateException("stale activity query failed")
                                    }
                                    emit(emptyList())
                                }

                            override fun observeAll(): Flow<List<EntryActivity>> = flowOf(emptyList())

                            override fun observeByType(activityType: ActivityType): Flow<List<EntryActivity>> = flowOf(emptyList())
                        },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()
                oldObserverStarted.await()

                model.load(OTHER_ENTRY_ID.value)
                runCurrent()
                releaseOldObserver.complete(Unit)
                runCurrent()

                assertEquals(
                    "Other title",
                    model.uiState.value.entry
                        ?.title,
                )
                assertEquals(DetailActivityHistoryState.Ready, model.uiState.value.activityHistory)
            } finally {
                releaseOldObserver.complete(Unit)
                store.clear()
            }
        }

    @Test
    fun revisionMetadataFatalErrorEscapesRecoveryBoundary() {
        val fatal = AssertionError("revision query failed fatally")
        val thrown =
            assertThrows(AssertionError::class.java) {
                runTest {
                    val model =
                        Fixture(
                            revisionMetadataFlow = { flow { throw fatal } },
                        ).viewModel
                    val store = ViewModelStore().apply { put("detail", model) }
                    try {
                        model.load(ENTRY_ID.value)
                        runCurrent()
                        model.onAction(DetailUiAction.OpenRevisionHistory)
                        runCurrent()
                    } finally {
                        store.clear()
                    }
                }
            }

        assertSame(fatal, thrown)
    }

    @Test
    fun closedRevisionObserverCannotFailAReopenedHistorySheet() =
        runTest {
            var subscriptions = 0
            val oldObserverStarted = CompletableDeferred<Unit>()
            val releaseOldObserver = CompletableDeferred<Unit>()
            val fixture =
                Fixture(
                    revisionMetadataFlow = {
                        if (++subscriptions == 1) {
                            flow {
                                oldObserverStarted.complete(Unit)
                                withContext(NonCancellable) { releaseOldObserver.await() }
                                throw IllegalStateException("stale revision query failed")
                            }
                        } else {
                            flowOf(listOf(revisionMetadata()))
                        }
                    },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()
                model.onAction(DetailUiAction.OpenRevisionHistory)
                runCurrent()
                oldObserverStarted.await()

                model.onAction(DetailUiAction.DismissRevisionHistory)
                runCurrent()
                model.onAction(DetailUiAction.OpenRevisionHistory)
                runCurrent()
                assertTrue((model.uiState.value.revisions as DetailRevisionState.Open).revisions is DetailRevisionListState.Ready)

                releaseOldObserver.complete(Unit)
                runCurrent()

                assertTrue((model.uiState.value.revisions as DetailRevisionState.Open).revisions is DetailRevisionListState.Ready)
                assertEquals(2, subscriptions)
            } finally {
                releaseOldObserver.complete(Unit)
                store.clear()
            }
        }

    @Test
    fun detailSnapshotFailureCanBeRetriedWithoutBecomingNotFound() =
        runTest {
            var failNextRead = true
            val fixture =
                Fixture(
                    entryQueryRepository = { entries ->
                        object : EntryQueryRepository {
                            override suspend fun getById(entryId: EntryId): Entry? {
                                if (failNextRead) {
                                    failNextRead = false
                                    throw IllegalStateException("entry query failed")
                                }
                                return entries.getById(entryId)
                            }
                        }
                    },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()

                assertEquals(DetailSnapshotState.Failed, model.uiState.value.snapshotState)
                assertEquals(null, model.uiState.value.entry)

                model.onAction(DetailUiAction.RetryDetailLoad)
                runCurrent()

                assertEquals(DetailSnapshotState.Ready, model.uiState.value.snapshotState)
                assertEquals(
                    "Current title",
                    model.uiState.value.entry
                        ?.title,
                )
            } finally {
                store.clear()
            }
        }

    @Test
    fun missingEntryHasASeparateSnapshotState() =
        runTest {
            val fixture =
                Fixture(
                    entryQueryRepository = {
                        object : EntryQueryRepository {
                            override suspend fun getById(entryId: EntryId): Entry? = null
                        }
                    },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()

                assertEquals(DetailSnapshotState.NotFound, model.uiState.value.snapshotState)
                assertEquals(null, model.uiState.value.entry)
            } finally {
                store.clear()
            }
        }

    @Test
    fun parallelSnapshotQueriesFailAtomicallyAndRetry() =
        runTest {
            var failPresence = true
            assertSnapshotFailureThenRetry(
                Fixture(
                    sensitiveFieldRepository =
                        sensitiveFields { entryId ->
                            if (failPresence) {
                                failPresence = false
                                throw IllegalStateException("sensitive presence query failed")
                            }
                            SensitiveFieldPresence(entryId, emptySet())
                        },
                ),
            )

            var failLinks = true
            assertSnapshotFailureThenRetry(
                Fixture(
                    entryLinkRepository =
                        object : EntryLinkRepository by EmptyEntryLinkRepository {
                            override suspend fun getAll(): List<EntryLink> {
                                if (failLinks) {
                                    failLinks = false
                                    throw IllegalStateException("entry links query failed")
                                }
                                return emptyList()
                            }
                        },
                ),
            )

            var failMetadata = true
            assertSnapshotFailureThenRetry(
                Fixture(
                    initialEntry =
                        currentEntry().let { entry ->
                            entry.copy(
                                profile =
                                    entry.profile.copy(
                                        associations = EntryAssociations(applicationIds = setOf("example.app")),
                                    ),
                            )
                        },
                    installedApps =
                        installedAppDirectory { packageName ->
                            if (failMetadata) {
                                failMetadata = false
                                throw IllegalStateException("associated app metadata query failed")
                            }
                            InstalledAppMetadata(label = packageName, packageName = packageName)
                        },
                ),
            )
        }

    @Test
    fun detailSnapshotCancellationDoesNotBecomeFailure() =
        runTest {
            val fixture =
                Fixture(
                    entryQueryRepository = {
                        object : EntryQueryRepository {
                            override suspend fun getById(entryId: EntryId): Entry? = awaitCancellation()
                        }
                    },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()
                model.onAction(DetailUiAction.ClearSensitiveState)
                runCurrent()

                assertEquals(DetailSnapshotState.Loading, model.uiState.value.snapshotState)
                assertEquals(null, model.uiState.value.entry)
            } finally {
                store.clear()
            }
        }

    @Test
    fun detailSnapshotFatalErrorEscapesRecoveryBoundary() {
        val fatal = AssertionError("detail query failed fatally")
        val thrown =
            assertThrows(AssertionError::class.java) {
                runTest {
                    val fixture =
                        Fixture(
                            entryQueryRepository = {
                                object : EntryQueryRepository {
                                    override suspend fun getById(entryId: EntryId): Entry? = throw fatal
                                }
                            },
                        )
                    val model = fixture.viewModel
                    val store = ViewModelStore().apply { put("detail", model) }
                    try {
                        model.load(ENTRY_ID.value)
                        runCurrent()
                    } finally {
                        store.clear()
                    }
                }
            }

        assertSame(fatal, thrown)
    }

    @Test
    fun lateDetailSnapshotFailureCannotReplaceNewEntry() =
        runTest {
            val oldQueryStarted = CompletableDeferred<Unit>()
            val releaseOldQuery = CompletableDeferred<Unit>()
            val fixture =
                Fixture(
                    entryQueryRepository = { entries ->
                        object : EntryQueryRepository {
                            override suspend fun getById(entryId: EntryId): Entry? {
                                if (entryId == ENTRY_ID) {
                                    oldQueryStarted.complete(Unit)
                                    withContext(NonCancellable) { releaseOldQuery.await() }
                                    throw IllegalStateException("stale detail query failed")
                                }
                                return entries.getById(entryId)
                            }
                        }
                    },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()
                oldQueryStarted.await()

                model.load(OTHER_ENTRY_ID.value)
                runCurrent()
                releaseOldQuery.complete(Unit)
                runCurrent()

                assertEquals(
                    "Other title",
                    model.uiState.value.entry
                        ?.title,
                )
                assertEquals(DetailSnapshotState.Ready, model.uiState.value.snapshotState)
            } finally {
                releaseOldQuery.complete(Unit)
                store.clear()
            }
        }

    @Test
    fun restoreCommitSurvivesSnapshotRefreshFailureAndRefreshRetry() =
        runTest {
            var failNextRead = false
            val fixture =
                Fixture(
                    entryQueryRepository = { entries ->
                        object : EntryQueryRepository {
                            override suspend fun getById(entryId: EntryId): Entry? {
                                if (failNextRead) {
                                    failNextRead = false
                                    throw IllegalStateException("post-restore query failed")
                                }
                                return entries.getById(entryId)
                            }
                        }
                    },
                )
            fixture.revisions.onRestoreCommitted = { failNextRead = true }
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                startRestore(model)
                fixture.revisions.releasePostCommitCleanup.complete(Unit)
                runCurrent()

                assertTrue(fixture.revisions.restoreCompleted)
                assertEquals(1, fixture.revisions.restoreCalls)
                assertEquals(DetailSnapshotState.RefreshFailedAfterRestore, model.uiState.value.snapshotState)
                assertEquals(
                    "Current title",
                    model.uiState.value.entry
                        ?.title,
                )
                val revisions = model.uiState.value.revisions as DetailRevisionState.Open
                assertEquals(DetailRevisionPage.List, revisions.page)

                model.onAction(DetailUiAction.RetrySnapshotRefresh)
                runCurrent()

                assertEquals(DetailSnapshotState.Ready, model.uiState.value.snapshotState)
                assertEquals(
                    "Restored title",
                    model.uiState.value.entry
                        ?.title,
                )
                assertEquals(1, fixture.revisions.restoreCalls)
            } finally {
                fixture.revisions.releasePostCommitCleanup.complete(Unit)
                store.clear()
            }
        }

    @Test
    fun backAndDismissDoNotCancelRestoreBeforeItRefreshesTheEntry() =
        runTest {
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
                    (
                        ((viewModel.uiState.value.revisions as DetailRevisionState.Open).page as DetailRevisionPage.Comparison)
                            .phase as DetailRevisionComparisonPhase.Ready
                    ).restore,
                )

                fixture.revisions.releasePostCommitCleanup.complete(Unit)
                runCurrent()

                assertEquals(
                    "Restored title",
                    viewModel.uiState.value.entry
                        ?.title,
                )
                assertTrue(fixture.revisions.restoreCompleted)
            } finally {
                store.clear()
            }
        }

    @Test
    fun reopeningRevisionHistoryDoesNotCancelRestoreInProgress() =
        runTest {
            val fixture = Fixture()
            val viewModel = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", viewModel) }
            try {
                startRestore(viewModel)
                assertTrue(fixture.revisions.restoreEntered.isCompleted)

                viewModel.onAction(DetailUiAction.OpenRevisionHistory)
                runCurrent()

                assertFalse(fixture.revisions.restoreCompleted)
                assertEquals(
                    DetailRevisionRestorePhase.Restoring,
                    (
                        ((viewModel.uiState.value.revisions as DetailRevisionState.Open).page as DetailRevisionPage.Comparison)
                            .phase as DetailRevisionComparisonPhase.Ready
                    ).restore,
                )

                fixture.revisions.releasePostCommitCleanup.complete(Unit)
                runCurrent()

                assertTrue(fixture.revisions.restoreCompleted)
                assertEquals(
                    "Restored title",
                    viewModel.uiState.value.entry
                        ?.title,
                )
            } finally {
                store.clear()
            }
        }

    @Test
    fun entryReplacementDoesNotPublishLateRestoreIntoTheNewEntry() =
        runTest {
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

                assertEquals(
                    "Other title",
                    viewModel.uiState.value.entry
                        ?.title,
                )
                assertTrue(fixture.revisions.restoreCompleted)
            } finally {
                store.clear()
            }
        }

    @Test
    fun sessionClosureDoesNotPublishLateRestoreIntoClearedState() =
        runTest {
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

    private suspend fun TestScope.assertSnapshotFailureThenRetry(fixture: Fixture) {
        val model = fixture.viewModel
        val store = ViewModelStore().apply { put("detail", model) }
        try {
            model.load(ENTRY_ID.value)
            runCurrent()
            assertEquals(DetailSnapshotState.Failed, model.uiState.value.snapshotState)
            assertEquals(null, model.uiState.value.entry)

            model.onAction(DetailUiAction.RetryDetailLoad)
            runCurrent()

            assertEquals(DetailSnapshotState.Ready, model.uiState.value.snapshotState)
            assertEquals(
                "Current title",
                model.uiState.value.entry
                    ?.title,
            )
        } finally {
            store.clear()
        }
    }

    private class Fixture(
        installedApps: InstalledAppDirectory = EmptyInstalledAppDirectory,
        activityQueryRepository: ActivityQueryRepository = EmptyActivityQueryRepository,
        revisionMetadataFlow: (EntryId) -> Flow<List<EntryRevisionMetadata>> = { flowOf(listOf(revisionMetadata())) },
        entryQueryRepository: (MutableEntries) -> EntryQueryRepository = { it },
        sensitiveFieldRepository: SensitiveFieldRepository = EmptySensitiveFieldRepository(),
        entryLinkRepository: EntryLinkRepository = EmptyEntryLinkRepository,
        initialEntry: Entry = currentEntry(),
        otpConfigRepository: OtpConfigRepository = NoOtpConfigRepository,
        revisionSnapshot: RedactedEntryRevision = restoredSnapshot(),
    ) {
        val access = AuthenticatedAccess()
        val entries = MutableEntries(initialEntry)
        private val entryQuery = entryQueryRepository(entries)
        val revisions = BlockingRevisionRepository(entries, revisionMetadataFlow, revisionSnapshot)
        private val sensitive = sensitiveFieldRepository
        private val authGate = UnusedAuthorizationGate
        private val activity = NoOpActivityRecorder
        private val fieldReader: EntryFieldReader = DefaultEntryFieldReader()
        private val clipboard = SensitiveClipboardWriter { }
        private val sessionProvider = UnlockedSessionProvider()
        private val freshness = FreshnessState()
        private val installedAppDirectory = installedApps
        private val favicon = UnusedFaviconImageProcessor

        val viewModel =
            DetailViewModel(
                entryTagQuery = EmptyEntryTagQuery,
                activityQueryRepository = activityQueryRepository,
                exportEntryQr = ExportEntryQrUseCase(authGate, sensitive, activity, testLogger()),
                secureSessionAccessState = access,
                revealEntryFields = RevealEntryFieldsUseCase(authGate, fieldReader, sensitive, activity),
                loadEditableEntry = LoadEditableEntryUseCase(entries, sensitive, authGate, access, activity),
                editEntry = EditEntryUseCase(entries, entries, access, testLogger()),
                copyEntryFieldUseCase =
                    CopyEntryFieldUseCase(
                        authGate,
                        entries,
                        fieldReader,
                        sensitive,
                        clipboard,
                        activity,
                    ),
                copyOtpCodeUseCase = CopyOtpCodeUseCase(authGate, clipboard, activity),
                faviconImageProcessor = favicon,
                presentationLoader =
                    DetailPresentationLoader(
                        DetailSessionQuery(entryQuery, sensitive, entryLinkRepository),
                        installedAppDirectory,
                    ),
                entryRevisionRepository = revisions,
                compareEntryRevision = CompareEntryRevisionUseCase(revisions),
                revealRevisionFields = RevealRevisionFieldsUseCase(revisions, authGate),
                restoreEntryRevision = RestoreEntryRevisionUseCase(revisions, entries, sensitive, authGate),
                otpCodeRuntimeFactory = OtpCodeRuntimeFactory(otpConfigRepository, sessionProvider, freshness),
            )
    }

    private class MutableEntries(
        initial: Entry,
    ) : EntryQueryRepository,
        EntryCommandRepository {
        private val values = mutableMapOf(initial.id to initial)

        init {
            values[OTHER_ENTRY_ID] = currentEntry(OTHER_ENTRY_ID, "Other title")
        }

        override suspend fun getById(entryId: EntryId): Entry? = values[entryId]

        override suspend fun createEntry(entry: Entry): AppResult<EntryId> = AppResult.Success(entry.id)

        override suspend fun updateEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
            changes: EntryUpdate,
        ): AppResult<Unit> {
            val current = requireNotNull(values[id])
            values[id] =
                current.copy(
                    identity = current.identity.copy(version = expectedVersion.next()),
                    profile = changes.profile ?: current.profile,
                    secret = changes.secret ?: current.secret,
                )
            return AppResult.Success(Unit)
        }

        override suspend fun moveToTrash(
            id: EntryId,
            expectedVersion: EntryVersion,
        ) = AppResult.Success(Unit)

        override suspend fun restoreEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
        ) = AppResult.Success(Unit)

        override suspend fun deletePermanently(
            id: EntryId,
            expectedVersion: EntryVersion,
        ) = AppResult.Success(Unit)

        override suspend fun emptyTrash() = AppResult.Success(0)

        fun restoreFrom(
            snapshot: RedactedEntryRevision,
            expectedVersion: EntryVersion,
        ) {
            val current = requireNotNull(values[snapshot.metadata.entryId])
            values[current.id] =
                current.copy(
                    identity = current.identity.copy(version = expectedVersion.next()),
                    profile = snapshot.profile,
                    secret = snapshot.secret,
                )
        }
    }

    private class BlockingRevisionRepository(
        private val entries: MutableEntries,
        private val revisionMetadataFlow: (EntryId) -> Flow<List<EntryRevisionMetadata>>,
        private val revisionSnapshot: RedactedEntryRevision,
    ) : EntryRevisionRepository {
        val restoreEntered = CompletableDeferred<Unit>()
        val releasePostCommitCleanup = CompletableDeferred<Unit>()
        var restoreCompleted = false
        var restoreCalls = 0
        var failRestore = false
        var onRestoreCommitted: () -> Unit = {}

        override suspend fun initializeHistory(): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeMetadata(entryId: EntryId): Flow<List<EntryRevisionMetadata>> = revisionMetadataFlow(entryId)

        override suspend fun loadRedacted(
            entryId: EntryId,
            revisionId: EntryRevisionId,
        ): AppResult<RedactedEntryRevision> = AppResult.Success(revisionSnapshot)

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
            restoreCalls += 1
            if (failRestore) return AppResult.Failure(Conflict())
            entries.restoreFrom(revisionSnapshot, expectedVersion)
            onRestoreCommitted()
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
    }

    private fun sensitiveFields(readPresence: suspend (EntryId) -> SensitiveFieldPresence): SensitiveFieldRepository =
        object : SensitiveFieldRepository {
            override suspend fun getPresence(entryId: EntryId) = readPresence(entryId)

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
        }

    private fun sensitiveFieldsThatFailWhenRequested(failure: () -> Throwable?): SensitiveFieldRepository =
        sensitiveFields { entryId ->
            failure()?.let { throw it }
            SensitiveFieldPresence(entryId, emptySet())
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
        override suspend fun recordUsage(
            entryId: String,
            type: ActivityType,
        ) = AppResult.Success(Unit)

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

    private fun installedAppsDirectory(launchableApps: suspend () -> List<InstalledAppMetadata>): InstalledAppDirectory =
        object : InstalledAppDirectory {
            override suspend fun metadataFor(packageName: String): InstalledAppMetadata? = null

            override suspend fun launchableApps() = launchableApps()
        }

    private fun installedAppDirectory(lookupMetadata: suspend (String) -> InstalledAppMetadata?): InstalledAppDirectory =
        object : InstalledAppDirectory {
            override suspend fun metadataFor(packageName: String) = lookupMetadata(packageName)

            override suspend fun launchableApps(): List<InstalledAppMetadata> = emptyList()
        }

    private object NoOtpConfigRepository : OtpConfigRepository {
        override suspend fun getConfig(entryId: String): OtpConfig? = null

        override suspend fun consumeHotpCode(entryId: String) = OtpResult.Failure(OtpGenerationError.InvalidSecret)
    }

    private class MutableOtpConfigRepository(
        var config: OtpConfig,
        private val blockReadNumber: Int,
        private val readGate: CompletableDeferred<Unit>,
    ) : OtpConfigRepository {
        var readCount = 0

        override suspend fun getConfig(entryId: String): OtpConfig {
            readCount++
            if (readCount == blockReadNumber) readGate.await()
            return config
        }

        override suspend fun consumeHotpCode(entryId: String) = OtpResult.Failure(OtpGenerationError.InvalidSecret)
    }

    private class UnlockedSessionProvider : SessionStateProvider {
        override val lockStateFlow = MutableStateFlow(SecureSessionState.UNLOCKED)
        override val lockGenerationFlow = MutableStateFlow(0L)
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

        override fun discardEditorResources(
            stagedPath: String?,
            pendingInputPath: String?,
            promotedCandidatePath: String?,
        ) = Unit

        override suspend fun stageUpload(uri: Uri): Result<String> = error("Not used")

        override suspend fun stageHttpsUrl(value: String): Result<String> = error("Not used")

        override suspend fun process(
            stagedInputPath: String,
            crop: FaviconCropRequest?,
        ): Result<String> = error("Not used")

        override suspend fun promote(stagedPath: String): Result<String> = error("Not used")

        override fun isStaged(path: String): Boolean = false
    }

    private companion object {
        val ENTRY_ID = EntryId("detail-entry")
        val OTHER_ENTRY_ID = EntryId("other-detail-entry")
        val REVISION_ID = EntryRevisionId("revision-1")

        fun currentEntry(
            id: EntryId = ENTRY_ID,
            title: String = "Current title",
        ) = Entry(
            identity = EntryIdentity(id, EntryType.LOGIN, EntryVersion(2), EntryTimestamps(1L)),
            profile = EntryProfile(title),
            secret = EntrySecret(LoginCredential()),
        )

        fun otpEntry() =
            Entry(
                identity = EntryIdentity(ENTRY_ID, EntryType.OTP, EntryVersion(2), EntryTimestamps(1L)),
                profile = EntryProfile("Current OTP"),
                secret = EntrySecret(OtpCredential(otpConfig("JBSWY3DPEHPK3PXP"))),
            )

        fun otpConfig(secret: String) =
            OtpConfig(
                type = OtpType.TOTP,
                secret = secret,
                digits = 6,
                periodSeconds = 30,
                encoding = OtpSecretEncoding.BASE32,
            )

        fun revisionMetadata() =
            EntryRevisionMetadata(
                id = REVISION_ID,
                entryId = ENTRY_ID,
                version = EntryVersion(1),
                createdAtMs = 1L,
                change = RevisionChange.VALUE_CHANGED,
            )

        fun restoredSnapshot() =
            RedactedEntryRevision(
                metadata = revisionMetadata(),
                type = EntryType.LOGIN,
                profile = EntryProfile("Restored title"),
                secret = EntrySecret(LoginCredential()),
            )

        fun otpRevisionSnapshot(secret: String) =
            RedactedEntryRevision(
                metadata = revisionMetadata(),
                type = EntryType.OTP,
                profile = EntryProfile("Historical OTP"),
                secret = EntrySecret(OtpCredential(otpConfig(secret))),
            )
    }
}
