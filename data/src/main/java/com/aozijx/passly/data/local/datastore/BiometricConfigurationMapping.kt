package com.aozijx.passly.data.local.datastore

import com.aozijx.passly.data.crypto.proto.BootstrapData
import com.aozijx.passly.domain.access.model.BiometricConfiguration

internal fun readBiometricConfiguration(data: BootstrapData): BiometricConfiguration =
    if (data.hasBiometricBinding() && data.biometricBinding.activeAlias.isNotBlank()) {
        BiometricConfiguration.Configured(data.biometricBinding.invalidateOnEnrollment)
    } else {
        BiometricConfiguration.NotConfigured
    }
