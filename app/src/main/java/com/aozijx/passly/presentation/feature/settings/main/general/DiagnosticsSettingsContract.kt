package com.aozijx.passly.presentation.feature.settings.main.general

data class DiagnosticsSettingsUiState(
    val logByteCount: Int = 0,
    val overlay: DiagnosticsOverlayUiState = DiagnosticsOverlayUiState.Closed,
)

sealed interface DiagnosticsOverlayUiState {
    data object Closed : DiagnosticsOverlayUiState
    data class Viewer(val page: DiagnosticsLogPageUiState) : DiagnosticsOverlayUiState
    data object ClearConfirmation : DiagnosticsOverlayUiState
}

sealed interface DiagnosticsLogPageUiState {
    data object Loading : DiagnosticsLogPageUiState
    data class Content(val text: String) : DiagnosticsLogPageUiState
}

sealed interface DiagnosticsSettingsAction {
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
