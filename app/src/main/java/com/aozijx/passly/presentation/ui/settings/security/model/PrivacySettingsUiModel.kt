package com.aozijx.passly.presentation.ui.settings.security.model

data class PrivacySettingsUiModel(
    val isSecureContentEnabled: Boolean,
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean,
    val reauthenticateSensitiveCopies: Boolean,
    val clipboardClearEnabled: Boolean,
    val clipboardClearDelaySeconds: Int,
    val clipboardClearDelayOptions: List<Int>,
)
