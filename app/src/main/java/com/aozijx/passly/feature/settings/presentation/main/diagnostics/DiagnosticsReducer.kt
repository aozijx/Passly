package com.aozijx.passly.feature.settings.presentation.main.diagnostics

import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import com.aozijx.passly.feature.settings.diagnostics.LogFormat

internal sealed interface DiagnosticsMutation {
    data object Loading : DiagnosticsMutation

    data class LinesLoaded(
        val lines: List<DiagnosticsLogLine>,
    ) : DiagnosticsMutation

    data object SessionRestricted : DiagnosticsMutation

    data object RefreshFailed : DiagnosticsMutation

    data class LevelToggled(
        val level: LogLevel,
    ) : DiagnosticsMutation

    data class SearchChanged(
        val query: String,
    ) : DiagnosticsMutation

    data class ExportActionSelected(
        val action: ExportAction,
    ) : DiagnosticsMutation

    data object FormatSheetDismissed : DiagnosticsMutation

    data class LocalFormatSelected(
        val format: LogFormat,
    ) : DiagnosticsMutation

    data object LocalExportConsumed : DiagnosticsMutation

    data object ClearRequested : DiagnosticsMutation

    data object ClearDismissed : DiagnosticsMutation

    data object ClearStarted : DiagnosticsMutation

    data object ClearFinished : DiagnosticsMutation

    data object LogsCleared : DiagnosticsMutation
}

internal object DiagnosticsReducer {
    fun reduce(
        state: DiagnosticsUiState,
        mutation: DiagnosticsMutation,
    ): DiagnosticsUiState =
        when (mutation) {
            DiagnosticsMutation.Loading -> {
                state.copy(loadState = DiagnosticsLoadState.Loading(state.retainedLines))
            }

            is DiagnosticsMutation.LinesLoaded -> {
                state.copy(
                    loadState = DiagnosticsLoadState.Ready(mutation.lines),
                )
            }

            DiagnosticsMutation.SessionRestricted -> {
                state.copy(
                    loadState = DiagnosticsLoadState.SessionRestricted,
                )
            }

            DiagnosticsMutation.RefreshFailed -> {
                state.copy(loadState = DiagnosticsLoadState.RefreshFailed(state.retainedLines))
            }

            is DiagnosticsMutation.LevelToggled -> {
                state.copy(
                    selectedLevels = state.selectedLevels.toggle(mutation.level),
                )
            }

            is DiagnosticsMutation.SearchChanged -> {
                state.copy(searchQuery = mutation.query)
            }

            is DiagnosticsMutation.ExportActionSelected -> {
                state.copy(exportState = DiagnosticsExportState.SelectingFormat(mutation.action))
            }

            DiagnosticsMutation.FormatSheetDismissed -> {
                state.copy(exportState = DiagnosticsExportState.None)
            }

            is DiagnosticsMutation.LocalFormatSelected -> {
                state.copy(
                    exportState = DiagnosticsExportState.SelectingLocalDocument(mutation.format),
                )
            }

            DiagnosticsMutation.LocalExportConsumed -> {
                state.copy(exportState = DiagnosticsExportState.None)
            }

            DiagnosticsMutation.ClearRequested -> {
                state.copy(
                    clearState =
                        if (state.clearState is DiagnosticsClearState.Clearing) {
                            DiagnosticsClearState.Clearing(confirmationVisible = true)
                        } else {
                            DiagnosticsClearState.Confirmation
                        },
                )
            }

            DiagnosticsMutation.ClearDismissed -> {
                state.copy(
                    clearState =
                        when (val clearState = state.clearState) {
                            is DiagnosticsClearState.Clearing -> clearState.copy(confirmationVisible = false)
                            else -> DiagnosticsClearState.Idle
                        },
                )
            }

            DiagnosticsMutation.ClearStarted -> {
                state.copy(
                    clearState =
                        when (val clearState = state.clearState) {
                            is DiagnosticsClearState.Clearing -> clearState
                            DiagnosticsClearState.Confirmation -> DiagnosticsClearState.Clearing(true)
                            DiagnosticsClearState.Idle -> DiagnosticsClearState.Clearing(false)
                        },
                )
            }

            DiagnosticsMutation.ClearFinished -> {
                state.copy(
                    clearState =
                        when (val clearState = state.clearState) {
                            is DiagnosticsClearState.Clearing -> {
                                if (clearState.confirmationVisible) DiagnosticsClearState.Confirmation else DiagnosticsClearState.Idle
                            }

                            else -> {
                                clearState
                            }
                        },
                )
            }

            DiagnosticsMutation.LogsCleared -> {
                state.copy(
                    loadState = DiagnosticsLoadState.Ready(emptyList()),
                    clearState = DiagnosticsClearState.Idle,
                )
            }
        }

    private fun Set<LogLevel>.toggle(level: LogLevel): Set<LogLevel> {
        require(level != LogLevel.FATAL) { "FATAL is grouped under ERROR" }
        return if (level in this) this - level else this + level
    }
}
