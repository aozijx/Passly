package com.aozijx.passly.presentation.feature.settings.main.diagnostics

import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import com.aozijx.passly.feature.settings.diagnostics.LogFormat

data class DiagnosticsUiState(
    val isLoading: Boolean = true,
    val isSessionRestricted: Boolean = false,
    val retainedLines: List<DiagnosticsLogLine> = emptyList(),
    val selectedLevels: Set<LogLevel> = DEFAULT_VISIBLE_LEVELS,
    val searchQuery: String = "",
    val pendingExportAction: ExportAction? = null,
    val pendingLocalFormat: LogFormat? = null,
    val isClearConfirmationVisible: Boolean = false,
) {
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

    private fun DiagnosticsLogLine.matchesSelectedLevel(): Boolean = when (level) {
        LogLevel.FATAL -> LogLevel.ERROR in selectedLevels
        else -> level in selectedLevels
    }

    companion object {
        val DEFAULT_VISIBLE_LEVELS = setOf(LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR)
    }
}

sealed interface DiagnosticsAction {
    data object PageEntered : DiagnosticsAction
    data object Refresh : DiagnosticsAction
    data class ToggleLevel(val level: LogLevel) : DiagnosticsAction
    data class SearchChanged(val query: String) : DiagnosticsAction
    data class CopyRow(val line: DiagnosticsLogLine) : DiagnosticsAction
    data class CopyTrace(val traceId: String) : DiagnosticsAction
    data class RequestExport(val action: ExportAction) : DiagnosticsAction
    data object DismissFormatSheet : DiagnosticsAction
    data class SelectFormat(val format: LogFormat) : DiagnosticsAction
    data class SaveToDocument(val uri: String) : DiagnosticsAction
    data object SaveDocumentCancelled : DiagnosticsAction
    data object RequestClear : DiagnosticsAction
    data object DismissClear : DiagnosticsAction
    data object ConfirmClear : DiagnosticsAction
}

sealed interface DiagnosticsEffect {
    data class SelectSaveDocument(
        val format: LogFormat,
        val mimeType: String,
        val fileName: String,
    ) : DiagnosticsEffect

    data class CopyText(val text: String) : DiagnosticsEffect
    data object SaveCompleted : DiagnosticsEffect
    data object ExportFailed : DiagnosticsEffect
}
