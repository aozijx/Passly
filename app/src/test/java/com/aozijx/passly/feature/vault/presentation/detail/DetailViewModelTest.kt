package com.aozijx.passly.feature.vault.presentation.detail

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.platform.media.FaviconCropRequest
import com.aozijx.passly.core.platform.media.FaviconEditorResource
import com.aozijx.passly.core.platform.media.FaviconImageProcessor
import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.core.platform.packageinfo.InstalledAppMetadata
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.history.EntryRevisionDifference
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.sensitive.RevealedRevisionSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationMaterial
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import com.aozijx.passly.domain.entry.policy.DefaultEntryFieldReader
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import com.aozijx.passly.domain.entry.port.EntryTagQuery
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.detail.EntryDetailQueryImpl
import com.aozijx.passly.feature.vault.detail.ExportEntryQrUseCase
import com.aozijx.passly.feature.vault.detail.LoadEditableEntryUseCase
import com.aozijx.passly.feature.vault.detail.RelatedPasswordOperationsImpl
import com.aozijx.passly.feature.vault.detail.RevealEntryFieldsUseCase
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldUseCase
import com.aozijx.passly.feature.vault.entry.CopyOtpCodeUseCase
import com.aozijx.passly.feature.vault.entry.RollbackHotpCounterUseCase
import com.aozijx.passly.feature.vault.entry.EditEntryUseCase
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.history.CompareEntryRevisionUseCase
import com.aozijx.passly.feature.vault.history.ObserveEntryRevisionMetadataUseCase
import com.aozijx.passly.feature.vault.history.RestoreEntryRevisionUseCase
import com.aozijx.passly.feature.vault.history.RevealRevisionFieldsUseCase
import com.aozijx.passly.feature.vault.otp.OtpCodeState
import com.aozijx.passly.feature.vault.otp.createTestOtpRuntime
import com.aozijx.passly.feature.vault.otp.generationMaterialForTest
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.presentation.detail.DetailRevisionRestorePhase
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionComparisonPhase
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionListState
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionPage
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionState
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionWorkflowFactory
import com.aozijx.passly.runtime.session.SessionLockedException
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
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
    fun faviconSelectionActionsUpdateSourceInTheViewModelStateOwner() =
        runTest {
            val fixture =
                Fixture(
                    initialEntry =
                        currentEntry().let { entry ->
                            entry.copy(
                                profile =
                                    entry.profile.copy(
                                        icon = EntryIcon(name = "finance.bank", color = "secondary"),
                                    ),
                            )
                        },
                )
            val store = ViewModelStore().apply { put("detail", fixture.viewModel) }
            try {
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()
                fixture.viewModel.onAction(DetailUiAction.OpenFaviconEditor)
                fixture.viewModel.onAction(DetailUiAction.SelectFaviconIcon("security.key"))

                assertEquals(
                    DetailFaviconSource.BuiltIn(
                        "security.key",
                        DetailFaviconSource.ColorSelection.Token("secondary"),
                    ),
                    (fixture.viewModel.uiState.value.faviconEditor as DetailFaviconEditorState.Open).source,
                )

                fixture.viewModel.onAction(
                    DetailUiAction.SelectFaviconColor(DetailFaviconSource.ColorSelection.Automatic),
                )
                assertEquals(
                    DetailFaviconSource.BuiltIn(
                        "security.key",
                        DetailFaviconSource.ColorSelection.Automatic,
                    ),
                    (fixture.viewModel.uiState.value.faviconEditor as DetailFaviconEditorState.Open).source,
                )
            } finally {
                store.clear()
            }
        }

    @Test
    fun sshPrivateKeyActivationRevealsThenCopiesFromCurrentState() =
        runTest {
            var revealCount = 0
            val copiedValues = mutableListOf<String>()
            val sensitiveFields =
                object : SensitiveFieldRepository {
                    override suspend fun getPresence(entryId: EntryId) =
                        SensitiveFieldPresence(entryId, setOf(SensitiveFieldKey.SSH_PRIVATE_KEY))

                    override suspend fun reveal(
                        entryId: EntryId,
                        key: SensitiveFieldKey,
                        action: SensitiveAccessAction,
                        permit: AuthorizationPermit,
                    ) = RevealedSensitiveField(entryId, key, OwnedChars.fromString(SSH_PRIVATE_KEY))

                    override suspend fun revealMany(
                        entryId: EntryId,
                        keys: Set<SensitiveFieldKey>,
                        action: SensitiveAccessAction,
                        permit: AuthorizationPermit,
                    ): List<RevealedSensitiveField> {
                        revealCount += 1
                        return keys.map { key ->
                            RevealedSensitiveField(entryId, key, OwnedChars.fromString(SSH_PRIVATE_KEY))
                        }
                    }

                    override suspend fun readBundle(entryId: EntryId): EntrySecret = EntrySecret(SshCredential())
                }
            val fixture =
                Fixture(
                    initialEntry = sshEntry(),
                    sensitiveFieldRepository = sensitiveFields,
                    authorizationGate = AllowingAuthorizationGate,
                    clipboardWriter = SensitiveClipboardWriter { copiedValues += it },
                )
            val store = ViewModelStore().apply { put("detail", fixture.viewModel) }
            try {
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()

                fixture.viewModel.onAction(DetailUiAction.ActivateSshPrivateKey)
                runCurrent()
                assertEquals(1, revealCount)
                assertTrue(
                    fixture.viewModel.uiState.value
                        .revealed(FieldKey.SSH_KEY) != null,
                )

                fixture.viewModel.onAction(DetailUiAction.ActivateSshPrivateKey)
                runCurrent()
                assertEquals(listOf(SSH_PRIVATE_KEY), copiedValues)
                assertTrue(
                    fixture.viewModel.uiState.value
                        .revealed(FieldKey.SSH_KEY) == null,
                )
            } finally {
                store.clear()
            }
        }

    @Test
    fun `saving an unchanged field draft does not start an edit`() =
        runTest {
            val fixture = Fixture(initialEntry = currentEntry(username = "latest-user"))
            val store = ViewModelStore().apply { put("detail", fixture.viewModel) }
            try {
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()
                val version =
                    fixture.viewModel.uiState.value.snapshotState
                        .entryOrNull()
                        ?.version

                fixture.viewModel.onAction(DetailUiAction.StartFieldEdit(DetailField.USERNAME))
                fixture.viewModel.onAction(DetailUiAction.SaveField(DetailField.USERNAME, "latest-user"))
                runCurrent()

                assertEquals(
                    version,
                    fixture.viewModel.uiState.value.snapshotState
                        .entryOrNull()
                        ?.version,
                )
                assertEquals(DetailSaveState.Idle, fixture.viewModel.uiState.value.saveState)
                assertEquals(
                    "latest-user",
                    (
                        fixture.viewModel.uiState.value.fieldEdits
                            .edit(DetailField.USERNAME.draftKey) as DetailFieldEdit.Editing
                    ).draft,
                )
            } finally {
                store.clear()
            }
        }

    @Test
    fun `saved login can bind another saved login from its detail page`() =
        runTest {
            val links = MutableEntryLinks()
            val fixture =
                Fixture(
                    entryLinkRepository = links,
                    passwordItems =
                        listOf(
                            passwordItem(ENTRY_ID, "Current title"),
                            passwordItem(OTHER_ENTRY_ID, "Other title"),
                        ),
                )
            val store = ViewModelStore().apply { put("detail", fixture.viewModel) }
            try {
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()
                fixture.viewModel.onAction(DetailUiAction.OpenPasswordBinding)
                runCurrent()

                val picker = fixture.viewModel.uiState.value.passwordBinding as RelatedPasswordBindingUiState.Ready
                assertEquals(listOf(OTHER_ENTRY_ID.value), picker.candidates.map { it.id })
                fixture.viewModel.onAction(DetailUiAction.BindPassword(OTHER_ENTRY_ID.value))
                runCurrent()

                assertEquals(EntryRelationType.RELATED_TO, links.saved.single().relationType)
                assertEquals(
                    listOf(OTHER_ENTRY_ID),
                    fixture.viewModel.uiState.value.relatedPasswords
                        .map { it.targetEntryId },
                )
                assertEquals(RelatedPasswordBindingUiState.Closed, fixture.viewModel.uiState.value.passwordBinding)
            } finally {
                store.clear()
            }
        }

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
            val otpLease = fixture.otpRuntime.acquire(ENTRY_ID.value)
            try {
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()
                val oldCode =
                    fixture.otpRuntime.states.value
                        .getValue(ENTRY_ID.value) as OtpCodeState.Ready
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
                assertEquals(null, fixture.otpRuntime.states.value[ENTRY_ID.value])
                assertEquals(2, repository.readCount)

                reloadGate.complete(Unit)
                runCurrent()

                val restoredCode =
                    fixture.otpRuntime.states.value
                        .getValue(ENTRY_ID.value) as OtpCodeState.Ready
                assertTrue(oldCode.code != restoredCode.code)
            } finally {
                otpLease.close()
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
            val otpLease = fixture.otpRuntime.acquire(ENTRY_ID.value)
            try {
                fixture.viewModel.load(ENTRY_ID.value)
                runCurrent()
                val originalCode =
                    fixture.otpRuntime.states.value
                        .getValue(ENTRY_ID.value) as OtpCodeState.Ready
                fixture.viewModel.onAction(DetailUiAction.OpenRevisionHistory)
                runCurrent()
                fixture.viewModel.onAction(DetailUiAction.SelectRevision(REVISION_ID.value))
                runCurrent()
                fixture.viewModel.onAction(DetailUiAction.RequestRevisionRestore)
                fixture.viewModel.onAction(DetailUiAction.ConfirmRevisionRestore)
                runCurrent()

                assertTrue(
                    originalCode.code ==
                        (
                            fixture.otpRuntime.states.value
                                .getValue(ENTRY_ID.value) as OtpCodeState.Ready
                        ).code,
                )
                assertEquals(1, repository.readCount)
                assertEquals(1, fixture.revisions.restoreCalls)
            } finally {
                otpLease.close()
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
                assertEquals(AppPackagePickerUiState.Failed, model.uiState.value.packagePickerApps)
                model.onAction(DetailUiAction.LoadPackagePickerApps)
                runCurrent()

                assertEquals(2, queries)
                val matches = model.uiState.value.packagePickerApps as AppPackagePickerUiState.Matches<DetailInstalledApp>
                assertEquals(
                    "com.example.mail",
                    matches.installedApps
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
                    model.uiState.value.snapshotState
                        .entryOrNull()
                        ?.title,
                )
                assertFalse(model.uiState.value.packagePickerApps is AppPackagePickerUiState.Failed)
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

                assertFalse(model.uiState.value.packagePickerApps is AppPackagePickerUiState.Failed)
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
                        },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()
                assertEquals(
                    DetailActivityHistoryState.Content(
                        activities = listOf(activity),
                        phase = DetailActivityHistoryPhase.Failed,
                    ),
                    model.uiState.value.activityHistory,
                )
                assertEquals(1, subscriptions)

                model.onAction(DetailUiAction.RetryActivityHistory)
                runCurrent()

                assertEquals(2, subscriptions)
                assertEquals(
                    DetailActivityHistoryState.Content(
                        activities = emptyList(),
                        phase = DetailActivityHistoryPhase.Ready,
                    ),
                    model.uiState.value.activityHistory,
                )
            } finally {
                store.clear()
            }
        }

    @Test
    fun activityHistoryFailureBeforeFirstEmissionUsesAnExplicitFailedState() =
        runTest {
            var subscriptions = 0
            val fixture =
                Fixture(
                    activityQueryRepository =
                        object : ActivityQueryRepository {
                            override fun observeByEntryId(entryId: String): Flow<List<EntryActivity>> =
                                if (++subscriptions == 1) {
                                    flow { throw IllegalStateException("history query failed") }
                                } else {
                                    flowOf(emptyList())
                                }
                        },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()

                assertEquals(DetailActivityHistoryState.Failed, model.uiState.value.activityHistory)

                model.onAction(DetailUiAction.RetryActivityHistory)
                runCurrent()

                assertEquals(
                    DetailActivityHistoryState.Content(emptyList(), DetailActivityHistoryPhase.Ready),
                    model.uiState.value.activityHistory,
                )
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
    fun revisionHistoryInitializationFailureCanBeRetriedBeforeMetadataSubscription() =
        runTest {
            var initializations = 0
            var subscriptions = 0
            val fixture =
                Fixture(
                    revisionInitialization = {
                        if (++initializations == 1) AppResult.Failure(Conflict()) else AppResult.Success(Unit)
                    },
                    revisionMetadataFlow = {
                        subscriptions += 1
                        flowOf(listOf(revisionMetadata()))
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
                assertEquals(1, initializations)
                assertEquals(0, subscriptions)

                model.onAction(DetailUiAction.RetryRevisionHistory)
                runCurrent()

                val ready = model.uiState.value.revisions as DetailRevisionState.Open
                assertEquals(2, initializations)
                assertEquals(1, subscriptions)
                assertTrue(ready.revisions is DetailRevisionListState.Ready)
            } finally {
                store.clear()
            }
        }

    @Test
    fun activityHistorySessionLockIsNotReportedAsQueryFailure() =
        runTest {
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
                                    emit(listOf(activity))
                                    throw SessionLockedException()
                                }
                        },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()

                assertEquals(
                    DetailActivityHistoryState.Content(
                        activities = listOf(activity),
                        phase = DetailActivityHistoryPhase.Ready,
                    ),
                    model.uiState.value.activityHistory,
                )

                fixture.access.lock()
                runCurrent()

                assertEquals(DetailSnapshotState.Loading, model.uiState.value.snapshotState)
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
                    model.uiState.value.snapshotState
                        .entryOrNull()
                        ?.title,
                )
                assertEquals(
                    DetailActivityHistoryState.Content(emptyList(), DetailActivityHistoryPhase.Ready),
                    model.uiState.value.activityHistory,
                )
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

                            override suspend fun getByIds(entryIds: List<EntryId>) = entries.getByIds(entryIds)
                        }
                    },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()

                assertEquals(DetailSnapshotState.Failed, model.uiState.value.snapshotState)

                model.onAction(DetailUiAction.RetryDetailLoad)
                runCurrent()

                assertTrue(model.uiState.value.snapshotState is DetailSnapshotState.Content)
                assertEquals(
                    "Current title",
                    model.uiState.value.snapshotState
                        .entryOrNull()
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

                            override suspend fun getByIds(entryIds: List<EntryId>) = emptyList<Entry>()
                        }
                    },
                )
            val model = fixture.viewModel
            val store = ViewModelStore().apply { put("detail", model) }
            try {
                model.load(ENTRY_ID.value)
                runCurrent()

                assertEquals(DetailSnapshotState.NotFound, model.uiState.value.snapshotState)
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

                            override suspend fun getByIds(entryIds: List<EntryId>) = emptyList<Entry>()
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

                                    override suspend fun getByIds(entryIds: List<EntryId>) = emptyList<Entry>()
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

                            override suspend fun getByIds(entryIds: List<EntryId>) = entries.getByIds(entryIds)
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
                    model.uiState.value.snapshotState
                        .entryOrNull()
                        ?.title,
                )
                assertTrue(model.uiState.value.snapshotState is DetailSnapshotState.Content)
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

                            override suspend fun getByIds(entryIds: List<EntryId>) = entries.getByIds(entryIds)
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
                assertEquals(
                    DetailSnapshotRefreshPhase.Failed,
                    (model.uiState.value.snapshotState as DetailSnapshotState.Content).refreshPhase,
                )
                assertEquals(
                    "Current title",
                    model.uiState.value.snapshotState
                        .entryOrNull()
                        ?.title,
                )
                val revisions = model.uiState.value.revisions as DetailRevisionState.Open
                assertEquals(DetailRevisionPage.List, revisions.page)

                model.onAction(DetailUiAction.RetrySnapshotRefresh)
                runCurrent()

                assertEquals(
                    DetailSnapshotRefreshPhase.Current,
                    (model.uiState.value.snapshotState as DetailSnapshotState.Content).refreshPhase,
                )
                assertEquals(
                    "Restored title",
                    model.uiState.value.snapshotState
                        .entryOrNull()
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
                    viewModel.uiState.value.snapshotState
                        .entryOrNull()
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
                    viewModel.uiState.value.snapshotState
                        .entryOrNull()
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
                    viewModel.uiState.value.snapshotState
                        .entryOrNull()
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

                assertEquals(DetailSnapshotState.Loading, viewModel.uiState.value.snapshotState)
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

            model.onAction(DetailUiAction.RetryDetailLoad)
            runCurrent()

            assertTrue(model.uiState.value.snapshotState is DetailSnapshotState.Content)
            assertEquals(
                "Current title",
                model.uiState.value.snapshotState
                    .entryOrNull()
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
        revisionInitialization: () -> AppResult<Unit> = { AppResult.Success(Unit) },
        entryQueryRepository: (MutableEntries) -> EntryQueryRepository = { it },
        sensitiveFieldRepository: SensitiveFieldRepository = EmptySensitiveFieldRepository(),
        entryLinkRepository: EntryLinkRepository = EmptyEntryLinkRepository,
        passwordItems: List<EntryListItem> = emptyList(),
        initialEntry: Entry = currentEntry(),
        otpConfigRepository: OtpConfigRepository = NoOtpConfigRepository,
        revisionSnapshot: RedactedEntryRevision = restoredSnapshot(),
        authorizationGate: AuthorizationGate = UnusedAuthorizationGate,
        clipboardWriter: SensitiveClipboardWriter = SensitiveClipboardWriter { },
    ) {
        val access = AuthenticatedAccess()
        val entries = MutableEntries(initialEntry)
        private val entryQuery = entryQueryRepository(entries)
        val revisions = BlockingRevisionRepository(entries, revisionMetadataFlow, revisionSnapshot, revisionInitialization)
        private val sensitive = sensitiveFieldRepository
        private val authGate = authorizationGate
        private val activity = NoOpActivityRecorder
        private val fieldReader: EntryFieldReader = DefaultEntryFieldReader()
        private val clipboard = clipboardWriter
        private val installedAppDirectory = installedApps
        private val favicon = UnusedFaviconImageProcessor
        val otpRuntime = createTestOtpRuntime(otpConfigRepository)

        val viewModel =
            DetailViewModel(
                entryTagQuery = EmptyEntryTagQuery,
                activityQueryRepository = activityQueryRepository,
                exportEntryQr = ExportEntryQrUseCase(authGate, sensitive, activity, testLogger()),
                secureSessionAccessState = access,
                revealEntryFields = RevealEntryFieldsUseCase(authGate, fieldReader, sensitive, activity),
                loadEditableEntry = LoadEditableEntryUseCase(entries, sensitive, authGate, access, activity),
                editEntry = EditEntryUseCase(entries, entries, access, testLogger()),
                fieldReader = fieldReader,
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
                rollbackHotpCounterUseCase = RollbackHotpCounterUseCase(authGate, otpRuntime),
                faviconImageProcessor = favicon,
                presentationLoader =
                    DetailPresentationLoader(
                        EntryDetailQueryImpl(entryQuery, sensitive, entryLinkRepository),
                        installedAppDirectory,
                    ),
                revisionWorkflowFactory =
                    DetailRevisionWorkflowFactory(
                        observeMetadata = ObserveEntryRevisionMetadataUseCase(revisions),
                        compareRevision = CompareEntryRevisionUseCase(revisions),
                        revealFields = RevealRevisionFieldsUseCase(revisions, authGate),
                        restoreRevision = RestoreEntryRevisionUseCase(revisions, entries, sensitive, authGate),
                    ),
                relatedPasswordOperations =
                    RelatedPasswordOperationsImpl(
                        TestPasswordListQueryRepository(passwordItems),
                        entryLinkRepository,
                        testLogger(),
                    ),
                logger = testLogger(),
                otpRuntime = otpRuntime,
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

        override suspend fun getByIds(entryIds: List<EntryId>): List<Entry> = entryIds.mapNotNull(values::get)

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

    private class TestPasswordListQueryRepository(
        private val items: List<EntryListItem>,
    ) : EntryListQueryRepository {
        override val availableCategories: Flow<List<String>> = emptyFlow()
        override val deletedEntries: Flow<List<EntryListItem>> = emptyFlow()

        override suspend fun getActiveByType(entryType: EntryType): List<EntryListItem> = items.filter { it.entryType == entryType }
    }

    private class MutableEntryLinks : EntryLinkRepository {
        val saved = mutableListOf<EntryLink>()

        override fun observeAll(): Flow<List<EntryLink>> = flowOf(saved)

        override fun observeLinks(entryId: EntryId): Flow<List<EntryLink>> =
            flowOf(
                saved.filter {
                    it.sourceEntryId == entryId || it.targetEntryId == entryId
                },
            )

        override suspend fun getAll(): List<EntryLink> = saved.toList()

        override suspend fun getLinks(entryId: EntryId): List<EntryLink> =
            saved.filter {
                it.sourceEntryId == entryId || it.targetEntryId == entryId
            }

        override suspend fun upsert(link: EntryLink): AppResult<Unit> {
            saved += link
            return AppResult.Success(Unit)
        }

        override suspend fun delete(linkId: EntryLinkId): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class BlockingRevisionRepository(
        private val entries: MutableEntries,
        private val revisionMetadataFlow: (EntryId) -> Flow<List<EntryRevisionMetadata>>,
        private val revisionSnapshot: RedactedEntryRevision,
        private val revisionInitialization: () -> AppResult<Unit>,
    ) : EntryRevisionRepository {
        val restoreEntered = CompletableDeferred<Unit>()
        val releasePostCommitCleanup = CompletableDeferred<Unit>()
        var restoreCompleted = false
        var restoreCalls = 0
        var failRestore = false
        var onRestoreCommitted: () -> Unit = {}

        override suspend fun initializeHistory(): AppResult<Unit> = revisionInitialization()

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
    }

    private object NoOpActivityRecorder : ActivityRecorder {
        override suspend fun recordUsage(
            entryId: String,
            type: ActivityType,
            affectedFields: Set<FieldKey>,
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

    private object AllowingAuthorizationGate : AuthorizationGate {
        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> = AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
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
        override suspend fun getGenerationMaterial(entryId: String): OtpGenerationMaterial? = null

        override suspend fun consumeHotpCode(entryId: String, expectedCounter: Long) =
            OtpResult.Failure(OtpGenerationError.InvalidSecret)

        override suspend fun rollbackHotpCounter(entryId: String, expectedCurrentCounter: Long) = false
    }

    private class MutableOtpConfigRepository(
        var config: OtpConfig,
        private val blockReadNumber: Int,
        private val readGate: CompletableDeferred<Unit>,
    ) : OtpConfigRepository {
        var readCount = 0

        override suspend fun getGenerationMaterial(entryId: String): OtpGenerationMaterial {
            readCount++
            if (readCount == blockReadNumber) readGate.await()
            return generationMaterialForTest(config)
        }

        override suspend fun consumeHotpCode(entryId: String, expectedCounter: Long) =
            OtpResult.Failure(OtpGenerationError.InvalidSecret)

        override suspend fun rollbackHotpCounter(entryId: String, expectedCurrentCounter: Long) = false
    }

    private class AuthenticatedAccess : SecureSessionAccessState {
        private val mutableAuthenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))
        override val authenticationState: StateFlow<AuthenticationState> = mutableAuthenticationState
        private var unlocked = true

        override fun isUnlocked() = unlocked

        fun lock() {
            unlocked = false
            mutableAuthenticationState.value = AuthenticationState.Locked
        }
    }

    private object UnusedFaviconImageProcessor : FaviconImageProcessor {
        override suspend fun discard(resource: FaviconEditorResource) = Unit

        override fun discardEditorResources(resources: Set<FaviconEditorResource>) = Unit

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
            username: String = "",
        ) = Entry(
            identity = EntryIdentity(id, EntryType.LOGIN, EntryVersion(2), EntryTimestamps(1L)),
            profile = EntryProfile(title, username),
            secret = EntrySecret(LoginCredential()),
        )

        fun sshEntry() =
            Entry(
                identity = EntryIdentity(ENTRY_ID, EntryType.SSH_KEY, EntryVersion(2), EntryTimestamps(1L)),
                profile = EntryProfile("Current SSH"),
                secret = EntrySecret(SshCredential()),
            )

        const val SSH_PRIVATE_KEY = "private-key"

        fun passwordItem(
            id: EntryId,
            title: String,
        ) = EntryListItem(
            identity = EntryIdentity(id, EntryType.LOGIN, timestamps = EntryTimestamps(1L)),
            profile = EntryProfile(title),
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
