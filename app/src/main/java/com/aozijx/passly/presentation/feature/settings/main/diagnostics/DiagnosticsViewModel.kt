package com.aozijx.passly.presentation.feature.settings.main.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportRequest
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportResult
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import com.aozijx.passly.feature.settings.diagnostics.ExportDiagnosticsUseCase
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
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
class DiagnosticsViewModel @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
    private val logStore: DiagnosticsLogStore,
    private val exportDiagnostics: ExportDiagnosticsUseCase,
    private val logger: Logger,
) : ViewModel() {
    private val effectChannel = Channel<DiagnosticsEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    fun onAction(action: DiagnosticsAction) {
        when (action) {
            DiagnosticsAction.PageEntered,
            DiagnosticsAction.Refresh,
                -> refresh()
            is DiagnosticsAction.ToggleLevel -> mutate(DiagnosticsMutation.LevelToggled(action.level))
            is DiagnosticsAction.SearchChanged -> mutate(DiagnosticsMutation.SearchChanged(action.query))
            is DiagnosticsAction.CopyRow -> copyRows(
                _uiState.value.retainedLines.filter { it == action.line }.take(1),
            )
            is DiagnosticsAction.CopyTrace -> copyRows(
                _uiState.value.retainedLines.filter { it.traceId == action.traceId }.take(MAX_COPY_LINES),
            )
            is DiagnosticsAction.RequestExport -> mutate(DiagnosticsMutation.ExportActionSelected(action.action))
            DiagnosticsAction.DismissFormatSheet -> mutate(DiagnosticsMutation.FormatSheetDismissed)
            is DiagnosticsAction.SelectFormat -> selectFormat(action.format)
            is DiagnosticsAction.SaveToDocument -> saveToDocument(action.uri)
            DiagnosticsAction.SaveDocumentCancelled -> mutate(DiagnosticsMutation.LocalExportConsumed)
            DiagnosticsAction.RequestClear -> mutate(DiagnosticsMutation.ClearRequested)
            DiagnosticsAction.DismissClear -> mutate(DiagnosticsMutation.ClearDismissed)
            DiagnosticsAction.ConfirmClear -> clearLogs()
        }
    }

    private fun refresh() {
        refreshJob?.cancel()
        mutate(DiagnosticsMutation.Loading)
        refreshJob = viewModelScope.launch {
            if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
                mutate(DiagnosticsMutation.SessionRestricted)
                return@launch
            }
            val lines = logStore.readLines(MAX_VIEW_LINES)
            mutate(DiagnosticsMutation.LinesLoaded(lines, lines.byteCount()))
        }
    }

    private fun selectFormat(format: LogFormat) {
        when (_uiState.value.pendingExportAction) {
            ExportAction.SHARE -> {
                mutate(DiagnosticsMutation.FormatSheetDismissed)
                authenticateAndExport(DiagnosticsExportRequest(ExportAction.SHARE, format))
            }
            ExportAction.EXPORT_LOCAL -> {
                mutate(DiagnosticsMutation.LocalFormatSelected(format))
                effectChannel.trySend(
                    DiagnosticsEffect.SelectSaveDocument(
                        format = format,
                        mimeType = format.mimeType,
                        fileName = "passly_diagnostics.${format.extension}",
                    ),
                )
            }
            null -> Unit
        }
    }

    private fun saveToDocument(uri: String) {
        val format = _uiState.value.pendingLocalFormat ?: return
        mutate(DiagnosticsMutation.LocalExportConsumed)
        authenticateAndExport(DiagnosticsExportRequest(ExportAction.EXPORT_LOCAL, format, uri))
    }

    private fun copyRows(lines: List<DiagnosticsLogLine>) {
        if (lines.isEmpty()) return
        effectChannel.trySend(DiagnosticsEffect.CopyText(lines.joinToString("\n") { it.text }))
    }

    private fun clearLogs() = viewModelScope.launch {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
            mutate(DiagnosticsMutation.SessionRestricted)
            return@launch
        }
        refreshJob?.cancelAndJoin()
        logStore.clear()
        mutate(DiagnosticsMutation.LogsCleared)
    }

    private fun authenticateAndExport(request: DiagnosticsExportRequest) = viewModelScope.launch {
        logger.withRootTrace(Operations.DiagnosticsExport) {
            when (val result = exportDiagnostics(request)) {
                DiagnosticsExportResult.Completed -> if (request.action == ExportAction.EXPORT_LOCAL) {
                    effectChannel.trySend(DiagnosticsEffect.SaveCompleted)
                }
                DiagnosticsExportResult.Cancelled -> Unit
                DiagnosticsExportResult.Denied,
                DiagnosticsExportResult.SessionRestricted,
                    -> effectChannel.trySend(DiagnosticsEffect.ExportFailed)
                is DiagnosticsExportResult.Failed -> {
                    logger.log(LogStatements.Application.diagnosticsExportFailed(result.cause))
                    effectChannel.trySend(DiagnosticsEffect.ExportFailed)
                }
            }
        }
    }

    private fun mutate(mutation: DiagnosticsMutation) {
        _uiState.update { DiagnosticsReducer.reduce(it, mutation) }
    }

    private fun List<DiagnosticsLogLine>.byteCount(): Int =
        joinToString("\n") { it.text }.toByteArray(Charsets.UTF_8).size

    private companion object {
        const val MAX_VIEW_LINES = 500
        const val MAX_COPY_LINES = 100
    }
}
