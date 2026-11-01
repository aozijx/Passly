package com.aozijx.passly.data.local.datastore

import com.aozijx.passly.data.crypto.proto.BiometricBinding
import com.aozijx.passly.data.crypto.proto.BootstrapData
import com.aozijx.passly.data.crypto.proto.EnvelopeEntry
import com.aozijx.passly.domain.access.model.AuthenticationCredentialSnapshot
import com.aozijx.passly.domain.access.model.EnvelopeType
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticationCredentialSnapshotMappingTest {
    @Test
    fun `snapshot only exposes authentication methods present in the same bootstrap value`() {
        val data = BootstrapData.newBuilder()
            .addEnvelopes(envelope(EnvelopeType.BIOMETRIC))
            .addEnvelopes(envelope(EnvelopeType.APP_PASSWORD))
            .addEnvelopes(envelope(EnvelopeType.RECOVERY))
            .setBiometricBinding(BiometricBinding.newBuilder().setActiveAlias("active"))
            .build()

        assertEquals(
            AuthenticationCredentialSnapshot(
                biometricConfigured = true,
                appPasswordConfigured = true,
                recoveryCodeConfigured = true,
            ),
            data.readAuthenticationCredentialSnapshot(),
        )
    }

    @Test
    fun `biometric requires both envelope and nonblank binding in the snapshot`() {
        val withEnvelopeOnly = BootstrapData.newBuilder()
            .addEnvelopes(envelope(EnvelopeType.BIOMETRIC))
            .build()
        val withBindingOnly = BootstrapData.newBuilder()
            .setBiometricBinding(BiometricBinding.newBuilder().setActiveAlias("active"))
            .build()
        val withBlankBinding = BootstrapData.newBuilder()
            .addEnvelopes(envelope(EnvelopeType.BIOMETRIC))
            .setBiometricBinding(BiometricBinding.getDefaultInstance())
            .build()

        listOf(withEnvelopeOnly, withBindingOnly, withBlankBinding).forEach { data ->
            assertEquals(
                AuthenticationCredentialSnapshot(),
                data.readAuthenticationCredentialSnapshot(),
            )
        }
    }

    private fun envelope(type: EnvelopeType) = EnvelopeEntry.newBuilder().setType(type.value).build()
}
