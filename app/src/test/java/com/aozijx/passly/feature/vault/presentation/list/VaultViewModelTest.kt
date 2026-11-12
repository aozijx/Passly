package com.aozijx.passly.feature.vault.presentation.list

import androidx.lifecycle.ViewModelStore
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.aozijx.passly.R
import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.result.AppResult
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
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.otp.generation.OtpGenerationMaterial
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.port.EntryPagingRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.port.CorruptSettingsDataException
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldUseCase
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.model.entryTypes
import com.aozijx.passly.feature.vault.otp.CopyOtpCodeUseCase
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimePort
import com.aozijx.passly.feature.vault.otp.OtpCodeState
import com.aozijx.passly.feature.vault.otp.OtpLease
import com.aozijx.passly.feature.vault.otp.createTestOtpRuntime
import com.aozijx.passly.feature.vault.otp.generationMaterialForTest
import com.aozijx.passly.feature.vault.otp.presentation.OtpPresentation
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.trash.TrashCommandUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModelTest {
    @get:Rule
    val mainDispatcherRule =
        com.aozijx.passly.testing
            .MainDispatcherRule()

    @Test
    fun categoryQueryFailureDoesNotEscapeTheViewModelScope() =
        runTest(mainDispatcherRule.testDispatcher) {
            var attempts = 0
            val viewModel =
                createViewModel(
                    PausingAuthorizationGate(),
                    mutableListOf(),
                    availableCategories =
                        flow {
                            attempts++
                            if (attempts == 1) throw IllegalStateException("category read failed")
                            emit(listOf("Work"))
                        },
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            var escapedFailure: IllegalStateException? = null

            try {
                try {
                    runCurrent()
                } catch (failure: IllegalStateException) {
                    escapedFailure = failure
                }

                assertNull("a repository observation failure must become recoverable state", escapedFailure)
                assertEquals(VaultCategoriesState.Failed, viewModel.uiState.value.categories)

                viewModel.onAction(VaultUiAction.RetryCategories)
                runCurrent()

                assertEquals(VaultCategoriesState.Ready(listOf("Work")), viewModel.uiState.value.categories)
                assertEquals(2, attempts)
            } finally {
                viewModelStore.clear()
            }
        }

    @Test
    fun categoryQueryFailureAfterAnEmissionCanBeRetried() =
        runTest(mainDispatcherRule.testDispatcher) {
            var attempts = 0
            val viewModel =
                createViewModel(
                    PausingAuthorizationGate(),
                    mutableListOf(),
                    availableCategories =
                        flow {
                            attempts++
                            if (attempts == 1) {
                                emit(listOf("Work"))
                                throw IllegalStateException("category stream failed after emission")
                            }
                            emit(listOf("Personal"))
                        },
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }

            try {
                runCurrent()

                assertEquals(VaultCategoriesState.Failed, viewModel.uiState.value.categories)

                viewModel.onAction(VaultUiAction.RetryCategories)
                runCurrent()

                assertEquals(VaultCategoriesState.Ready(listOf("Personal")), viewModel.uiState.value.categories)
                assertEquals(2, attempts)
            } finally {
                viewModelStore.clear()
            }
        }

    @Test
    fun categoryQueryDistinguishesEmptyAndLaterValues() =
        runTest(mainDispatcherRule.testDispatcher) {
            val categories = MutableSharedFlow<List<String>>(replay = 1)
            val viewModel =
                createViewModel(
                    PausingAuthorizationGate(),
                    mutableListOf(),
                    availableCategories = categories,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }

            try {
                runCurrent()
                categories.emit(emptyList())
                runCurrent()
                assertEquals(VaultCategoriesState.ReadyEmpty, viewModel.uiState.value.categories)

                categories.emit(listOf("Work"))
                runCurrent()
                assertEquals(VaultCategoriesState.Ready(listOf("Work")), viewModel.uiState.value.categories)
            } finally {
                viewModelStore.clear()
            }
        }

    @Test
    fun duplicateDeleteForTheSameEntryIsSingleFlight() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = PausingAuthorizationGate()
            val movedEntries = mutableListOf<EntryId>()
            val viewModel = createViewModel(gate, mutableListOf(), onMoveToTrash = movedEntries::add)
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            viewModel.onPageEntered()

            try {
                viewModel.onAction(VaultUiAction.QuickDelete(ENTRY_ID.value))
                gate.started.await()
                viewModel.onAction(VaultUiAction.QuickDelete(ENTRY_ID.value))
                gate.resume.complete(Unit)
                runCurrent()

                assertEquals(listOf(ENTRY_ID), movedEntries)
            } finally {
                gate.resume.complete(Unit)
                viewModelStore.clear()
            }
        }

    @Test
    fun deleteFailureCompletingAfterPageExitDoesNotEmitStalePageEffect() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = PausingAuthorizationGate()
            val viewModel =
                createViewModel(
                    gate,
                    mutableListOf(),
                    moveToTrashResult = AppResult.Failure(NotFound()),
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val owner = viewModel.onPageEntered()
            val effects = Channel<VaultEffect>(Channel.UNLIMITED)
            val effectCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.effects.collect { effects.send(it) }
                }

            try {
                viewModel.onAction(VaultUiAction.QuickDelete(ENTRY_ID.value))
                gate.started.await()
                viewModel.onPageExited(owner)
                gate.resume.complete(Unit)
                runCurrent()

                assertNull("a departed page must not receive its delete failure", effects.tryReceive().getOrNull())
            } finally {
                gate.resume.complete(Unit)
                effectCollector.cancel()
                viewModelStore.clear()
            }
        }

    @Test
    fun routeExitWhileCopyWaitsForAuthorizationPreventsClipboardWriteAndEffect() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = PausingAuthorizationGate()
            val clipboardValues = mutableListOf<String>()
            val viewModel = createViewModel(gate, clipboardValues)
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val owner = viewModel.onPageEntered()
            val effects = Channel<VaultEffect>(Channel.UNLIMITED)
            val effectCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.effects.collect(effects::send)
                }

            try {
                viewModel.onAction(
                    VaultUiAction.CopyField(
                        entryId = ENTRY_ID.value,
                        entryType = EntryType.ACCOUNT,
                        fieldKey = FieldKey.USERNAME,
                    ),
                )
                gate.started.await()
                viewModel.onPageExited(owner)
                gate.resume.complete(Unit)
                runCurrent()

                assertTrue("a departed page must not write its pending copy", clipboardValues.isEmpty())
                assertTrue("a departed page must not emit a copy effect", effects.tryReceive().isFailure)
            } finally {
                gate.resume.complete(Unit)
                effectCollector.cancel()
                viewModelStore.clear()
            }
        }

    @Test
    fun routeExitWhileOtpCopyWaitsForAuthorizationPreventsClipboardWrite() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = PausingAuthorizationGate()
            val clipboardValues = mutableListOf<String>()
            val viewModel = createViewModel(gate, clipboardValues, ReadyOtpConfigRepository)
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val owner = viewModel.onPageEntered()
            val otpStateCollector = backgroundScope.collectOtpState(viewModel, ENTRY_ID.value)

            try {
                runCurrent()
                assertTrue(viewModel.otpStates.value[ENTRY_ID.value] is OtpCodeState.TimedReady)

                viewModel.onAction(VaultUiAction.CopyOtp(ENTRY_ID.value))
                gate.started.await()
                viewModel.onPageExited(owner)
                gate.resume.complete(Unit)
                runCurrent()

                assertTrue("a departed page must not write its pending OTP", clipboardValues.isEmpty())
                assertTrue(
                    "Vault graph retains OTP state across row and page transitions",
                    viewModel.otpStates.value[ENTRY_ID.value] is OtpCodeState.TimedReady,
                )
            } finally {
                gate.resume.complete(Unit)
                otpStateCollector.cancel()
                runCurrent()
                viewModelStore.clear()
            }
        }

    @Test
    fun vaultHOTPcopyConsumesCounterOnceAndRecordsOnlyDeliveredCopy() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingHotpRepository()
            val runtime = createTestOtpRuntime(repository, scope = backgroundScope)
            val gate = PausingAuthorizationGate()
            val clipboardValues = mutableListOf<String>()
            val activityRecorder = RecordingActivityRecorder()
            val viewModel =
                createViewModel(
                    gate = gate,
                    clipboardValues = clipboardValues,
                    otpRuntime = runtime,
                    activityRecorder = activityRecorder,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val owner = viewModel.onPageEntered()
            val otpStateCollector = backgroundScope.collectOtpState(viewModel, ENTRY_ID.value, OtpType.HOTP)

            try {
                runCurrent()
                assertTrue(viewModel.otpStates.value[ENTRY_ID.value] is OtpCodeState.HotpReady)

                viewModel.onAction(VaultUiAction.CopyOtp(ENTRY_ID.value))
                gate.started.await()
                gate.resume.complete(Unit)
                runCurrent()

                assertEquals(listOf(0L), repository.consumedCounters)
                assertEquals(1L, repository.currentCounter)
                assertEquals(listOf("123456"), clipboardValues)
                assertEquals(listOf(ENTRY_ID.value), activityRecorder.copiedEntryIds)
                assertEquals(listOf(ActivityType.COPY_OTP_CODE), activityRecorder.recordedTypes)
            } finally {
                gate.resume.complete(Unit)
                viewModel.onPageExited(owner)
                otpStateCollector.cancel()
                runCurrent()
                viewModelStore.clear()
            }
        }

    @Test
    fun vaultHOTPcopyDoesNotDeliverWhenCounterTransactionFails() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingHotpRepository(failConsumption = true)
            val runtime = createTestOtpRuntime(repository, scope = backgroundScope)
            val gate = PausingAuthorizationGate()
            val clipboardValues = mutableListOf<String>()
            val activityRecorder = RecordingActivityRecorder()
            val viewModel =
                createViewModel(
                    gate = gate,
                    clipboardValues = clipboardValues,
                    otpRuntime = runtime,
                    activityRecorder = activityRecorder,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val owner = viewModel.onPageEntered()
            val otpStateCollector = backgroundScope.collectOtpState(viewModel, ENTRY_ID.value, OtpType.HOTP)

            try {
                runCurrent()
                viewModel.onAction(VaultUiAction.CopyOtp(ENTRY_ID.value))
                gate.started.await()
                gate.resume.complete(Unit)
                runCurrent()

                assertEquals(listOf(0L), repository.consumedCounters)
                assertEquals(0L, repository.currentCounter)
                assertTrue(clipboardValues.isEmpty())
                assertTrue(activityRecorder.copiedEntryIds.isEmpty())
                assertTrue(activityRecorder.recordedTypes.isEmpty())
            } finally {
                gate.resume.complete(Unit)
                viewModel.onPageExited(owner)
                otpStateCollector.cancel()
                runCurrent()
                viewModelStore.clear()
            }
        }

    @Test
    fun vaultHOTPcopyDoesNotConsumeOrDeliverWhenAuthorizationIsCancelled() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingHotpRepository()
            val runtime = createTestOtpRuntime(repository, scope = backgroundScope)
            val clipboardValues = mutableListOf<String>()
            val activityRecorder = RecordingActivityRecorder()
            val viewModel =
                createViewModel(
                    gate = CancelledAuthorizationGate,
                    clipboardValues = clipboardValues,
                    otpRuntime = runtime,
                    activityRecorder = activityRecorder,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val owner = viewModel.onPageEntered()
            val otpStateCollector = backgroundScope.collectOtpState(viewModel, ENTRY_ID.value, OtpType.HOTP)

            try {
                runCurrent()
                viewModel.onAction(VaultUiAction.CopyOtp(ENTRY_ID.value))
                runCurrent()

                assertTrue(repository.consumedCounters.isEmpty())
                assertEquals(0L, repository.currentCounter)
                assertTrue(clipboardValues.isEmpty())
                assertTrue(activityRecorder.copiedEntryIds.isEmpty())
                assertTrue(activityRecorder.recordedTypes.isEmpty())
            } finally {
                viewModel.onPageExited(owner)
                otpStateCollector.cancel()
                runCurrent()
                viewModelStore.clear()
            }
        }

    @Test
    fun vaultHOTPcopyDoesNotConsumeOrDeliverAfterPageExitDuringAuthorization() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingHotpRepository()
            val runtime = createTestOtpRuntime(repository, scope = backgroundScope)
            val gate = PausingAuthorizationGate()
            val clipboardValues = mutableListOf<String>()
            val activityRecorder = RecordingActivityRecorder()
            val viewModel =
                createViewModel(
                    gate = gate,
                    clipboardValues = clipboardValues,
                    otpRuntime = runtime,
                    activityRecorder = activityRecorder,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val owner = viewModel.onPageEntered()
            val otpStateCollector = backgroundScope.collectOtpState(viewModel, ENTRY_ID.value, OtpType.HOTP)

            try {
                runCurrent()
                viewModel.onAction(VaultUiAction.CopyOtp(ENTRY_ID.value))
                gate.started.await()
                viewModel.onPageExited(owner)
                gate.resume.complete(Unit)
                runCurrent()

                assertTrue(repository.consumedCounters.isEmpty())
                assertEquals(0L, repository.currentCounter)
                assertTrue(clipboardValues.isEmpty())
                assertTrue(activityRecorder.copiedEntryIds.isEmpty())
                assertTrue(activityRecorder.recordedTypes.isEmpty())
            } finally {
                gate.resume.complete(Unit)
                otpStateCollector.cancel()
                runCurrent()
                viewModelStore.clear()
            }
        }

    @Test
    fun vaultHOTPcopyDoesNotConsumeOrDeliverAfterSessionRevocationDuringAuthorization() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingHotpRepository()
            val runtime = createTestOtpRuntime(repository, scope = backgroundScope)
            val gate = PausingAuthorizationGate()
            val access = TestAccessState()
            val clipboardValues = mutableListOf<String>()
            val activityRecorder = RecordingActivityRecorder()
            val viewModel =
                createViewModel(
                    gate = gate,
                    clipboardValues = clipboardValues,
                    otpRuntime = runtime,
                    activityRecorder = activityRecorder,
                    secureSessionAccessState = access,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val owner = viewModel.onPageEntered()
            val otpStateCollector = backgroundScope.collectOtpState(viewModel, ENTRY_ID.value, OtpType.HOTP)

            try {
                runCurrent()
                viewModel.onAction(VaultUiAction.CopyOtp(ENTRY_ID.value))
                gate.started.await()
                access.authenticationState.value = AuthenticationState.Locked
                gate.resume.complete(Unit)
                runCurrent()

                assertTrue(repository.consumedCounters.isEmpty())
                assertEquals(0L, repository.currentCounter)
                assertTrue(clipboardValues.isEmpty())
                assertTrue(activityRecorder.copiedEntryIds.isEmpty())
                assertTrue(activityRecorder.recordedTypes.isEmpty())
            } finally {
                gate.resume.complete(Unit)
                viewModel.onPageExited(owner)
                otpStateCollector.cancel()
                runCurrent()
                viewModelStore.clear()
            }
        }

    @Test
    fun completedDeleteClearsRetainedOtp() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = PausingAuthorizationGate()
            val deletedEntries = mutableListOf<EntryId>()
            val viewModel =
                createViewModel(
                    gate,
                    mutableListOf(),
                    ReadyOtpConfigRepository,
                    onMoveToTrash = deletedEntries::add,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val owner = viewModel.onPageEntered()
            val otpStateCollector = backgroundScope.collectOtpState(viewModel, ENTRY_ID.value)

            try {
                runCurrent()
                assertTrue(viewModel.otpStates.value[ENTRY_ID.value] is OtpCodeState.TimedReady)

                viewModel.onAction(VaultUiAction.QuickDelete(ENTRY_ID.value))
                gate.started.await()
                gate.resume.complete(Unit)
                runCurrent()

                assertEquals(listOf(ENTRY_ID), deletedEntries)
                assertNull(viewModel.otpStates.value[ENTRY_ID.value])
            } finally {
                gate.resume.complete(Unit)
                viewModel.onPageExited(owner)
                otpStateCollector.cancel()
                runCurrent()
                viewModelStore.clear()
            }
        }

    @Test
    fun otpLeaseLifetimeFollowsEachActiveUiStateCollection() =
        runTest(mainDispatcherRule.testDispatcher) {
            val runtime = RecordingOtpRuntime()
            val viewModel = createViewModel(PausingAuthorizationGate(), mutableListOf(), otpRuntime = runtime)
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }

            val firstOwner = backgroundScope.collectOtpState(viewModel, ENTRY_ID.value)
            val secondOwner = backgroundScope.collectOtpState(viewModel, ENTRY_ID.value)
            val otherEntry = backgroundScope.collectOtpState(viewModel, "entry-2")
            runCurrent()

            assertEquals(listOf(ENTRY_ID.value, ENTRY_ID.value, "entry-2"), runtime.acquired)
            assertEquals(emptyList<String>(), runtime.released)

            firstOwner.cancel()
            runCurrent()
            assertEquals(listOf(ENTRY_ID.value), runtime.released)

            secondOwner.cancel()
            otherEntry.cancel()
            runCurrent()
            assertEquals(listOf(ENTRY_ID.value, ENTRY_ID.value, "entry-2"), runtime.released)
            viewModelStore.clear()
        }

    @Test
    fun otpUiStateFlowProjectsOnlyItsEntryAndUpdatesWithRuntimeState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val runtime = RecordingOtpRuntime()
            runtime.states.value =
                mapOf(
                    ENTRY_ID.value to
                        OtpCodeState.TimedReady(
                            code = "123456",
                            nextCode = "654321",
                            windowEndMillis = 30_000L,
                            periodSeconds = 30,
                        ),
                    "other-entry" to OtpCodeState.Pending,
                )
            val viewModel = createViewModel(PausingAuthorizationGate(), mutableListOf(), otpRuntime = runtime)
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val otpState =
                viewModel.otpUiStateFlow(
                    entryId = ENTRY_ID.value,
                    otpPresentation = OtpPresentation.Applicable(OtpType.TOTP),
                    showCode = true,
                )
            val observedStates = mutableListOf<OtpUiState>()
            val collector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    otpState.collect { state -> observedStates += state }
                }

            assertEquals(
                OtpUiState.TimedReady(
                    currentCode = "123 456",
                    nextCode = "654 321",
                    windowEndMillis = 30_000L,
                    periodSeconds = 30,
                ),
                otpState.value,
            )

            runtime.states.value =
                runtime.states.value + ("other-entry" to OtpCodeState.Failed(OtpGenerationError.InvalidSecret))
            runCurrent()
            assertEquals(1, observedStates.size)

            runtime.states.value = runtime.states.value + (ENTRY_ID.value to OtpCodeState.Pending)
            runCurrent()

            assertEquals(OtpUiState.Pending, otpState.value)
            assertEquals(
                listOf(
                    OtpUiState.TimedReady(
                        currentCode = "123 456",
                        nextCode = "654 321",
                        windowEndMillis = 30_000L,
                        periodSeconds = 30,
                    ),
                    OtpUiState.Pending,
                ),
                observedStates,
            )
            collector.cancel()
            viewModelStore.clear()
        }

    @Test
    fun deletingEntryInvalidatesItsOtpStateWhileTheUiLeaseIsOpen() =
        runTest(mainDispatcherRule.testDispatcher) {
            val runtime = RecordingOtpRuntime()
            val gate = PausingAuthorizationGate()
            val movedEntries = mutableListOf<EntryId>()
            val viewModel =
                createViewModel(
                    gate,
                    mutableListOf(),
                    otpRuntime = runtime,
                    onMoveToTrash = movedEntries::add,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            viewModel.onPageEntered()

            val otpStateCollector = backgroundScope.collectOtpState(viewModel, ENTRY_ID.value)
            runCurrent()
            viewModel.onAction(VaultUiAction.QuickDelete(ENTRY_ID.value))
            gate.started.await()
            gate.resume.complete(Unit)
            runCurrent()

            assertEquals(listOf(ENTRY_ID), movedEntries)
            assertEquals(emptyList<String>(), runtime.released)
            assertEquals(listOf(ENTRY_ID.value), runtime.removed)
            otpStateCollector.cancel()
            runCurrent()
            assertEquals(listOf(ENTRY_ID.value), runtime.released)
            viewModelStore.clear()
            assertEquals(listOf(ENTRY_ID.value), runtime.released)
        }

    @Test
    fun currentPageCanCopyAfterAuthorizationCompletes() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = PausingAuthorizationGate()
            val clipboardValues = mutableListOf<String>()
            val viewModel = createViewModel(gate, clipboardValues)
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            viewModel.onPageEntered()
            val effects = Channel<VaultEffect>(Channel.UNLIMITED)
            val effectCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.effects.collect(effects::send)
                }

            try {
                viewModel.onAction(
                    VaultUiAction.CopyField(
                        entryId = ENTRY_ID.value,
                        entryType = EntryType.ACCOUNT,
                        fieldKey = FieldKey.USERNAME,
                    ),
                )
                gate.started.await()
                gate.resume.complete(Unit)
                runCurrent()

                assertTrue("the active page should still copy", clipboardValues == listOf("alice"))
                assertTrue(effects.tryReceive().getOrNull() is VaultEffect.FieldCopied)
            } finally {
                gate.resume.complete(Unit)
                effectCollector.cancel()
                viewModelStore.clear()
            }
        }

    @Test
    fun sessionRevocationWhileAuthorizationIsPendingSuppressesCopy() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = PausingAuthorizationGate()
            val access = TestAccessState()
            val clipboardValues = mutableListOf<String>()
            val viewModel = createViewModel(gate, clipboardValues, secureSessionAccessState = access)
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            viewModel.onPageEntered()
            val effects = Channel<VaultEffect>(Channel.UNLIMITED)
            val effectCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.effects.collect(effects::send)
                }

            try {
                viewModel.onAction(
                    VaultUiAction.CopyField(
                        entryId = ENTRY_ID.value,
                        entryType = EntryType.ACCOUNT,
                        fieldKey = FieldKey.USERNAME,
                    ),
                )
                gate.started.await()
                access.authenticationState.value = AuthenticationState.Locked
                gate.resume.complete(Unit)
                runCurrent()

                assertTrue("a locked session must not copy", clipboardValues.isEmpty())
                assertNull(effects.tryReceive().getOrNull())
            } finally {
                gate.resume.complete(Unit)
                effectCollector.cancel()
                viewModelStore.clear()
            }
        }

    @Test
    fun settingsSortUpdatesTheViewModelPagingGeneration() =
        runTest(mainDispatcherRule.testDispatcher) {
            val initialSort = EntrySort(EntrySortField.CREATED_AT, SortDirection.DESC)
            val updatedSort = EntrySort(EntrySortField.TITLE, SortDirection.ASC)
            val settings =
                MutableStateFlow(
                    LibraryViewSettings(
                        sort = initialSort,
                        entryCardPresentations = emptyList(),
                        entryHierarchyDisplayMode = EntryHierarchyDisplayMode.COLLAPSED,
                    ),
                )
            val pageQueries = mutableListOf<EntryListQuery>()
            val viewModel =
                createViewModel(
                    PausingAuthorizationGate(),
                    mutableListOf(),
                    librarySettings = settings,
                    onPageQuery = pageQueries::add,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val entriesCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.entries.collect()
                }

            try {
                runCurrent()
                advanceTimeBy(250L)
                runCurrent()

                assertEquals(listOf(initialSort), pageQueries.map(EntryListQuery::sort))

                settings.value =
                    LibraryViewSettings(
                        sort = updatedSort,
                        entryCardPresentations = emptyList(),
                        entryHierarchyDisplayMode = EntryHierarchyDisplayMode.COLLAPSED,
                    )
                runCurrent()
                advanceTimeBy(250L)
                runCurrent()

                assertEquals(listOf(initialSort, updatedSort), pageQueries.map(EntryListQuery::sort))
            } finally {
                entriesCollector.cancel()
                viewModelStore.clear()
            }
        }

    @Test
    fun displaySettingsUseTheSameRecoverableReadAndRetryAsTheVaultQuery() =
        runTest(mainDispatcherRule.testDispatcher) {
            var attempts = 0
            val librarySettings =
                flow {
                    attempts++
                    if (attempts == 1) throw CorruptSettingsDataException(IllegalStateException())
                    emit(sampleLibrarySettings())
                    kotlinx.coroutines.awaitCancellation()
                }
            val viewModel =
                createViewModel(
                    PausingAuthorizationGate(),
                    mutableListOf(),
                    librarySettings = librarySettings,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val displayCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.displayState.collect()
                }

            try {
                runCurrent()
                assertEquals(VaultDisplayUiState.Failed, viewModel.displayState.value)
                viewModel.retrySettings()
                runCurrent()

                assertEquals(2, attempts)
                assertEquals(
                    VaultDisplayUiState.Ready(
                        hideSystemBars = false,
                        collapseTopBarOnScroll = true,
                        collapseQuickFilterBarOnScroll = true,
                        entryCardPresentations = emptyList(),
                        isSwipeEnabled = true,
                        isPullToSearchEnabled = false,
                        swipeLeftAction = SwipeActionType.DELETE,
                        swipeRightAction = SwipeActionType.COPY_PASSWORD,
                        sort = EntrySort(EntrySortField.CREATED_AT, SortDirection.DESC),
                    ),
                    viewModel.displayState.value,
                )
            } finally {
                displayCollector.cancel()
                viewModelStore.clear()
            }
        }

    @Test
    fun displaySettingsStayLoadingUntilEveryPersistedSourceEmits() =
        runTest(mainDispatcherRule.testDispatcher) {
            val interfaceSettings = MutableSharedFlow<InterfaceSettings>()
            val librarySettings = MutableSharedFlow<LibraryViewSettings>()
            val interactionSettings = MutableSharedFlow<InteractionSettings>()
            val viewModel =
                createViewModel(
                    PausingAuthorizationGate(),
                    mutableListOf(),
                    librarySettings = librarySettings,
                    interfaceSettings = interfaceSettings,
                    interactionSettings = interactionSettings,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val displayCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.displayState.collect()
                }

            try {
                runCurrent()
                assertEquals(VaultDisplayUiState.Loading, viewModel.displayState.value)

                interfaceSettings.emit(sampleInterfaceSettings())
                librarySettings.emit(sampleLibrarySettings())
                runCurrent()
                assertEquals(VaultDisplayUiState.Loading, viewModel.displayState.value)

                interactionSettings.emit(sampleInteractionSettings())
                runCurrent()
                assertEquals(
                    VaultDisplayUiState.Ready(
                        hideSystemBars = false,
                        collapseTopBarOnScroll = true,
                        collapseQuickFilterBarOnScroll = true,
                        entryCardPresentations = emptyList(),
                        isSwipeEnabled = true,
                        isPullToSearchEnabled = false,
                        swipeLeftAction = SwipeActionType.DELETE,
                        swipeRightAction = SwipeActionType.COPY_PASSWORD,
                        sort = EntrySort(EntrySortField.CREATED_AT, SortDirection.DESC),
                    ),
                    viewModel.displayState.value,
                )
            } finally {
                displayCollector.cancel()
                viewModelStore.clear()
            }
        }

    @Test
    fun hierarchyModeChangesDoNotAlterTheDisplayProjection() =
        runTest(mainDispatcherRule.testDispatcher) {
            val settings = MutableStateFlow(sampleLibrarySettings())
            val viewModel = createViewModel(PausingAuthorizationGate(), mutableListOf(), librarySettings = settings)
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val displayCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.displayState.collect {}
                }

            try {
                runCurrent()
                val initial = viewModel.displayState.value
                assertTrue(initial is VaultDisplayUiState.Ready)

                settings.value = settings.value.copy(entryHierarchyDisplayMode = EntryHierarchyDisplayMode.EXPANDED)
                runCurrent()

                assertEquals(initial, viewModel.displayState.value)
            } finally {
                displayCollector.cancel()
                viewModelStore.clear()
            }
        }

    @Test
    fun sortPreferenceFailureShowsCurrentPageFeedbackWithoutChangingSelectedSort() =
        runTest(mainDispatcherRule.testDispatcher) {
            val currentSort = EntrySort(EntrySortField.CREATED_AT, SortDirection.DESC)
            val settings =
                MutableStateFlow(
                    LibraryViewSettings(
                        sort = currentSort,
                        entryCardPresentations = emptyList(),
                        entryHierarchyDisplayMode = EntryHierarchyDisplayMode.COLLAPSED,
                    ),
                )
            val viewModel =
                createViewModel(
                    PausingAuthorizationGate(),
                    mutableListOf(),
                    librarySettings = settings,
                    setSort = { throw IllegalStateException("settings write failed") },
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val owner = viewModel.onPageEntered()
            val effects = Channel<VaultEffect>(Channel.UNLIMITED)
            val settingsCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.displayState.collect()
                }
            val effectCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.effects.collect { effects.send(it) }
                }

            try {
                runCurrent()
                viewModel.onAction(VaultUiAction.SortOptionSelected(VaultSortOption.TITLE))
                runCurrent()

                val effect = effects.receive() as VaultEffect.ShowError
                assertEquals(R.string.vault_error_sort_failed, effect.message.resourceId)
                assertEquals(currentSort, settings.value.sort)
                assertNull(effects.tryReceive().getOrNull())
            } finally {
                viewModel.onPageExited(owner)
                settingsCollector.cancel()
                effectCollector.cancel()
                viewModelStore.clear()
            }
        }

    @Test
    fun searchAndTypeFilterTogetherReachThePagingSource() =
        runTest(mainDispatcherRule.testDispatcher) {
            val pageQueries = mutableListOf<EntryListQuery>()
            val viewModel =
                createViewModel(
                    PausingAuthorizationGate(),
                    mutableListOf(),
                    onPageQuery = pageQueries::add,
                )
            val viewModelStore = ViewModelStore().apply { put("vault", viewModel) }
            val entriesCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.entries.collect()
                }

            try {
                runCurrent()
                advanceTimeBy(250L)
                runCurrent()

                viewModel.onAction(VaultUiAction.SearchToggled(active = true))
                viewModel.onAction(VaultUiAction.SearchQueryChanged("  mail  "))
                viewModel.onAction(VaultUiAction.FilterToggled(AddType.PASSWORD))
                assertEquals(VaultSearchInput.Open("  mail  "), viewModel.uiState.value.search)
                runCurrent()
                advanceTimeBy(250L)
                runCurrent()

                val currentQuery = pageQueries.last()
                assertEquals("mail", currentQuery.searchText)
                assertEquals(AddType.PASSWORD.entryTypes.toSet(), currentQuery.entryTypes)
                assertEquals(
                    EntrySort(EntrySortField.CREATED_AT, SortDirection.DESC),
                    currentQuery.sort,
                )

                viewModel.onAction(VaultUiAction.ClearFilters)
                assertEquals(emptySet<AddType>(), viewModel.uiState.value.selectedFilters)
            } finally {
                entriesCollector.cancel()
                viewModelStore.clear()
            }
        }

    private fun CoroutineScope.collectOtpState(
        viewModel: VaultViewModel,
        entryId: String,
        otpType: OtpType = OtpType.TOTP,
    ) = launch {
        viewModel
            .otpUiStateFlow(
                entryId = entryId,
                otpPresentation = OtpPresentation.Applicable(otpType),
                showCode = true,
            ).collect()
    }

    private fun createViewModel(
        gate: AuthorizationGate,
        clipboardValues: MutableList<String>,
        otpConfigRepository: OtpConfigRepository = EmptyOtpConfigRepository,
        onMoveToTrash: (EntryId) -> Unit = {},
        availableCategories: Flow<List<String>> = flowOf(emptyList()),
        librarySettings: Flow<LibraryViewSettings> = flowOf(sampleLibrarySettings()),
        interfaceSettings: Flow<InterfaceSettings> = flowOf(sampleInterfaceSettings()),
        interactionSettings: Flow<InteractionSettings> = flowOf(sampleInteractionSettings()),
        otpRuntime: OtpCodeRuntimePort? = null,
        activityRecorder: ActivityRecorder = TestActivityRecorder,
        onPageQuery: (EntryListQuery) -> Unit = {},
        setSort: suspend (EntrySort) -> Unit = {},
        moveToTrashResult: AppResult<Unit> = AppResult.Success(Unit),
        secureSessionAccessState: TestAccessState = TestAccessState(),
    ): VaultViewModel {
        val access = secureSessionAccessState
        val entries = TestEntries(onMoveToTrash, moveToTrashResult)
        val clipboard = SensitiveClipboardWriter { clipboardValues += it }
        val activity = activityRecorder
        val sensitiveFields = EmptySensitiveFields
        val settings = TestSettingsRepository(librarySettings, setSort)
        val otpRuntime = otpRuntime ?: createTestOtpRuntime(otpConfigRepository)
        return VaultViewModel(
            entryListQueryRepository = TestEntryListQueryRepository(availableCategories),
            entryPagingRepository =
                object : EntryPagingRepository {
                    override fun pages(
                        query: EntryListQuery,
                        config: PagingConfig,
                    ): Flow<PagingData<EntryListItem>> {
                        onPageQuery(query)
                        return flowOf(PagingData.empty())
                    }
                },
            settingsRepository = settings,
            interfaceSettingsRepository = TestInterfaceSettingsRepository(interfaceSettings),
            interactionSettingsSource = TestInteractionSettingsSource(interactionSettings),
            entryCommandWorkflowFactory =
                VaultEntryCommandWorkflowFactory(
                    secureSessionAccessState = access,
                    trashCommand = TrashCommandUseCase(entries, entries, access, gate),
                    copyEntryField =
                        CopyEntryFieldUseCase(
                            authorizationGate = gate,
                            entryQueryRepository = entries,
                            sensitiveFieldRepository = sensitiveFields,
                            clipboardWriter = clipboard,
                            activityRecorder = activity,
                        ),
                    copyOtpCode = CopyOtpCodeUseCase(gate, clipboard, activity, otpRuntime),
                ),
            otpRuntime = otpRuntime,
        )
    }

    private class TestInterfaceSettingsRepository(
        override val interfaceSettings: Flow<InterfaceSettings>,
    ) : InterfaceSettingsRepository {
        override suspend fun setHideSystemBars(enabled: Boolean) = Unit

        override suspend fun setTopBarCollapsible(enabled: Boolean) = Unit

        override suspend fun setQuickFilterBarCollapsible(enabled: Boolean) = Unit

        override suspend fun setAppCornerRadius(radiusDp: Float) = Unit
    }

    private class TestInteractionSettingsSource(
        override val interaction: Flow<InteractionSettings>,
    ) : InteractionSettingsSource

    private class RecordingOtpRuntime : OtpCodeRuntimePort {
        override val states = MutableStateFlow<Map<String, OtpCodeState>>(emptyMap())
        val acquired = mutableListOf<String>()
        val released = mutableListOf<String>()
        val removed = mutableListOf<String>()

        override fun acquire(entryId: String): OtpLease {
            acquired += entryId
            return OtpLease { released += entryId }
        }

        override suspend fun codeForCopy(entryId: String): String? = null

        override suspend fun rollbackLastHotpCounter(entryId: String): Boolean = false

        override fun entryRemoved(entryId: String) {
            removed += entryId
            states.value -= entryId
        }

        override fun entryChanged(entryId: String) = Unit
    }

    private fun sampleInterfaceSettings() =
        InterfaceSettings(
            hideSystemBars = false,
            collapseTopBarOnScroll = true,
            collapseQuickFilterBarOnScroll = true,
            appCornerRadiusDp = 16f,
        )

    private fun sampleInteractionSettings() =
        InteractionSettings(
            isSwipeEnabled = true,
            isPullToSearchEnabled = false,
            swipeLeftAction = SwipeActionType.DELETE,
            swipeRightAction = SwipeActionType.COPY_PASSWORD,
        )

    private class PausingAuthorizationGate : AuthorizationGate {
        val started = CompletableDeferred<Unit>()
        val resume = CompletableDeferred<Unit>()

        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> {
            started.complete(Unit)
            resume.await()
            return AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
        }
    }

    private object CancelledAuthorizationGate : AuthorizationGate {
        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> = AuthorizationResult.Cancelled
    }

    private class TestAccessState : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))

        override fun isUnlocked() = true
    }

    private class TestEntries(
        private val onMoveToTrash: (EntryId) -> Unit,
        private val moveToTrashResult: AppResult<Unit>,
    ) : EntryQueryRepository,
        EntryCommandRepository {
        override suspend fun getById(entryId: EntryId): Entry? = entry.takeIf { it.id == entryId }

        override suspend fun getByIds(entryIds: List<EntryId>): List<Entry> = entry.takeIf { it.id in entryIds }?.let(::listOf).orEmpty()

        override suspend fun getTypesByIds(entryIds: List<EntryId>) =
            entry.takeIf { it.id in entryIds }?.let { mapOf(it.id to it.type) }.orEmpty()

        override suspend fun createEntry(entry: Entry): AppResult<EntryId> = AppResult.Success(entry.id)

        override suspend fun updateEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
            changes: EntryUpdate,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun moveToTrash(
            id: EntryId,
            expectedVersion: EntryVersion,
        ): AppResult<Unit> {
            onMoveToTrash(id)
            return moveToTrashResult
        }

        override suspend fun restoreEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun deletePermanently(
            id: EntryId,
            expectedVersion: EntryVersion,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun emptyTrash(): AppResult<Int> = AppResult.Success(0)

        private companion object {
            val entry =
                Entry(
                    identity = EntryIdentity(ENTRY_ID, EntryType.ACCOUNT, timestamps = EntryTimestamps(1L)),
                    profile = EntryProfile(title = "Example", username = "alice"),
                )
        }
    }

    private class TestEntryListQueryRepository(
        override val availableCategories: Flow<List<String>>,
    ) : EntryListQueryRepository {
        override val deletedEntries: Flow<List<EntryListItem>> = flowOf(emptyList())

        override suspend fun getActiveByType(entryType: EntryType): List<EntryListItem> = emptyList()
    }

    private class TestSettingsRepository(
        override val libraryViewSettings: Flow<LibraryViewSettings>,
        private val setSort: suspend (EntrySort) -> Unit,
    ) : LibraryViewSettingsRepository {
        override suspend fun setSort(sort: EntrySort) = setSort.invoke(sort)

        override suspend fun setEntryHierarchyDisplayMode(mode: EntryHierarchyDisplayMode) = Unit

        override suspend fun setEntryCardPresentation(presentation: com.aozijx.passly.domain.settings.model.EntryCardPresentation) = Unit
    }

    private object EmptySensitiveFields : SensitiveFieldRepository {
        override suspend fun getPresence(entryId: EntryId) = SensitiveFieldPresence(entryId, emptySet())

        override suspend fun reveal(
            entryId: EntryId,
            key: SensitiveFieldKey,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): RevealedSensitiveField? = null

        override suspend fun revealMany(
            entryId: EntryId,
            keys: Set<SensitiveFieldKey>,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): List<RevealedSensitiveField> = emptyList()

        override suspend fun readBundle(entryId: EntryId) = EntrySecret()
    }

    private object TestActivityRecorder : ActivityRecorder {
        override suspend fun recordUsage(
            entryId: String,
            type: ActivityType,
            affectedFields: Set<FieldKey>,
        ) = AppResult.Success(Unit)

        override suspend fun deleteByEntryId(entryId: String) = Unit

        override suspend fun deleteBefore(timestamp: Long) = Unit
    }

    private class RecordingActivityRecorder : ActivityRecorder {
        val copiedEntryIds = mutableListOf<String>()
        val recordedTypes = mutableListOf<ActivityType>()

        override suspend fun recordUsage(
            entryId: String,
            type: ActivityType,
            affectedFields: Set<FieldKey>,
        ): AppResult<Unit> {
            copiedEntryIds += entryId
            recordedTypes += type
            return AppResult.Success(Unit)
        }

        override suspend fun deleteByEntryId(entryId: String) = Unit

        override suspend fun deleteBefore(timestamp: Long) = Unit
    }

    private object EmptyOtpConfigRepository : OtpConfigRepository {
        override suspend fun getGenerationMaterial(entryId: String): OtpGenerationMaterial? = null

        override suspend fun consumeHotpCode(
            entryId: String,
            expectedCounter: Long,
        ): OtpResult = OtpResult.Failure(OtpGenerationError.InvalidSecret)

        override suspend fun rollbackHotpCounter(
            entryId: String,
            expectedCurrentCounter: Long,
        ): Boolean = false
    }

    private object ReadyOtpConfigRepository : OtpConfigRepository {
        override suspend fun getGenerationMaterial(entryId: String) = generationMaterialForTest(OtpConfig.Totp(secret = "JBSWY3DPEHPK3PXP"))

        override suspend fun consumeHotpCode(
            entryId: String,
            expectedCounter: Long,
        ): OtpResult = OtpResult.Failure(OtpGenerationError.InvalidSecret)

        override suspend fun rollbackHotpCounter(
            entryId: String,
            expectedCurrentCounter: Long,
        ): Boolean = false
    }

    private class RecordingHotpRepository(
        private val failConsumption: Boolean = false,
    ) : OtpConfigRepository {
        var currentCounter = 0L
            private set
        val consumedCounters = mutableListOf<Long>()

        override suspend fun getGenerationMaterial(entryId: String) =
            generationMaterialForTest(OtpConfig.Hotp(secret = "JBSWY3DPEHPK3PXP", counter = currentCounter))

        override suspend fun consumeHotpCode(
            entryId: String,
            expectedCounter: Long,
        ): OtpResult {
            consumedCounters += expectedCounter
            if (expectedCounter != currentCounter) return OtpResult.Failure(OtpGenerationError.CounterChanged)
            if (failConsumption) return OtpResult.Failure(OtpGenerationError.InvalidSecret)
            val nextCounter = currentCounter + 1
            currentCounter = nextCounter
            return OtpResult.CounterSuccess(code = "123456", nextCounter = nextCounter)
        }

        override suspend fun rollbackHotpCounter(
            entryId: String,
            expectedCurrentCounter: Long,
        ): Boolean = false
    }

    private companion object {
        val ENTRY_ID = EntryId("entry-1")

        fun sampleLibrarySettings() =
            LibraryViewSettings(
                sort = EntrySort(EntrySortField.CREATED_AT, SortDirection.DESC),
                entryCardPresentations = emptyList(),
                entryHierarchyDisplayMode = EntryHierarchyDisplayMode.COLLAPSED,
            )
    }
}
