package com.aozijx.passly.presentation.feature.settings.main.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import com.aozijx.passly.core.telemetry.TelemetryPolicyController
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.aozijx.passly.core.telemetry.TelemetryRuntime
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportResult
import com.aozijx.passly.feature.settings.diagnostics.ExportDiagnosticsUseCase

@HiltViewModel
class DiagnosticsSettingsViewModel @Inject constructor(
    private val policies: TelemetryPolicyController,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val logStore: DiagnosticsLogStore,
    private val exportDiagnostics: ExportDiagnosticsUseCase
) : ViewModel() {
    private val eventChannel = Channel<DiagnosticsSettingsEffect>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(DiagnosticsSettingsUiState())
    val uiState: StateFlow<DiagnosticsSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            policies.policies.collect { policy ->
                mutate(
                    DiagnosticsSettingsMutation.FileLoggingChanged(
                        policy.isEncryptedFileEnabled()
                    )
                )
            }
        }
    }

    fun onAction(action: DiagnosticsSettingsAction) {
        when (action) {
            is DiagnosticsSettingsAction.SetFileLoggingEnabled ->
                setFileLoggingEnabled(action.enabled)
            DiagnosticsSettingsAction.OpenViewer -> openViewer()
            DiagnosticsSettingsAction.CloseViewer ->
                mutate(DiagnosticsSettingsMutation.ViewerClosed)
            DiagnosticsSettingsAction.RequestClear ->
                mutate(DiagnosticsSettingsMutation.ClearRequested)
            DiagnosticsSettingsAction.DismissClear ->
                mutate(DiagnosticsSettingsMutation.ClearDismissed)
            DiagnosticsSettingsAction.ConfirmClear -> clearLogs()
            DiagnosticsSettingsAction.Export -> authenticateAndExport()
        }
    }

    private fun setFileLoggingEnabled(enabled: Boolean) = viewModelScope.launch {
        if (enabled) policies.enableEncryptedFile() else policies.disableEncryptedFile()
    }

    private fun openViewer() {
        mutate(DiagnosticsSettingsMutation.ViewerOpened)
        viewModelScope.launch {
            val content = readPage()
            mutate(
                DiagnosticsSettingsMutation.LogPageLoaded(
                    content = content,
                    byteCount = content.toByteArray(Charsets.UTF_8).size,
                )
            )
        }
    }

    private suspend fun readPage(): String =
        if (secureSessionAccessState.hasFullSecureSessionAccess()) {
            logStore.readLines(MAX_VIEW_LINES).joinToString("\n")
        } else {
            ""
        }

    private fun clearLogs() = viewModelScope.launch {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) return@launch
        logStore.clear()
        mutate(DiagnosticsSettingsMutation.LogsCleared)
    }

    private fun authenticateAndExport() = viewModelScope.launch {
        when (val result = exportDiagnostics()) {
            DiagnosticsExportResult.Completed,
            DiagnosticsExportResult.Cancelled,
                -> Unit
            DiagnosticsExportResult.Denied,
            DiagnosticsExportResult.SessionRestricted,
                -> eventChannel.trySend(DiagnosticsSettingsEffect.ExportFailed)
            is DiagnosticsExportResult.Failed -> {
                TelemetryRuntime.e(
                    "DiagnosticsExport",
                    "Plaintext diagnostics export failed",
                    result.cause,
                )
                eventChannel.trySend(DiagnosticsSettingsEffect.ExportFailed)
            }
        }
    }
    private fun mutate(mutation: DiagnosticsSettingsMutation) {
        _uiState.update { state -> DiagnosticsSettingsReducer.reduce(state, mutation) }
    }

    private companion object {
        const val MAX_VIEW_LINES = 500
    }
}
