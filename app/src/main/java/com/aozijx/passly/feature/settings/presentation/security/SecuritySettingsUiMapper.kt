package com.aozijx.passly.feature.settings.presentation.security

import com.aozijx.passly.domain.settings.model.LockTimeoutConstraints
import com.aozijx.passly.feature.settings.presentation.ui.security.model.SecuritySettingsUiModel

fun SecuritySettingsUiState.Ready.toSecuritySettingsUiModel(
    isAppPasswordEnabled: Boolean,
): SecuritySettingsUiModel = SecuritySettingsUiModel(
    lockTimeoutMs = lockTimeout,
    sliderMinSeconds = LockTimeoutConstraints.SLIDER_MIN_MS / 1000f,
    sliderMaxSeconds = LockTimeoutConstraints.MAX_MS / 1000f,
    sliderStepSeconds = LockTimeoutConstraints.SLIDER_STEP_MS / 1000f,
    isAppPasswordEnabled = isAppPasswordEnabled,
    isBiometricEnabled = isBiometricEnabled,
    isAuthenticationChangeInProgress = isAuthenticationChangeInProgress,
    isInvalidateKeyOnBioChange = isInvalidateKeyOnBioChange,
    isLockOnBackground = isLockOnBackground,
)
