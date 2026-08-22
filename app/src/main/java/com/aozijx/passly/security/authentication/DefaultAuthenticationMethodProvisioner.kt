package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KdfAlgorithm
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.CancellationException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthenticationMethodProvisioner @Inject constructor(
    private val kdfRunner: KdfRunner,
    private val dekManager: DekManager,
    private val session: VaultSessionController,
    private val authenticationManager: AuthenticationManager,
    private val vaultBootstrapStore: VaultBootstrapStore,
    private val hostRegistry: com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry,
    private val rotateBiometricKey: RotateBiometricKeyUseCase,
    private val cryptoFactory: BiometricCryptoFactory,
    private val availabilityResolver: AuthenticationAvailabilityResolver
) : AuthenticationMethodProvisioner {
    override suspend fun setAppPassword(password: CharArray): AuthenticationResult {
        val correlationId = UuidCreator.getTimeOrderedEpoch().toString()
        val wasRecoveryMode = session.isRecoveryMode()
        if (!AppPasswordPolicy.DEFAULT.acceptsLength(password.size)) {
            password.fill('\u0000')
            return AuthenticationResult.Failure(
                AuthenticationFailure(
                    AuthenticationFailureCode.PASSWORD_POLICY_VIOLATION,
                    AuthenticationRequestId(correlationId)
                )
            )
        }
        val secret = SecretChars.take(password)
        val salt = KeyDerivation.generateSalt()
        return try {
            val ownedKey = kdfRunner.execute(secret) { chars ->
                OwnedBytes(KeyDerivation.deriveKeyBytesArgon2id(chars, salt))
            }
            val rawKey = ownedKey.consume()
            try {
                val key = SecretKeySpec(rawKey, "AES")
                if (dekManager.isUnlocked.value) {
                    dekManager.rekeyWithKey(
                        EnvelopeType.APP_PASSWORD,
                        key,
                        salt,
                        KdfAlgorithm.ARGON2ID
                    )
                } else {
                    dekManager.initializeWithKey(
                        EnvelopeType.APP_PASSWORD,
                        key,
                        salt,
                        KdfAlgorithm.ARGON2ID
                    )
                }
                if (wasRecoveryMode) {
                    finishRecoveryPasswordProvisioning(
                        seal = { session.lock(LockReason.RECOVERY_EXIT) },
                        refreshAvailability = authenticationManager::refreshAvailability
                    )
                } else {
                    authenticationManager.refreshAvailability()
                    session.markAuthenticated()
                }
                AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
            } finally {
                rawKey.fill(0)
                ownedKey.discard()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            AuthenticationResult.Failure(
                AuthenticationFailure(
                    AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
                    AuthenticationRequestId(correlationId),
                )
            )
        } finally {
            secret.close()
        }
    }

    override suspend fun changeAppPassword(
        currentPassword: CharArray,
        newPassword: CharArray
    ): AuthenticationResult {
        val authentication = try {
            try {
                authenticationManager.authenticate(
                    AuthenticationRequest(
                        purpose = AuthenticationPurpose.MANAGE_APP_PASSWORD,
                        allowedMethods = setOf(AuthenticationMethod.APP_PASSWORD)
                    ),
                    AuthInput.AppPassword.from(currentPassword)
                )
            } catch (error: Throwable) {
                newPassword.fill('\u0000')
                throw error
            }
        } finally {
            currentPassword.fill('\u0000')
        }
        if (authentication !is AuthenticationResult.Success) {
            newPassword.fill('\u0000')
            return authentication
        }
        return setAppPassword(newPassword)
    }

    override suspend fun disableAppPassword(): AuthenticationResult {
        val correlationId = UuidCreator.getTimeOrderedEpoch().toString()
        val authentication = authenticationManager.authenticate(
            AuthenticationRequest(
                purpose = AuthenticationPurpose.MANAGE_APP_PASSWORD,
                allowedMethods = setOf(AuthenticationMethod.APP_PASSWORD)
            )
        )
        if (authentication !is AuthenticationResult.Success) return authentication
        // Rely on the resolver for a consistent primary-factor availability check.
        if (!availabilityResolver.hasAlternativePrimaryFactor(EnvelopeType.APP_PASSWORD)) {
            return AuthenticationResult.Failure(
                AuthenticationFailure(
                    AuthenticationFailureCode.LAST_METHOD_REQUIRED,
                    AuthenticationRequestId(correlationId),
                )
            )
        }
        vaultBootstrapStore.delete(EnvelopeType.APP_PASSWORD)
        authenticationManager.refreshAvailability()
        return AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
    }

    override suspend fun disableBiometric(): AuthenticationResult {
        val correlationId = UuidCreator.getTimeOrderedEpoch().toString()
        val authentication = authenticationManager.authenticate(
            AuthenticationRequest(AuthenticationPurpose.CHANGE_BIOMETRIC_POLICY)
        )
        if (authentication !is AuthenticationResult.Success) return authentication
        // Rely on the resolver for a consistent primary-factor availability check.
        if (!availabilityResolver.hasAlternativePrimaryFactor(EnvelopeType.BIOMETRIC)) {
            return AuthenticationResult.Failure(
                AuthenticationFailure(
                    AuthenticationFailureCode.LAST_METHOD_REQUIRED,
                    AuthenticationRequestId(correlationId),
                )
            )
        }
        val activeAlias = vaultBootstrapStore.loadBiometricState().binding?.activeAlias
            ?: return AuthenticationResult.Failure(
                AuthenticationFailure(AuthenticationFailureCode.KEY_MISSING, AuthenticationRequestId(correlationId))
            )
        vaultBootstrapStore.disableBiometric(activeAlias)
        if (cryptoFactory.deleteAlias(activeAlias)) {
            vaultBootstrapStore.clearBiometricCleanupAlias(activeAlias)
        }
        authenticationManager.refreshAvailability()
        return AuthenticationResult.Success(AuthenticationMethod.BIOMETRIC)
    }

    override suspend fun rotateBiometricPolicy(
        invalidateOnEnrollment: Boolean
    ): AuthenticationResult {
        val correlationId = UuidCreator.getTimeOrderedEpoch().toString()
        if (!session.isRecoveryMode()) {
            val authentication = authenticationManager.authenticate(
                AuthenticationRequest(AuthenticationPurpose.CHANGE_BIOMETRIC_POLICY)
            )
            if (authentication !is AuthenticationResult.Success) return authentication
        }
        val host = hostRegistry.awaitLease()?.hostOrNull()
            ?: return AuthenticationResult.Failure(
                AuthenticationFailure(
                    AuthenticationFailureCode.HOST_UNAVAILABLE,
                    AuthenticationRequestId(correlationId),
                )
            )
        val result = rotateBiometricKey.rotate(
            host = host,
            invalidateOnEnrollment = invalidateOnEnrollment,
            correlationId = correlationId
        )
        if (result is AuthenticationResult.Success) authenticationManager.refreshAvailability()
        return result
    }

    override suspend fun hasRecoveryCode(): Boolean =
        vaultBootstrapStore.load(EnvelopeType.RECOVERY) != null

    override suspend fun checkRecoveryCode(code: CharArray): Boolean {
        val envelope = vaultBootstrapStore.load(EnvelopeType.RECOVERY) ?: run {
            code.fill('\u0000')
            return false
        }
        val secret = SecretChars.take(code)
        return try {
            val ownedKey = kdfRunner.execute(secret) { chars ->
                OwnedBytes(KeyDerivation.deriveKeyBytesArgon2id(chars, envelope.salt))
            }
            val rawKey = ownedKey.consume()
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(rawKey, "AES"),
                    GCMParameterSpec(128, envelope.iv)
                )
                val dek = cipher.doFinal(envelope.ciphertext)
                dek.fill(0)
                true
            } catch (_: Exception) {
                false
            } finally {
                rawKey.fill(0)
                ownedKey.discard()
            }
        } finally {
            secret.close()
        }
    }
}
