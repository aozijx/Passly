package com.aozijx.passly.data.local.datastore

import com.aozijx.passly.data.crypto.proto.BiometricBinding
import com.aozijx.passly.data.crypto.proto.BiometricRotationJournal
import com.aozijx.passly.data.crypto.proto.BootstrapData
import com.aozijx.passly.domain.access.model.BiometricConfiguration
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricConfigurationMappingTest {
    @Test
    fun `prepared rotation cannot pretend to be a committed configuration`() {
        val data = BootstrapData.newBuilder().setBiometricRotation(
            BiometricRotationJournal.newBuilder().setCandidateAlias("candidate")
                .setPhase(BiometricRotationJournal.Phase.PREPARED),
        ).build()
        assertEquals(BiometricConfiguration.NotConfigured, readBiometricConfiguration(data))
    }

    @Test
    fun `projection uses committed policy regardless of rotation journal`() {
        for (policy in listOf(false, true)) {
            val data = BootstrapData.newBuilder().setBiometricBinding(
                BiometricBinding.newBuilder().setActiveAlias("active").setInvalidateOnEnrollment(policy),
            ).setBiometricRotation(
                BiometricRotationJournal.newBuilder().setCandidateAlias("candidate")
                    .setPhase(BiometricRotationJournal.Phase.PREPARED),
            ).build()
            assertEquals(BiometricConfiguration.Configured(policy), readBiometricConfiguration(data))
        }
    }

    @Test
    fun `empty binding is not a configured key`() {
        val data = BootstrapData.newBuilder().setBiometricBinding(BiometricBinding.getDefaultInstance()).build()
        assertEquals(BiometricConfiguration.NotConfigured, readBiometricConfiguration(data))
    }
}
