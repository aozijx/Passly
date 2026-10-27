package com.aozijx.passly.domain.settings.model

data class SecuritySettings(
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = true,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val isLockOnBackground: Boolean = false,
    val lockTimeout: Long = 60000L,
    val reauthenticateSensitiveCopies: Boolean = true,
    val clipboardClearPolicy: ClipboardClearPolicy = ClipboardClearPolicy(),
)
