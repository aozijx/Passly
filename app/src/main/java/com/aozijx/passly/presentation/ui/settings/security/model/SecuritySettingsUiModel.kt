package com.aozijx.passly.presentation.ui.settings.security.model

data class SecuritySettingsUiModel(
    val lockTimeoutMs: Long,
    val sliderMinSeconds: Float,
    val sliderMaxSeconds: Float,
    val sliderStepSeconds: Float,
    val isAppPasswordEnabled: Boolean,
    val isBiometricEnabled: Boolean,
    val isInvalidateKeyOnBioChange: Boolean,
    val isLockOnBackground: Boolean,
)
