package com.aozijx.passly.domain.settings.model

data class SecuritySettings(
    val isSecureContentEnabled: Boolean,
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean,
    val isLockOnBackground: Boolean,
    val lockTimeout: Long,
    val reauthenticateSensitiveCopies: Boolean,
    val clipboardClearPolicy: ClipboardClearPolicy,
)
