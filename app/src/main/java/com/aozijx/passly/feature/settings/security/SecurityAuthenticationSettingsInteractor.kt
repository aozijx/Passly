package com.aozijx.passly.feature.settings.security

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.BiometricConfigurationSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

class SecurityAuthenticationSettingsInteractor @Inject constructor(
    authenticationMethodAvailability: AuthenticationMethodAvailability,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authenticationMethodProvisioner: AuthenticationMethodProvisioner,
    private val configurationSource: BiometricConfigurationSource,
) {
    val isBiometricEnabled: Flow<Boolean> = authenticationMethodAvailability.methods
        .map { AuthenticationMethod.BIOMETRIC in it }
        .distinctUntilChanged()

    suspend fun hasRecoveryCode(): Boolean =
        authenticationMethodProvisioner.hasRecoveryCode()

    suspend fun verifyRecoveryCode(code: CharArray): Boolean = try {
        !secureSessionAccessState.isRecoveryMode() &&
            authenticationMethodProvisioner.checkRecoveryCode(code)
    } finally {
        code.fill('\u0000')
    }

    suspend fun setBiometricEnabled(
        enabled: Boolean,
    ): AuthenticationResult {
        if (secureSessionAccessState.isRecoveryMode()) {
            return AuthenticationResult.Failure(AuthenticationFailure(AuthenticationFailureCode.SESSION_MODE_RESTRICTED))
        }
        return if (enabled) {
            authenticationMethodProvisioner.rotateBiometricPolicy(
                configurationSource.biometricConfiguration.first().invalidateOnEnrollmentForNextKey,
            )
        } else {
            authenticationMethodProvisioner.disableBiometric()
        }
    }

    suspend fun setKeyInvalidationPolicy(
        enabled: Boolean,
    ): AuthenticationResult {
        if (secureSessionAccessState.isRecoveryMode()) {
            return AuthenticationResult.Failure(AuthenticationFailure(AuthenticationFailureCode.SESSION_MODE_RESTRICTED))
        }
        return authenticationMethodProvisioner.rotateBiometricPolicy(enabled)
    }
}
