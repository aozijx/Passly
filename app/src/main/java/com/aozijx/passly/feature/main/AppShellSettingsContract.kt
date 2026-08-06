package com.aozijx.passly.feature.main

data class AppShellSettingsUiState(
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = false,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val isStatusBarAutoHide: Boolean = true,
)
