package com.aozijx.passly.feature.settings.presentation.security

import com.aozijx.passly.feature.settings.presentation.ui.security.model.PrivacySettingsUiModel
import com.aozijx.passly.domain.settings.model.ClipboardClearPolicy

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
