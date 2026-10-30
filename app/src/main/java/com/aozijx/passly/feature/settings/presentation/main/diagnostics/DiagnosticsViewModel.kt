package com.aozijx.passly.feature.settings.presentation.main.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.sink.LogClearResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportRequest
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportResult
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import com.aozijx.passly.feature.settings.diagnostics.ExportDiagnosticsUseCase
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
    private val logStore: DiagnosticsLogStore,
    private val exportDiagnostics: ExportDiagnosticsUseCase,
    private val logger: Logger,
    private val clipboardWriter: SensitiveClipboardWriter,
) : ViewModel() {
    private val pageEffects = PageEffects<DiagnosticsEffect>()
    val effects = pageEffects.effects

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null
    private var activeRefreshOwner: Any? = null
    private var copyJob: Job? = null
    private var activeCopyOwner: Any? = null
    private var activePageOwner: Any? = null

    fun onAction(action: DiagnosticsAction) {
        when (action) {
            DiagnosticsAction.PageEntered -> pageEntered()
            DiagnosticsAction.PageExited -> pageExited()
            DiagnosticsAction.Refresh -> refresh()
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

    private fun pageEntered() {
        activePageOwner = pageEffects.enter()
        refresh()
    }

    private fun refresh() {
        val pageOwner = activePageOwner ?: return
        val owner = Any()
        activeRefreshOwner = owner
        refreshJob?.cancel()
        mutate(DiagnosticsMutation.Loading)
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
                    if (activePageOwner === pageOwner && activeRefreshOwner === owner) {
                        mutate(DiagnosticsMutation.SessionRestricted)
                    }
                    return@launch
                }
                val lines = logStore.readLines(MAX_VIEW_LINES)
                if (activePageOwner === pageOwner && activeRefreshOwner === owner) {
                    mutate(DiagnosticsMutation.LinesLoaded(lines))
                }
            } finally {
                if (activeRefreshOwner === owner) {
                    activeRefreshOwner = null
                    refreshJob = null
                }
            }
        }
        refreshJob = job
        job.start()
    }

    private fun selectFormat(format: LogFormat) {
        val pageOwner = activePageOwner ?: return
        when (_uiState.value.pendingExportAction) {
            ExportAction.SHARE -> {
                mutate(DiagnosticsMutation.FormatSheetDismissed)
                authenticateAndExport(DiagnosticsExportRequest(ExportAction.SHARE, format), pageOwner)
            }
            ExportAction.EXPORT_LOCAL -> {
                mutate(DiagnosticsMutation.LocalFormatSelected(format))
                offerEffect(
                    pageOwner,
                    DiagnosticsEffect.SelectSaveDocument(
                        format = format,
                        mimeType = format.mimeType,
                        fileName = "passly_logs.${format.extension}",
                    ),
                )
            }
            null -> Unit
        }
    }

    private fun saveToDocument(uri: String) {
        val pageOwner = activePageOwner ?: return
        val format = _uiState.value.pendingLocalFormat ?: return
        mutate(DiagnosticsMutation.LocalExportConsumed)
        authenticateAndExport(DiagnosticsExportRequest(ExportAction.EXPORT_LOCAL, format, uri), pageOwner)
    }

    private fun copyRows(lines: List<DiagnosticsLogLine>) {
        val pageOwner = activePageOwner ?: return
        if (lines.isEmpty()) return
        val text = lines.joinToString("\n") { it.text }
        val owner = Any()
        activeCopyOwner = owner
        copyJob?.cancel()
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                clipboardWriter.writeSensitiveIf(text) {
                    activePageOwner === pageOwner && activeCopyOwner === owner
                }
            } finally {
                if (activeCopyOwner === owner) {
                    activeCopyOwner = null
                    copyJob = null
                }
            }
        }
        copyJob = job
        job.start()
    }

    private fun pageExited() {
        activePageOwner?.let(pageEffects::exit)
        activePageOwner = null
        activeCopyOwner = null
        copyJob?.cancel()
        copyJob = null
        activeRefreshOwner = null
        refreshJob?.cancel()
        refreshJob = null
    }

    private fun clearLogs() {
        val pageOwner = activePageOwner ?: return
        viewModelScope.launch {
            if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
                if (activePageOwner === pageOwner) {
                    mutate(DiagnosticsMutation.SessionRestricted)
                }
                return@launch
            }
            refreshJob?.cancelAndJoin()
            val result = try {
                logStore.clear()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                LogClearResult.Failed
            }
            when (result) {
                LogClearResult.Cleared -> if (activePageOwner != null) {
                    mutate(DiagnosticsMutation.LogsCleared)
                }
                LogClearResult.Failed -> {
                    if (activePageOwner != null) {
                        mutate(DiagnosticsMutation.ClearDismissed)
                    }
                    offerEffect(pageOwner, DiagnosticsEffect.ClearFailed)
                }
            }
        }
    }

    private fun authenticateAndExport(request: DiagnosticsExportRequest, pageOwner: Any) = viewModelScope.launch {
        logger.withRootTrace(Operations.DiagnosticsExport) {
            when (val result = exportDiagnostics(request)) {
                DiagnosticsExportResult.Completed -> if (request.action == ExportAction.EXPORT_LOCAL) {
                    offerEffect(pageOwner, DiagnosticsEffect.SaveCompleted)
                }
                DiagnosticsExportResult.Cancelled -> Unit
                DiagnosticsExportResult.Denied,
                DiagnosticsExportResult.SessionRestricted,
                    -> offerEffect(pageOwner, DiagnosticsEffect.ExportFailed)
                is DiagnosticsExportResult.Failed -> {
                    logger.log(LogStatements.Application.diagnosticsExportFailed(result.cause))
                    offerEffect(pageOwner, DiagnosticsEffect.ExportFailed)
                }
            }
        }
    }

    private fun offerEffect(owner: Any, effect: DiagnosticsEffect) {
        pageEffects.tryEmit(owner, effect)
    }

    private fun mutate(mutation: DiagnosticsMutation) {
        _uiState.update { DiagnosticsReducer.reduce(it, mutation) }
    }

    private companion object {
        const val MAX_VIEW_LINES = 500
        const val MAX_COPY_LINES = 100
    }
}
