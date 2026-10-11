package com.aozijx.passly.presentation.feature.settings.main.general

internal sealed interface DiagnosticsSettingsMutation {
    data object ViewerOpened : DiagnosticsSettingsMutation
    data object ViewerClosed : DiagnosticsSettingsMutation
    data class LogPageLoaded(
        val content: String,
        val byteCount: Int,
    ) : DiagnosticsSettingsMutation
    data object ClearRequested : DiagnosticsSettingsMutation
    data object ClearDismissed : DiagnosticsSettingsMutation
    data object LogsCleared : DiagnosticsSettingsMutation
}

internal object DiagnosticsSettingsReducer {
    fun reduce(
        state: DiagnosticsSettingsUiState,
        mutation: DiagnosticsSettingsMutation,
    ): DiagnosticsSettingsUiState = when (mutation) {
        DiagnosticsSettingsMutation.ViewerOpened -> state.copy(
            overlay = DiagnosticsOverlayUiState.Viewer(DiagnosticsLogPageUiState.Loading),
        )
        DiagnosticsSettingsMutation.ViewerClosed -> state.copy(
            overlay = DiagnosticsOverlayUiState.Closed,
        )
        is DiagnosticsSettingsMutation.LogPageLoaded -> if (
            state.overlay is DiagnosticsOverlayUiState.Viewer
        ) {
            state.copy(
                overlay = DiagnosticsOverlayUiState.Viewer(
                    DiagnosticsLogPageUiState.Content(mutation.content),
                ),
                logByteCount = mutation.byteCount,
            )
        } else {
            state
        }
        DiagnosticsSettingsMutation.ClearRequested -> state.copy(
            overlay = DiagnosticsOverlayUiState.ClearConfirmation,
        )
        DiagnosticsSettingsMutation.ClearDismissed -> state.copy(
            overlay = DiagnosticsOverlayUiState.Closed,
        )
        DiagnosticsSettingsMutation.LogsCleared -> state.copy(
            logByteCount = 0,
            overlay = DiagnosticsOverlayUiState.Closed,
        )
    }
}
