package com.aozijx.passly.feature.settings.presentation.security

sealed interface PrivacySettingsUiState {
    data object Loading : PrivacySettingsUiState

    data class Ready(
        val isSecureContentEnabled: Boolean,
        val isFlipToLockEnabled: Boolean,
        val isFlipExitAndClearStackEnabled: Boolean,
        val reauthenticateSensitiveCopies: Boolean,
        val clipboardClearEnabled: Boolean,
        val clipboardClearDelaySeconds: Int,
    ) : PrivacySettingsUiState
}

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
