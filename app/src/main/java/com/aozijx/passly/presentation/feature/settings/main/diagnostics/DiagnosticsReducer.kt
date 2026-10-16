package com.aozijx.passly.presentation.feature.settings.main.diagnostics

import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import com.aozijx.passly.feature.settings.diagnostics.LogFormat

internal sealed interface DiagnosticsMutation {
    data object Loading : DiagnosticsMutation
    data class LinesLoaded(val lines: List<DiagnosticsLogLine>, val byteCount: Long) : DiagnosticsMutation
    data object SessionRestricted : DiagnosticsMutation
    data class LevelToggled(val level: LogLevel) : DiagnosticsMutation
    data class SearchChanged(val query: String) : DiagnosticsMutation
    data class ExportActionSelected(val action: ExportAction) : DiagnosticsMutation
    data object FormatSheetDismissed : DiagnosticsMutation
    data class LocalFormatSelected(val format: LogFormat) : DiagnosticsMutation
    data object LocalExportConsumed : DiagnosticsMutation
    data object ClearRequested : DiagnosticsMutation
    data object ClearDismissed : DiagnosticsMutation
    data object LogsCleared : DiagnosticsMutation
}

internal object DiagnosticsReducer {
    fun reduce(state: DiagnosticsUiState, mutation: DiagnosticsMutation): DiagnosticsUiState = when (mutation) {
        DiagnosticsMutation.Loading -> state.copy(isLoading = true, isSessionRestricted = false)
        is DiagnosticsMutation.LinesLoaded -> state.copy(
            isLoading = false,
            isSessionRestricted = false,
            retainedLines = mutation.lines,
            logByteCount = mutation.byteCount,
        )
        DiagnosticsMutation.SessionRestricted -> state.copy(
            isLoading = false,
            isSessionRestricted = true,
            retainedLines = emptyList(),
            logByteCount = 0,
        )
        is DiagnosticsMutation.LevelToggled -> state.copy(
            selectedLevels = state.selectedLevels.toggle(mutation.level),
        )
        is DiagnosticsMutation.SearchChanged -> state.copy(searchQuery = mutation.query)
        is DiagnosticsMutation.ExportActionSelected -> state.copy(pendingExportAction = mutation.action)
        DiagnosticsMutation.FormatSheetDismissed -> state.copy(pendingExportAction = null)
        is DiagnosticsMutation.LocalFormatSelected -> state.copy(
            pendingExportAction = null,
            pendingLocalFormat = mutation.format,
        )
        DiagnosticsMutation.LocalExportConsumed -> state.copy(pendingLocalFormat = null)
        DiagnosticsMutation.ClearRequested -> state.copy(isClearConfirmationVisible = true)
        DiagnosticsMutation.ClearDismissed -> state.copy(isClearConfirmationVisible = false)
        DiagnosticsMutation.LogsCleared -> state.copy(
            retainedLines = emptyList(),
            logByteCount = 0,
            isClearConfirmationVisible = false,
        )
    }

    private fun Set<LogLevel>.toggle(level: LogLevel): Set<LogLevel> {
        require(level != LogLevel.FATAL) { "FATAL is grouped under ERROR" }
        return if (level in this) this - level else this + level
    }
}
