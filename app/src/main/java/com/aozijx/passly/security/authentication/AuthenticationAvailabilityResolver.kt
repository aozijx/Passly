package com.aozijx.passly.security.authentication

import android.hardware.biometrics.BiometricManager
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
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
    private val vaultBootstrapStore: VaultBootstrapStore,
    private val biometricManager: BiometricManager
) {
    suspend fun resolve(): AuthenticationMethods = withContext(Dispatchers.Default) {
        val biometricState = vaultBootstrapStore.loadBiometricState()
        AuthenticationMethods(buildSet {
            if (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BiometricManager.BIOMETRIC_SUCCESS &&
                    vaultBootstrapStore.load(EnvelopeType.BIOMETRIC) != null &&
                    biometricState.binding != null
            ) add(AuthenticationMethod.BIOMETRIC)
            if (vaultBootstrapStore.load(EnvelopeType.APP_PASSWORD) != null) {
                add(AuthenticationMethod.APP_PASSWORD)
            }
            if (vaultBootstrapStore.load(EnvelopeType.RECOVERY) != null) {
                add(AuthenticationMethod.RECOVERY_CODE)
            }
        })
    }

    /** 是否存在除 [method] 之外的其他可用 primary factor。 */
    suspend fun hasAlternativePrimaryFactor(excluding: EnvelopeType): Boolean =
        resolve().let { availability ->
            when (excluding) {
                EnvelopeType.APP_PASSWORD -> AuthenticationMethod.BIOMETRIC in availability
                EnvelopeType.BIOMETRIC -> AuthenticationMethod.APP_PASSWORD in availability
                else -> false
            }
        }
}
