package com.aozijx.passly.feature.settings.presentation.main.data

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.settings.data.AppCacheStore
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorageSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `opening data management loads cache and log sizes`() = runTest {
        val viewModel = StorageSettingsViewModel(
            cacheStore = RecordingCacheStore(sizeBytes = 2_048L),
            diagnosticsLogStore = RecordingLogStore(sizeBytes = 1_024L),
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

    private class RecordingCacheStore(private var sizeBytes: Long) : AppCacheStore {
        override suspend fun sizeBytes(): Long = sizeBytes
        override suspend fun clear() {
            sizeBytes = 0L
        }
    }

    private class RecordingLogStore(private var sizeBytes: Long) : DiagnosticsLogStore {
        var clearCount = 0

        override suspend fun readLines(limit: Int): List<DiagnosticsLogLine> = emptyList()
        override suspend fun storageBytes(): Long = sizeBytes
        override suspend fun clear() {
            clearCount++
            sizeBytes = 0L
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
