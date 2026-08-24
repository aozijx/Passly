package com.aozijx.passly.presentation.feature.shell

data class AppShellSettingsUiState(
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = false,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val isStatusBarAutoHide: Boolean = true,
)
