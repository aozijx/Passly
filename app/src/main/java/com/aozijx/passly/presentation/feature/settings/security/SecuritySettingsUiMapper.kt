package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.domain.settings.model.LockTimeoutConstraints
import com.aozijx.passly.presentation.ui.settings.security.model.SecuritySettingsUiModel

fun SecuritySettingsUiState.toSecuritySettingsUiModel(
    isAppPasswordEnabled: Boolean,
): SecuritySettingsUiModel = SecuritySettingsUiModel(
    lockTimeoutMs = lockTimeout,
    sliderMinSeconds = LockTimeoutConstraints.SLIDER_MIN_MS / 1000f,
    sliderMaxSeconds = LockTimeoutConstraints.MAX_MS / 1000f,
    sliderStepSeconds = LockTimeoutConstraints.SLIDER_STEP_MS / 1000f,
    isAppPasswordEnabled = isAppPasswordEnabled,
    isBiometricEnabled = isBiometricEnabled,
    isInvalidateKeyOnBioChange = isInvalidateKeyOnBioChange,
    isLockOnBackground = isLockOnBackground,
)
