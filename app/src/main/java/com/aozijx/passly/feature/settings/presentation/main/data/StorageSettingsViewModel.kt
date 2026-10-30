package com.aozijx.passly.feature.settings.presentation.main.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.platform.cache.ByteSizeFormatter
import com.aozijx.passly.core.observability.sink.LogClearResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import com.aozijx.passly.domain.settings.port.DiagnosticsLogSettingsRepository
import com.aozijx.passly.feature.settings.data.AppCacheStore
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageSettingsViewModel @Inject constructor(
    private val cacheStore: AppCacheStore,
    private val diagnosticsLogStore: DiagnosticsLogStore,
    private val diagnosticsLogSettings: DiagnosticsLogSettingsRepository,
    private val secureSessionAccessState: SecureSessionAccessState,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageSettingsUiState())
    val uiState: StateFlow<StorageSettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<StorageSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Each storage area serializes its own refresh and clear operations. */
    private var cacheOperationJob: Job? = null
    private var logOperationJob: Job? = null

    init {
        viewModelScope.launch {
            diagnosticsLogSettings.diagnosticsLogRecordingMode.collect { mode ->
                _uiState.update { it.copy(logRecordingMode = mode) }
            }
        }
    }

    fun onAction(action: StorageSettingsAction) {
        when (action) {
            StorageSettingsAction.PageEntered -> refreshStorageUsage()
            StorageSettingsAction.ClearCache -> clearCache()
            StorageSettingsAction.ClearLogs -> clearLogs()
            is StorageSettingsAction.SetLogRecordingMode -> setLogRecordingMode(action.mode)
        }
    }

    private fun setLogRecordingMode(mode: DiagnosticsLogRecordingMode) {
        viewModelScope.launch {
            try {
                diagnosticsLogSettings.setDiagnosticsLogRecordingMode(mode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _effects.send(StorageSettingsEffect.LogRecordingSettingsSaveFailed)
            }
        }
    }

    private fun clearLogs() {
        logOperationJob?.cancel()
        logOperationJob = viewModelScope.launch {
            if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
                _effects.send(StorageSettingsEffect.LogClearFailed)
                return@launch
            }
            _uiState.update { it.copy(logs = StorageSizeUiState.Loading) }
            try {
                if (diagnosticsLogStore.clear() != LogClearResult.Cleared) {
                    reportLogClearFailed()
                    return@launch
                }
                _uiState.update { it.copy(logs = readLogSize()) }
                _effects.send(StorageSettingsEffect.LogsCleared)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(logs = StorageSizeUiState.Unavailable) }
                _effects.send(StorageSettingsEffect.LogClearFailed)
            }
        }
    }

    private suspend fun reportLogClearFailed() {
        _uiState.update { it.copy(logs = StorageSizeUiState.Unavailable) }
        _effects.send(StorageSettingsEffect.LogClearFailed)
    }

    private fun refreshStorageUsage() {
        cacheOperationJob?.cancel()
        logOperationJob?.cancel()
        cacheOperationJob = viewModelScope.launch {
            _uiState.update { it.copy(cache = StorageSizeUiState.Loading) }
            _uiState.update { it.copy(cache = readCacheSize()) }
        }
        logOperationJob = viewModelScope.launch {
            _uiState.update { it.copy(logs = StorageSizeUiState.Loading) }
            _uiState.update { it.copy(logs = readLogSize()) }
        }
    }

    private fun clearCache() {
        cacheOperationJob?.cancel()
        cacheOperationJob = viewModelScope.launch {
            _uiState.update { it.copy(cache = StorageSizeUiState.Loading) }
            try {
                cacheStore.clear()
                _uiState.update { it.copy(cache = readCacheSize()) }
                _effects.trySend(StorageSettingsEffect.CacheCleared)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(cache = StorageSizeUiState.Unavailable) }
            }
        }
    }

    private suspend fun readCacheSize(): StorageSizeUiState = try {
        StorageSizeUiState.Available(ByteSizeFormatter.format(cacheStore.sizeBytes()))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        StorageSizeUiState.Unavailable
    }

    private suspend fun readLogSize(): StorageSizeUiState = try {
        StorageSizeUiState.Available(ByteSizeFormatter.format(diagnosticsLogStore.storageBytes()))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        StorageSizeUiState.Unavailable
    }
}
