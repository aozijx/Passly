package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.presentation.ui.settings.security.model.PrivacySettingsUiModel
import com.aozijx.passly.domain.settings.model.ClipboardClearPolicy

fun PrivacySettingsUiState.toPrivacySettingsUiModel(): PrivacySettingsUiModel =
    PrivacySettingsUiModel(
        isSecureContentEnabled = isSecureContentEnabled,
        isFlipToLockEnabled = isFlipToLockEnabled,
        isFlipExitAndClearStackEnabled = isFlipExitAndClearStackEnabled,
        reauthenticateSensitiveCopies = reauthenticateSensitiveCopies,
        clipboardClearEnabled = clipboardClearEnabled,
        clipboardClearDelaySeconds = clipboardClearDelaySeconds,
        clipboardClearDelayOptions = ClipboardClearPolicy.ALLOWED_DELAY_SECONDS.toList(),
    )
