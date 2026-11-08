package com.aozijx.passly.feature.settings.presentation.main.diagnostics

import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import com.aozijx.passly.feature.settings.diagnostics.LogFormat

data class DiagnosticsUiState(
    val loadState: DiagnosticsLoadState = DiagnosticsLoadState.Loading(emptyList()),
    val clearState: DiagnosticsClearState = DiagnosticsClearState.Idle,
    val selectedLevels: Set<LogLevel> = DEFAULT_VISIBLE_LEVELS,
    val searchQuery: String = "",
    val exportState: DiagnosticsExportState = DiagnosticsExportState.None,
) {
    val retainedLines: List<DiagnosticsLogLine>
        get() = loadState.retainedLines

    val visibleLines: List<DiagnosticsLogLine>
        get() {
            val query = searchQuery.trim()
            return retainedLines.filter { line ->
                line.matchesSelectedLevel() && (query.isEmpty() || line.matches(query))
            }
        }

    private fun DiagnosticsLogLine.matches(query: String): Boolean =
        listOfNotNull(traceId, event, errorCode, exceptionKind, exceptionType)
            .any { it.contains(query, ignoreCase = true) }

    private fun DiagnosticsLogLine.matchesSelectedLevel(): Boolean =
        when (level) {
            LogLevel.FATAL -> LogLevel.ERROR in selectedLevels
            else -> level in selectedLevels
        }

    companion object {
        val DEFAULT_VISIBLE_LEVELS = setOf(LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR)
    }
}

sealed interface DiagnosticsLoadState {
    val retainedLines: List<DiagnosticsLogLine>

    data class Loading(override val retainedLines: List<DiagnosticsLogLine>) : DiagnosticsLoadState

    data class Ready(override val retainedLines: List<DiagnosticsLogLine>) : DiagnosticsLoadState

    data class RefreshFailed(override val retainedLines: List<DiagnosticsLogLine>) : DiagnosticsLoadState

    data object SessionRestricted : DiagnosticsLoadState {
        override val retainedLines: List<DiagnosticsLogLine> = emptyList()
    }
}

sealed interface DiagnosticsClearState {
    data object Idle : DiagnosticsClearState

    data object Confirmation : DiagnosticsClearState

    data class Clearing(val confirmationVisible: Boolean) : DiagnosticsClearState
}

val DiagnosticsClearState.isConfirmationVisible: Boolean
    get() = this == DiagnosticsClearState.Confirmation || (this as? DiagnosticsClearState.Clearing)?.confirmationVisible == true

val DiagnosticsClearState.isClearing: Boolean
    get() = this is DiagnosticsClearState.Clearing

sealed interface DiagnosticsExportState {
    data object None : DiagnosticsExportState

    data class SelectingFormat(val action: ExportAction) : DiagnosticsExportState

    data class SelectingLocalDocument(val format: LogFormat) : DiagnosticsExportState
}

sealed interface DiagnosticsAction {
    data object Refresh : DiagnosticsAction

    data class ToggleLevel(
        val level: LogLevel,
    ) : DiagnosticsAction

    data class SearchChanged(
        val query: String,
    ) : DiagnosticsAction

    data class CopyRow(
        val line: DiagnosticsLogLine,
    ) : DiagnosticsAction

    data class CopyTrace(
        val traceId: String,
    ) : DiagnosticsAction

    data class RequestExport(
        val action: ExportAction,
    ) : DiagnosticsAction

    data object DismissFormatSheet : DiagnosticsAction

    data class SelectFormat(
        val format: LogFormat,
    ) : DiagnosticsAction

    data class SaveToDocument(
        val uri: String,
        val pageOwner: Any,
        val requestId: String,
    ) : DiagnosticsAction

    data class SaveDocumentCancelled(
        val pageOwner: Any,
        val requestId: String,
    ) : DiagnosticsAction

    data object RequestClear : DiagnosticsAction

    data object DismissClear : DiagnosticsAction

    data object ConfirmClear : DiagnosticsAction
}

sealed interface DiagnosticsEffect {
    data class SelectSaveDocument(
        val format: LogFormat,
        val mimeType: String,
        val fileName: String,
        val requestId: String,
    ) : DiagnosticsEffect

    data object SaveCompleted : DiagnosticsEffect

    data object ExportFailed : DiagnosticsEffect

    data object ClearFailed : DiagnosticsEffect
}
