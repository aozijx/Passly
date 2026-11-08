package com.aozijx.passly.feature.settings.presentation.security

import com.aozijx.passly.presentation.shared.error.UiErrorMessage

sealed interface PrivacySettingsAction {
    data object RetryRead : PrivacySettingsAction

    data class SetSecureContentEnabled(
        val enabled: Boolean,
    ) : PrivacySettingsAction

    data class SetFlipToLockEnabled(
        val enabled: Boolean,
    ) : PrivacySettingsAction

    data class SetFlipExitAndClearStackEnabled(
        val enabled: Boolean,
    ) : PrivacySettingsAction

    data class SetSensitiveCopyReauthentication(
        val enabled: Boolean,
    ) : PrivacySettingsAction

    data class SetClipboardClearEnabled(
        val enabled: Boolean,
    ) : PrivacySettingsAction

    data class SetClipboardClearDelaySeconds(
        val seconds: Int,
    ) : PrivacySettingsAction

    data object ClearClipboardNow : PrivacySettingsAction
}

sealed interface PrivacySettingsEffect {
    data class SaveFailed(
        val message: UiErrorMessage,
    ) : PrivacySettingsEffect

    data object ClipboardCleared : PrivacySettingsEffect

    data object ClipboardNotCleared : PrivacySettingsEffect
}
