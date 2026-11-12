package com.aozijx.passly.feature.settings.presentation.main.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.app.settings.SettingsReadRetrySignal
import com.aozijx.passly.core.observability.sink.LogClearResult
import com.aozijx.passly.core.platform.cache.ByteSizeFormatter
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import com.aozijx.passly.domain.settings.port.DiagnosticsLogSettingsRepository
import com.aozijx.passly.feature.settings.data.AppCacheStore
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import com.aozijx.passly.presentation.shared.components.group.model.SettingsItemValue
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StorageSettingsViewModel
    @Inject
    constructor(
        private val cacheStore: AppCacheStore,
        private val diagnosticsLogStore: DiagnosticsLogStore,
        private val diagnosticsLogSettings: DiagnosticsLogSettingsRepository,
        private val secureSessionAccessState: SecureSessionAccessState,
        private val settingsReadRetrySignal: SettingsReadRetrySignal = SettingsReadRetrySignal(),
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(StorageSettingsUiState())
        val uiState: StateFlow<StorageSettingsUiState> = _uiState.asStateFlow()

        private val pageEffects = PageEffects<StorageSettingsEffect>()
        val effects = pageEffects.effects

        private var cacheRefreshJob: Job? = null
        private var logRefreshJob: Job? = null
        private var isCacheClearInProgress = false
        private var isLogClearInProgress = false
        private val recordingModeRead =
            RecoverableRead(
                source = diagnosticsLogSettings.diagnosticsLogRecordingMode,
                scope = viewModelScope,
                sharedRetrySignal = settingsReadRetrySignal,
            )

        init {
            viewModelScope.launch {
                recordingModeRead.state.collect { mode ->
                    _uiState.update { it.copy(logRecordingMode = mode) }
                }
            }
        }

        fun onAction(action: StorageSettingsAction) {
            when (action) {
                StorageSettingsAction.ClearCache -> clearCache()
                StorageSettingsAction.ClearLogs -> clearLogs()
                StorageSettingsAction.RetryLogRecordingMode -> recordingModeRead.retry()
                is StorageSettingsAction.SetLogRecordingMode -> setLogRecordingMode(action.mode)
            }
        }

        private fun setLogRecordingMode(mode: DiagnosticsLogRecordingMode) {
            if (_uiState.value.logRecordingMode !is RecoverableReadState.Ready) return
            val owner = pageEffects.currentOwner ?: return
            viewModelScope.launch {
                try {
                    diagnosticsLogSettings.setDiagnosticsLogRecordingMode(mode)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    emitEffect(owner, StorageSettingsEffect.LogRecordingSettingsSaveFailed)
                }
            }
        }

        private fun clearLogs() {
            val owner = pageEffects.currentOwner ?: return
            if (isLogClearInProgress) return
            isLogClearInProgress = true
            viewModelScope.launch {
                try {
                    if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
                        val currentOwner = pageEffects.currentOwner
                        if (currentOwner != null && currentOwner !== owner) {
                            isLogClearInProgress = false
                            refreshLogSize(currentOwner)
                        }
                        emitEffect(owner, StorageSettingsEffect.LogClearFailed)
                        return@launch
                    }
                    logRefreshJob?.cancel()
                    if (pageEffects.isActive(owner)) {
                        _uiState.update { it.copy(logs = SettingsItemValue.Loading) }
                    }
                    val result = diagnosticsLogStore.clear()
                    if (result != LogClearResult.Cleared) {
                        reportLogClearFailed(owner)
                        return@launch
                    }
                    val logs = readLogSize()
                    if (pageEffects.currentOwner != null) {
                        _uiState.update { it.copy(logs = logs) }
                    }
                    emitEffect(owner, StorageSettingsEffect.LogsCleared)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    reportLogClearFailed(owner)
                } finally {
                    isLogClearInProgress = false
                }
            }
        }

        private suspend fun reportLogClearFailed(owner: Any) {
            if (pageEffects.currentOwner != null) {
                _uiState.update { it.copy(logs = SettingsItemValue.Unavailable) }
            }
            emitEffect(owner, StorageSettingsEffect.LogClearFailed)
        }

        private fun emitEffect(
            owner: Any,
            effect: StorageSettingsEffect,
        ) {
            pageEffects.tryEmit(owner, effect)
        }

        fun onPageEntered(): Any {
            val owner = pageEffects.enter()
            refreshStorageUsage(owner)
            return owner
        }

        fun onPageExited(owner: Any) {
            if (!pageEffects.isActive(owner)) return
            pageEffects.exit(owner)
            cacheRefreshJob?.cancel()
            cacheRefreshJob = null
            logRefreshJob?.cancel()
            logRefreshJob = null
        }

        private fun refreshStorageUsage(owner: Any) {
            if (pageEffects.isActive(owner)) {
                _uiState.update {
                    it.copy(
                        cache = SettingsItemValue.Loading,
                        logs = SettingsItemValue.Loading,
                    )
                }
            }
            refreshCacheSize(owner)
            refreshLogSize(owner)
        }

        private fun refreshCacheSize(owner: Any) {
            cacheRefreshJob?.cancel()
            cacheRefreshJob = null
            if (!pageEffects.isActive(owner) || isCacheClearInProgress) return
            cacheRefreshJob =
                viewModelScope.launch {
                    val cache = readCacheSize()
                    if (pageEffects.isActive(owner)) {
                        _uiState.update { it.copy(cache = cache) }
                    }
                }
        }

        private fun refreshLogSize(owner: Any) {
            logRefreshJob?.cancel()
            logRefreshJob = null
            if (!pageEffects.isActive(owner) || isLogClearInProgress) return
            logRefreshJob =
                viewModelScope.launch {
                    val logs = readLogSize()
                    if (pageEffects.isActive(owner)) {
                        _uiState.update { it.copy(logs = logs) }
                    }
                }
        }

        private fun clearCache() {
            val owner = pageEffects.currentOwner ?: return
            if (isCacheClearInProgress) return
            isCacheClearInProgress = true
            cacheRefreshJob?.cancel()
            if (pageEffects.isActive(owner)) {
                _uiState.update { it.copy(cache = SettingsItemValue.Loading) }
            }
            viewModelScope.launch {
                try {
                    cacheStore.clear()
                    if (pageEffects.currentOwner == null) return@launch
                    val cache = readCacheSize()
                    if (pageEffects.currentOwner != null) {
                        _uiState.update { it.copy(cache = cache) }
                    }
                    emitEffect(owner, StorageSettingsEffect.CacheCleared)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    val cache = readCacheSize()
                    if (pageEffects.currentOwner != null) {
                        _uiState.update { it.copy(cache = cache) }
                    }
                    emitEffect(owner, StorageSettingsEffect.CacheClearFailed)
                } finally {
                    isCacheClearInProgress = false
                }
            }
        }

        private suspend fun readCacheSize(): SettingsItemValue =
            try {
                SettingsItemValue.Text(ByteSizeFormatter.format(cacheStore.sizeBytes()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SettingsItemValue.Unavailable
            }

        private suspend fun readLogSize(): SettingsItemValue =
            try {
                SettingsItemValue.Text(ByteSizeFormatter.format(diagnosticsLogStore.storageBytes()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SettingsItemValue.Unavailable
            }
    }
