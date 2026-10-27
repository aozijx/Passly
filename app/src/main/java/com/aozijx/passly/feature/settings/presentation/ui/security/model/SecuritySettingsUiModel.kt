package com.aozijx.passly.feature.settings.presentation.ui.security.model

data class SecuritySettingsUiModel(
    val lockTimeoutMs: Long,
    val sliderMinSeconds: Float,
    val sliderMaxSeconds: Float,
    val sliderStepSeconds: Float,
    val isAppPasswordEnabled: Boolean,
    val isBiometricEnabled: Boolean,
    val isAuthenticationChangeInProgress: Boolean,
    val isInvalidateKeyOnBioChange: Boolean,
    val isLockOnBackground: Boolean,
)
