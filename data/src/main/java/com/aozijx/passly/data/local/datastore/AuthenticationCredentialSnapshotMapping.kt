package com.aozijx.passly.data.local.datastore

import com.aozijx.passly.data.crypto.proto.BootstrapData
import com.aozijx.passly.domain.access.model.AuthenticationCredentialSnapshot
import com.aozijx.passly.domain.access.model.EnvelopeType

internal fun BootstrapData.readAuthenticationCredentialSnapshot(): AuthenticationCredentialSnapshot {
    val envelopeTypes = envelopesList.mapTo(hashSetOf()) { it.type }
    return AuthenticationCredentialSnapshot(
        biometricConfigured =
            hasBiometricBinding() && biometricBinding.activeAlias.isNotBlank() &&
                EnvelopeType.BIOMETRIC.value in envelopeTypes,
        appPasswordConfigured = EnvelopeType.APP_PASSWORD.value in envelopeTypes,
        recoveryCodeConfigured = EnvelopeType.RECOVERY.value in envelopeTypes,
    )
}
