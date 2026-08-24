package com.aozijx.passly.presentation.feature.settings.main.general

data class DiagnosticsSettingsUiState(
    val fileLoggingEnabled: Boolean = false,
    val isViewerOpen: Boolean = false,
    val logContent: String? = null,
    val logByteCount: Int = 0,
    val isClearConfirmationOpen: Boolean = false,
)

sealed interface DiagnosticsSettingsAction {
    data class SetFileLoggingEnabled(val enabled: Boolean) : DiagnosticsSettingsAction
    data object OpenViewer : DiagnosticsSettingsAction
    data object CloseViewer : DiagnosticsSettingsAction
    data object RequestClear : DiagnosticsSettingsAction
    data object DismissClear : DiagnosticsSettingsAction
    data object ConfirmClear : DiagnosticsSettingsAction
    data object Export : DiagnosticsSettingsAction
}

sealed interface DiagnosticsSettingsEffect {
    data object ExportFailed : DiagnosticsSettingsEffect
}
