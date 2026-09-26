package com.aozijx.passly.feature.settings.security

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class SecurityAuthenticationChangeResult {
    APPLIED,
    NOT_APPLIED,
}

class SecurityAuthenticationSettingsInteractor @Inject constructor(
    authenticationMethodAvailability: AuthenticationMethodAvailability,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authenticationMethodProvisioner: AuthenticationMethodProvisioner,
    private val securitySettingsRepository: SecuritySettingsRepository,
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
        invalidateOnEnrollment: Boolean,
    ): SecurityAuthenticationChangeResult {
        if (secureSessionAccessState.isRecoveryMode()) {
            return SecurityAuthenticationChangeResult.NOT_APPLIED
        }
        val result = if (enabled) {
            authenticationMethodProvisioner.rotateBiometricPolicy(invalidateOnEnrollment)
        } else {
            authenticationMethodProvisioner.disableBiometric()
        }
        return result.toChangeResult()
    }

    suspend fun setKeyInvalidationPolicy(
        enabled: Boolean,
    ): SecurityAuthenticationChangeResult {
        if (secureSessionAccessState.isRecoveryMode()) {
            return SecurityAuthenticationChangeResult.NOT_APPLIED
        }
        val result = authenticationMethodProvisioner.rotateBiometricPolicy(enabled)
        if (result !is AuthenticationResult.Success) {
            return SecurityAuthenticationChangeResult.NOT_APPLIED
        }
        securitySettingsRepository.setInvalidateBiometricKeyOnChange(enabled)
        return SecurityAuthenticationChangeResult.APPLIED
    }
}

private fun AuthenticationResult.toChangeResult(): SecurityAuthenticationChangeResult =
    if (this is AuthenticationResult.Success) {
        SecurityAuthenticationChangeResult.APPLIED
    } else {
        SecurityAuthenticationChangeResult.NOT_APPLIED
    }
