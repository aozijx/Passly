package com.aozijx.passly.presentation.feature.settings.main.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportResult
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import com.aozijx.passly.feature.settings.diagnostics.ExportDiagnosticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DiagnosticsSettingsViewModel @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
    private val logStore: DiagnosticsLogStore,
    private val exportDiagnostics: ExportDiagnosticsUseCase,
    private val logger: Logger,
) : ViewModel() {
    private val eventChannel = Channel<DiagnosticsSettingsEffect>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(DiagnosticsSettingsUiState())
    val uiState: StateFlow<DiagnosticsSettingsUiState> = _uiState.asStateFlow()
    private var cachedLogContent: String? = null
    private var refreshJob: Job? = null

    fun onAction(action: DiagnosticsSettingsAction) {
        when (action) {
            DiagnosticsSettingsAction.RefreshLogs -> refreshLogs()
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

    private fun openViewer() {
        mutate(DiagnosticsSettingsMutation.ViewerOpened)
        val content = cachedLogContent
        if (content != null) {
            mutate(
                DiagnosticsSettingsMutation.LogPageLoaded(
                    content = content,
                    byteCount = content.byteCount(),
                ),
            )
        } else if (refreshJob?.isActive != true) {
            refreshLogs()
        }
    }

    private fun refreshLogs() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val content = readPage()
            cachedLogContent = content
            val byteCount = content.byteCount()
            mutate(DiagnosticsSettingsMutation.LogCacheMeasured(byteCount))
            if (_uiState.value.overlay is DiagnosticsOverlayUiState.Viewer) {
                mutate(DiagnosticsSettingsMutation.LogPageLoaded(content, byteCount))
            }
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
        refreshJob?.cancelAndJoin()
        logStore.clear()
        cachedLogContent = ""
        mutate(DiagnosticsSettingsMutation.LogsCleared)
    }

    private fun authenticateAndExport() = viewModelScope.launch {
        logger.withRootTrace(Operations.DiagnosticsExport) {
            when (val result = exportDiagnostics()) {
                DiagnosticsExportResult.Completed,
                DiagnosticsExportResult.Cancelled,
                    -> Unit
                DiagnosticsExportResult.Denied,
                DiagnosticsExportResult.SessionRestricted,
                    -> eventChannel.trySend(DiagnosticsSettingsEffect.ExportFailed)
                is DiagnosticsExportResult.Failed -> {
                    logger.log(LogStatements.Application.diagnosticsExportFailed(result.cause))
                    eventChannel.trySend(DiagnosticsSettingsEffect.ExportFailed)
                }
            }
        }
    }

    private fun mutate(mutation: DiagnosticsSettingsMutation) {
        _uiState.update { state -> DiagnosticsSettingsReducer.reduce(state, mutation) }
    }

    private fun String.byteCount(): Int = toByteArray(Charsets.UTF_8).size

    private companion object {
        const val MAX_VIEW_LINES = 500
    }
}
