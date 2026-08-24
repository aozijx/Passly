package com.aozijx.passly.presentation.feature.settings.security

data class PrivacySettingsUiState(
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = false,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val reauthenticateSensitiveCopies: Boolean = true,
)

sealed interface PrivacySettingsAction {
    data class SetSecureContentEnabled(val enabled: Boolean) : PrivacySettingsAction
    data class SetFlipToLockEnabled(val enabled: Boolean) : PrivacySettingsAction
    data class SetFlipExitAndClearStackEnabled(val enabled: Boolean) : PrivacySettingsAction
    data class SetSensitiveCopyReauthentication(val enabled: Boolean) : PrivacySettingsAction
}
