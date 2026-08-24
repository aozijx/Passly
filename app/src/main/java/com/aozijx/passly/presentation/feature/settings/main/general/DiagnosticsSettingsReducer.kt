package com.aozijx.passly.presentation.feature.settings.main.general

internal sealed interface DiagnosticsSettingsMutation {
    data class FileLoggingChanged(val enabled: Boolean) : DiagnosticsSettingsMutation
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
        is DiagnosticsSettingsMutation.FileLoggingChanged -> state.copy(
            fileLoggingEnabled = mutation.enabled,
        )
        DiagnosticsSettingsMutation.ViewerOpened -> state.copy(
            isViewerOpen = true,
            logContent = null,
        )
        DiagnosticsSettingsMutation.ViewerClosed -> state.copy(
            isViewerOpen = false,
            logContent = null,
        )
        is DiagnosticsSettingsMutation.LogPageLoaded -> if (state.isViewerOpen) {
            state.copy(
                logContent = mutation.content,
                logByteCount = mutation.byteCount,
            )
        } else {
            state
        }
        DiagnosticsSettingsMutation.ClearRequested -> state.copy(
            isClearConfirmationOpen = true,
        )
        DiagnosticsSettingsMutation.ClearDismissed -> state.copy(
            isClearConfirmationOpen = false,
        )
        DiagnosticsSettingsMutation.LogsCleared -> state.copy(
            logContent = null,
            logByteCount = 0,
            isClearConfirmationOpen = false,
        )
    }
}
