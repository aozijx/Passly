package com.aozijx.passly.feature.settings.presentation.security

import com.aozijx.passly.domain.settings.model.ClipboardClearPolicy
import com.aozijx.passly.feature.settings.presentation.ui.security.model.PrivacySettingsUiModel

fun PrivacySettingsUiState.Ready.toPrivacySettingsUiModel(): PrivacySettingsUiModel =
    PrivacySettingsUiModel(
        isSecureContentEnabled = isSecureContentEnabled,
        isFlipToLockEnabled = isFlipToLockEnabled,
        isFlipExitAndClearStackEnabled = isFlipExitAndClearStackEnabled,
        reauthenticateSensitiveCopies = reauthenticateSensitiveCopies,
        clipboardClearEnabled = clipboardClearEnabled,
        clipboardClearDelaySeconds = clipboardClearDelaySeconds,
        clipboardClearDelayOptions = ClipboardClearPolicy.ALLOWED_DELAY_SECONDS.toList(),
    )
