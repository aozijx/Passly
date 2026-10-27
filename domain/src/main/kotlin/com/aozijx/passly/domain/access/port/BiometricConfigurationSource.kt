package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.BiometricConfiguration
import kotlinx.coroutines.flow.Flow

interface BiometricConfigurationSource {
    val biometricConfiguration: Flow<BiometricConfiguration>
}
