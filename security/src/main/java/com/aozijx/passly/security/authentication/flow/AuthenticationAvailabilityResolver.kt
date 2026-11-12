package com.aozijx.passly.security.authentication.flow
import android.hardware.biometrics.BiometricManager
import com.aozijx.passly.domain.access.model.AuthenticationCredentialSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 统一的认证方式可用性解析器。
 *
 * 所有调用方（[DefaultAuthenticationManager]、[DefaultAuthenticationMethodProvisioner]）
 * 共用同一套判断逻辑，避免各处散落不一致的可用性检查。
 */
@Singleton
internal interface AuthenticationAvailabilityResolverPort {
    suspend fun resolve(): AuthenticationMethods

    suspend fun hasAlternativePrimaryFactor(excluding: EnvelopeType): Boolean
}

internal class AuthenticationAvailabilityResolver
    @Inject
    constructor(
        private val vaultBootstrapStore: VaultBootstrapStore,
        private val biometricManager: BiometricManager,
    ) : AuthenticationAvailabilityResolverPort {
        override suspend fun resolve(): AuthenticationMethods =
            withContext(Dispatchers.Default) {
                val credentials = vaultBootstrapStore.loadAuthenticationCredentialSnapshot()
                credentials.toAuthenticationMethods(
                    biometricAvailable =
                        biometricManager.canAuthenticate(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG,
                        ) == BiometricManager.BIOMETRIC_SUCCESS,
                )
            }

        /** 是否存在除 [method] 之外的其他可用 primary factor。 */
        override suspend fun hasAlternativePrimaryFactor(excluding: EnvelopeType): Boolean =
            resolve().let { availability ->
                when (excluding) {
                    EnvelopeType.APP_PASSWORD -> AuthenticationMethod.BIOMETRIC in availability
                    EnvelopeType.BIOMETRIC -> AuthenticationMethod.APP_PASSWORD in availability
                    else -> false
                }
            }
    }

internal fun AuthenticationCredentialSnapshot.toAuthenticationMethods(biometricAvailable: Boolean): AuthenticationMethods =
    AuthenticationMethods(
        buildSet {
            if (biometricAvailable && biometricConfigured) add(AuthenticationMethod.BIOMETRIC)
            if (appPasswordConfigured) add(AuthenticationMethod.APP_PASSWORD)
            if (recoveryCodeConfigured) add(AuthenticationMethod.RECOVERY_CODE)
        },
    )
