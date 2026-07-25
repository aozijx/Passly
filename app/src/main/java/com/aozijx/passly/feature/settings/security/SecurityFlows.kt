package com.aozijx.passly.feature.settings.security

internal fun handleInvalidateKeyToggle(
    enabled: Boolean,
    switchPolicy: (Boolean, (Boolean) -> Unit) -> Unit
) {
    switchPolicy(enabled) { }
}

internal fun handleBiometricToggle(
    enabled: Boolean,
    setEnabled: (Boolean, (Boolean) -> Unit) -> Unit
) {
    setEnabled(enabled) { }
}
