package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.presentation.ui.settings.security.model.PrivacySettingsUiModel

fun PrivacySettingsUiState.toPrivacySettingsUiModel(): PrivacySettingsUiModel =
    PrivacySettingsUiModel(
        isSecureContentEnabled = isSecureContentEnabled,
        isFlipToLockEnabled = isFlipToLockEnabled,
        isFlipExitAndClearStackEnabled = isFlipExitAndClearStackEnabled,
        reauthenticateSensitiveCopies = reauthenticateSensitiveCopies,
    )
