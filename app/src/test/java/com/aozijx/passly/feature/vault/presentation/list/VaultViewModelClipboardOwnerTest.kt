package com.aozijx.passly.feature.vault.presentation.list

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SensitiveKeyFreshnessState
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
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.otp.OtpResult
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.entry.policy.DefaultEntryFieldReader
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldUseCase
import com.aozijx.passly.feature.vault.entry.CopyOtpCodeUseCase
import com.aozijx.passly.feature.vault.entry.VaultEntryPageSource
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimeFactory
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.trash.TrashCommandUseCase
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionStateProvider
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModelClipboardOwnerTest {
    @get:Rule
    val mainDispatcherRule = com.aozijx.passly.testing.MainDispatcherRule()

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
    fun categoryQueryDistinguishesEmptyResultsAndRecoversAfterSessionReadabilityReturns() =
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
                assertEquals(VaultCategoriesState.Ready(emptyList()), viewModel.uiState.value.categories)

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
            val otpStateCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.totpStatesFlow.collect()
                }

            try {
                viewModel.subscribeVisibleOtp(ENTRY_ID.value)
                runCurrent()
                assertTrue(viewModel.totpStatesFlow.value[ENTRY_ID.value] is OtpCodeState.Ready)

                viewModel.onAction(VaultUiAction.CopyOtp(ENTRY_ID.value))
                gate.started.await()
                viewModel.onPageExited(owner)
                gate.resume.complete(Unit)
                runCurrent()

                assertTrue("a departed page must not write its pending OTP", clipboardValues.isEmpty())
            } finally {
                gate.resume.complete(Unit)
                otpStateCollector.cancel()
                viewModelStore.clear()
            }
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

    private fun createViewModel(
        gate: AuthorizationGate,
        clipboardValues: MutableList<String>,
        otpConfigRepository: OtpConfigRepository = EmptyOtpConfigRepository,
        onMoveToTrash: (EntryId) -> Unit = {},
        availableCategories: Flow<List<String>> = flowOf(emptyList()),
    ): VaultViewModel {
        val access = TestAccessState()
        val entries = TestEntries(onMoveToTrash)
        val clipboard = SensitiveClipboardWriter { clipboardValues += it }
        val activity = TestActivityRecorder
        val sensitiveFields = EmptySensitiveFields
        val settings = TestSettingsRepository()
        val otpFactory =
            OtpCodeRuntimeFactory(
                otpConfigRepository = otpConfigRepository,
                sessionStateProvider = TestSessionStateProvider,
                sensitiveKeyFreshnessState = TestSensitiveKeyFreshnessState,
            )
        return VaultViewModel(
                entryListQueryRepository = TestEntryListQueryRepository(availableCategories),
            entryPageSource =
                object : VaultEntryPageSource {
                    override fun pages(
                        query: EntryListQuery,
                        config: PagingConfig,
                    ) = flowOf(PagingData.empty<EntryListItem>())
                },
            settingsRepository = settings,
            entryCommandWorkflowFactory =
                VaultEntryCommandWorkflowFactory(
                    secureSessionAccessState = access,
                    trashCommand = TrashCommandUseCase(entries, entries, access, gate),
                    copyEntryField =
                        CopyEntryFieldUseCase(
                            authorizationGate = gate,
                            entryQueryRepository = entries,
                            entryFieldReader = DefaultEntryFieldReader(),
                            sensitiveFieldRepository = sensitiveFields,
                            clipboardWriter = clipboard,
                            activityRecorder = activity,
                        ),
                    copyOtpCode = CopyOtpCodeUseCase(gate, clipboard, activity),
                ),
            otpCodeRuntimeFactory = otpFactory,
        )
    }

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

    private class TestAccessState : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))

        override fun isUnlocked() = true
    }

    private class TestEntries(
        private val onMoveToTrash: (EntryId) -> Unit,
    ) : EntryQueryRepository, EntryCommandRepository {
        override suspend fun getById(entryId: EntryId): Entry? = entry.takeIf { it.id == entryId }

        override suspend fun getByIds(entryIds: List<EntryId>): List<Entry> = entry.takeIf { it.id in entryIds }?.let(::listOf).orEmpty()

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
            return AppResult.Success(Unit)
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

    private class TestSettingsRepository : LibraryViewSettingsRepository {
        override val libraryViewSettings =
            flowOf(
                LibraryViewSettings(
                    sort = EntrySort(EntrySortField.CREATED_AT, SortDirection.DESC),
                ),
            )

        override suspend fun setSort(sort: EntrySort) = Unit

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
        ) = AppResult.Success(Unit)

        override suspend fun deleteByEntryId(entryId: String) = Unit

        override suspend fun deleteBefore(timestamp: Long) = Unit
    }

    private object EmptyOtpConfigRepository : OtpConfigRepository {
        override suspend fun getConfig(entryId: String): OtpConfig? = null

        override suspend fun consumeHotpCode(entryId: String): OtpResult = OtpResult.Failure(OtpGenerationError.InvalidSecret)
    }

    private object ReadyOtpConfigRepository : OtpConfigRepository {
        override suspend fun getConfig(entryId: String) = OtpConfig(secret = "JBSWY3DPEHPK3PXP")

        override suspend fun consumeHotpCode(entryId: String): OtpResult = OtpResult.Failure(OtpGenerationError.InvalidSecret)
    }

    private object TestSessionStateProvider : SessionStateProvider {
        override val lockState = SecureSessionState.UNLOCKED
        override val lockStateFlow = MutableStateFlow(SecureSessionState.UNLOCKED)
        override val lockGenerationFlow = MutableStateFlow(0L)
    }

    private object TestSensitiveKeyFreshnessState : SensitiveKeyFreshnessState {
        override val generation: StateFlow<Long> = MutableStateFlow(0L)
    }

    private companion object {
        val ENTRY_ID = EntryId("entry-1")
    }
}
