package com.aozijx.passly.feature.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.DiagnosticsExportService
import com.aozijx.passly.app.diagnostics.DiagnosticsRuntimeController
import com.aozijx.passly.core.telemetry.TelemetryPolicyController
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aozijx.passly.app.diagnostics.AppTelemetry

@HiltViewModel
class DiagnosticsSettingsViewModel @Inject constructor(
    private val policies: TelemetryPolicyController,
    private val authenticationManager: AuthenticationManager,
    private val runtime: DiagnosticsRuntimeController,
    private val exportService: DiagnosticsExportService
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
        if (authenticationManager.state.value is AuthenticationState.Authenticated) {
            withContext(Dispatchers.IO) {
                runtime.readLines(MAX_VIEW_LINES).joinToString("\n")
            }
        } else {
            ""
        }

    private fun clearLogs() = viewModelScope.launch(Dispatchers.IO) {
        if (authenticationManager.state.value !is AuthenticationState.Authenticated) return@launch
        runtime.clear()
        mutate(DiagnosticsSettingsMutation.LogsCleared)
    }

    private fun authenticateAndExport() = viewModelScope.launch {
        if (authenticationManager.state.value !is AuthenticationState.Authenticated) {
            eventChannel.trySend(DiagnosticsSettingsEffect.ExportFailed)
            return@launch
        }
        val result = authenticationManager.authenticate(
            AuthenticationRequest(AuthenticationPurpose.EXPORT_DIAGNOSTICS)
        )
        if (result !is AuthenticationResult.Success) return@launch
        runCatching {
            withContext(Dispatchers.IO) {
                exportService.createPlaintextExport()
            }
        }.mapCatching { file ->
            exportService.share(file).getOrThrow()
        }.onFailure { error ->
            AppTelemetry.e("DiagnosticsExport", "Plaintext diagnostics export failed", error)
            eventChannel.trySend(DiagnosticsSettingsEffect.ExportFailed)
        }
    }

    private fun mutate(mutation: DiagnosticsSettingsMutation) {
        _uiState.update { state -> DiagnosticsSettingsReducer.reduce(state, mutation) }
    }

    private companion object {
        const val MAX_VIEW_LINES = 500
    }
}
