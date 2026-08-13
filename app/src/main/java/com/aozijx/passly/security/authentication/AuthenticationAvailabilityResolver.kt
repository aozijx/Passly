package com.aozijx.passly.security.authentication

import android.hardware.biometrics.BiometricManager
import com.aozijx.passly.domain.auth.model.envelope.EnvelopeType
import com.aozijx.passly.domain.authentication.AuthMethodAvailability
import com.aozijx.passly.security.envelope.BootstrapStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一的认证方式可用性解析器。
 *
 * 所有调用方（[DefaultAuthenticationManager]、[DefaultAuthenticationMethodProvisioner]）
 * 共用同一套判断逻辑，避免各处散落不一致的可用性检查。
 */
@Singleton
class AuthenticationAvailabilityResolver @Inject constructor(
    private val bootstrapStore: BootstrapStore,
    private val biometricManager: BiometricManager
) {
    suspend fun resolve(): AuthMethodAvailability = withContext(Dispatchers.Default) {
        val biometricState = bootstrapStore.loadBiometricState()
        AuthMethodAvailability(
            biometric = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BiometricManager.BIOMETRIC_SUCCESS &&
                    bootstrapStore.load(EnvelopeType.BIOMETRIC) != null &&
                    biometricState.binding != null,
            appPassword = bootstrapStore.load(EnvelopeType.APP_PASSWORD) != null,
            recoveryCode = bootstrapStore.load(EnvelopeType.RECOVERY) != null
        )
    }

    /** 是否存在除 [method] 之外的其他可用 primary factor。 */
    suspend fun hasAlternativePrimaryFactor(excluding: EnvelopeType): Boolean =
        resolve().let { availability ->
            when (excluding) {
                EnvelopeType.APP_PASSWORD -> availability.biometric
                EnvelopeType.BIOMETRIC -> availability.appPassword
                else -> false
            }
        }
}