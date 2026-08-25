package com.aozijx.passly.presentation.feature.settings.security

data class PrivacySettingsUiState(
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = false,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val reauthenticateSensitiveCopies: Boolean = true,
    val clipboardClearEnabled: Boolean = true,
    val clipboardClearDelaySeconds: Int = 30,
)

sealed interface PrivacySettingsAction {
    data class SetSecureContentEnabled(val enabled: Boolean) : PrivacySettingsAction
    data class SetFlipToLockEnabled(val enabled: Boolean) : PrivacySettingsAction
    data class SetFlipExitAndClearStackEnabled(val enabled: Boolean) : PrivacySettingsAction
    data class SetSensitiveCopyReauthentication(val enabled: Boolean) : PrivacySettingsAction
    data class SetClipboardClearEnabled(val enabled: Boolean) : PrivacySettingsAction
    data class SetClipboardClearDelaySeconds(val seconds: Int) : PrivacySettingsAction
    data object ClearClipboardNow : PrivacySettingsAction
}

sealed interface PrivacySettingsEffect {
    data object ClipboardCleared : PrivacySettingsEffect
    data object ClipboardNotCleared : PrivacySettingsEffect
}
