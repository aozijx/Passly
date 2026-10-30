package com.aozijx.passly.feature.settings.presentation.main.data

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import com.aozijx.passly.domain.settings.port.DiagnosticsLogSettingsRepository
import com.aozijx.passly.core.observability.sink.LogClearResult
import com.aozijx.passly.feature.settings.data.AppCacheStore
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorageSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `changing recording mode persists from data management settings`() = runTest {
        val settings = RecordingLogSettingsRepository()
        val viewModel = StorageSettingsViewModel(
            cacheStore = RecordingCacheStore(sizeBytes = 0L),
            diagnosticsLogStore = RecordingLogStore(sizeBytes = 0L),
            diagnosticsLogSettings = settings,
            secureSessionAccessState = FullAccess,
        )
        viewModel.onAction(StorageSettingsAction.PageEntered)
        runCurrent()

        viewModel.onAction(StorageSettingsAction.SetLogRecordingMode(DiagnosticsLogRecordingMode.DEBUG_AND_ABOVE))
        runCurrent()

        assertEquals(DiagnosticsLogRecordingMode.DEBUG_AND_ABOVE, settings.savedMode)
        assertEquals(DiagnosticsLogRecordingMode.DEBUG_AND_ABOVE, viewModel.uiState.value.logRecordingMode)
    }

    @Test
    fun `recording mode save failure emits user feedback`() = runTest {
        val viewModel = StorageSettingsViewModel(
            cacheStore = RecordingCacheStore(sizeBytes = 0L),
            diagnosticsLogStore = RecordingLogStore(sizeBytes = 0L),
            diagnosticsLogSettings = RecordingLogSettingsRepository(saveFailure = IOException()),
            secureSessionAccessState = FullAccess,
        )
        viewModel.onAction(StorageSettingsAction.PageEntered)
        runCurrent()
        val effect = async { viewModel.effects.first() }

        viewModel.onAction(StorageSettingsAction.SetLogRecordingMode(DiagnosticsLogRecordingMode.DISABLED))
        runCurrent()

        assertEquals(StorageSettingsEffect.LogRecordingSettingsSaveFailed, effect.await())
    }

    @Test
    fun `opening data management loads cache and log sizes`() = runTest {
        val viewModel = StorageSettingsViewModel(
            cacheStore = RecordingCacheStore(sizeBytes = 2_048L),
            diagnosticsLogStore = RecordingLogStore(sizeBytes = 1_024L),
            diagnosticsLogSettings = RecordingLogSettingsRepository(),
            secureSessionAccessState = FullAccess,
        )
        viewModel.onAction(StorageSettingsAction.PageEntered)
        runCurrent()

        assertEquals(StorageSizeUiState.Available("2.0 KB"), viewModel.uiState.value.cache)
        assertEquals(StorageSizeUiState.Available("1.0 KB"), viewModel.uiState.value.logs)
    }

    @Test
    fun `clearing logs refreshes displayed size`() = runTest {
        val store = RecordingLogStore(sizeBytes = 1_024L)
        val viewModel = StorageSettingsViewModel(
            cacheStore = RecordingCacheStore(sizeBytes = 0L),
            diagnosticsLogStore = store,
            diagnosticsLogSettings = RecordingLogSettingsRepository(),
            secureSessionAccessState = FullAccess,
        )
        viewModel.onAction(StorageSettingsAction.PageEntered)
        runCurrent()
        val effect = async { viewModel.effects.first() }

        viewModel.onAction(StorageSettingsAction.ClearLogs)
        runCurrent()

        assertEquals(StorageSettingsEffect.LogsCleared, effect.await())
        assertEquals(1, store.clearCount)
        assertEquals(StorageSizeUiState.Available("0 B"), viewModel.uiState.value.logs)
    }

    @Test
    fun `restricted session cannot clear logs`() = runTest {
        val store = RecordingLogStore(sizeBytes = 1_024L)
        val viewModel = StorageSettingsViewModel(
            cacheStore = RecordingCacheStore(sizeBytes = 0L),
            diagnosticsLogStore = store,
            diagnosticsLogSettings = RecordingLogSettingsRepository(),
            secureSessionAccessState = NoAccess,
        )
        viewModel.onAction(StorageSettingsAction.PageEntered)
        runCurrent()
        val effect = async { viewModel.effects.first() }

        viewModel.onAction(StorageSettingsAction.ClearLogs)
        runCurrent()

        assertEquals(StorageSettingsEffect.LogClearFailed, effect.await())
        assertEquals(0, store.clearCount)
        assertEquals(StorageSizeUiState.Available("1.0 KB"), viewModel.uiState.value.logs)
    }

    @Test
    fun `failed log clear reports failure and does not show stale size as available`() = runTest {
        val store = RecordingLogStore(sizeBytes = 1_024L, clearResult = LogClearResult.Failed)
        val viewModel = StorageSettingsViewModel(
            cacheStore = RecordingCacheStore(sizeBytes = 0L),
            diagnosticsLogStore = store,
            diagnosticsLogSettings = RecordingLogSettingsRepository(),
            secureSessionAccessState = FullAccess,
        )
        viewModel.onAction(StorageSettingsAction.PageEntered)
        runCurrent()
        val effect = async { viewModel.effects.first() }

        viewModel.onAction(StorageSettingsAction.ClearLogs)
        runCurrent()

        assertEquals(StorageSettingsEffect.LogClearFailed, effect.await())
        assertEquals(1, store.clearCount)
        assertEquals(StorageSizeUiState.Unavailable, viewModel.uiState.value.logs)
    }

    @Test
    fun `reentry refreshes logs after clear without replaying old feedback`() = runTest {
        val store = DeferredClearLogStore()
        val viewModel = StorageSettingsViewModel(
            cacheStore = RecordingCacheStore(sizeBytes = 0L),
            diagnosticsLogStore = store,
            diagnosticsLogSettings = RecordingLogSettingsRepository(),
            secureSessionAccessState = FullAccess,
        )
        viewModel.onAction(StorageSettingsAction.PageEntered)
        runCurrent()

        viewModel.onAction(StorageSettingsAction.ClearLogs)
        store.clearStarted.await()
        viewModel.onAction(StorageSettingsAction.PageExited)
        viewModel.onAction(StorageSettingsAction.PageEntered)
        store.clearResult.complete(LogClearResult.Cleared)
        runCurrent()

        assertEquals(StorageSizeUiState.Available("0 B"), viewModel.uiState.value.logs)
        assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
    }

    private class RecordingCacheStore(private var sizeBytes: Long) : AppCacheStore {
        override suspend fun sizeBytes(): Long = sizeBytes
        override suspend fun clear() {
            sizeBytes = 0L
        }
    }

    private class RecordingLogStore(
        private var sizeBytes: Long,
        private val clearResult: LogClearResult = LogClearResult.Cleared,
    ) : DiagnosticsLogStore {
        var clearCount = 0

        override suspend fun readLines(limit: Int): List<DiagnosticsLogLine> = emptyList()
        override suspend fun storageBytes(): Long = sizeBytes
        override suspend fun clear(): LogClearResult {
            clearCount++
            if (clearResult == LogClearResult.Cleared) sizeBytes = 0L
            return clearResult
        }
    }

    private class DeferredClearLogStore : DiagnosticsLogStore {
        val clearStarted = CompletableDeferred<Unit>()
        val clearResult = CompletableDeferred<LogClearResult>()

        override suspend fun readLines(limit: Int): List<DiagnosticsLogLine> = emptyList()

        override suspend fun storageBytes(): Long = 0

        override suspend fun clear(): LogClearResult {
            clearStarted.complete(Unit)
            return clearResult.await()
        }
    }

    private class RecordingLogSettingsRepository(
        private val saveFailure: Exception? = null,
    ) : DiagnosticsLogSettingsRepository {
        private val mode = MutableStateFlow(DiagnosticsLogRecordingMode.INFO_AND_ABOVE)
        var savedMode: DiagnosticsLogRecordingMode? = null

        override val diagnosticsLogRecordingMode: Flow<DiagnosticsLogRecordingMode> = mode

        override suspend fun setDiagnosticsLogRecordingMode(mode: DiagnosticsLogRecordingMode) {
            saveFailure?.let { throw it }
            savedMode = mode
            this.mode.value = mode
        }
    }

    private object FullAccess : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))

        override fun isUnlocked() = true
    }

    private object NoAccess : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Locked)

        override fun isUnlocked() = false
    }
}
