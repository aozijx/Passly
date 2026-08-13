package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.security.KeyDerivation
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.security.authentication.host.AuthUiHost
import com.aozijx.passly.security.authentication.host.SecretHostResult
import com.aozijx.passly.security.envelope.BootstrapStore
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialMethodExecutor @Inject constructor(
    private val bootstrapStore: BootstrapStore,
    private val kdfRunner: KdfRunner,
    private val session: VaultSessionController,
    private val attemptLimiter: CredentialAttemptLimiter
) {
    internal suspend fun execute(
        request: AuthenticationRequest,
        method: AuthenticationMethod,
        host: AuthUiHost,
        credential: CharArray? = null
    ): MethodExecutionResult {
        val type = method.envelopeType()
        val envelope = bootstrapStore.load(type) ?: return failure(
            AuthenticationFailureCode.METHOD_UNAVAILABLE,
            request
        )
        attemptLimiter.beforeAttempt(method, request)?.let { limited ->
            return MethodExecutionResult.Failure(limited)
        }
        val input = credential
            ?.let { SecretHostResult.Submitted(SecretChars.copyOf(it)) }
            ?: host.requestSecret(request.purpose, method)
        return when (input) {
            is SecretHostResult.Cancelled -> MethodExecutionResult.Cancelled(input.byUser)
            SecretHostResult.HostUnavailable -> failure(AuthenticationFailureCode.HOST_UNAVAILABLE, request)
            is SecretHostResult.Submitted -> input.secret.use { secret ->
                try {
                    val ownedDek = kdfRunner.execute(secret) { workerSecret ->
                        val rawKey = KeyDerivation.deriveKeyBytesArgon2id(workerSecret, envelope.salt)
                        try {
                            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                            cipher.init(
                                Cipher.DECRYPT_MODE,
                                SecretKeySpec(rawKey, "AES"),
                                GCMParameterSpec(128, envelope.iv)
                            )
                            OwnedBytes(cipher.doFinal(envelope.ciphertext))
                        } finally {
                            rawKey.fill(0)
                        }
                    }
                    when (request.purpose) {
                        AuthenticationPurpose.UNLOCK_VAULT -> {
                            if (session.commitUnlock(type, ownedDek, request.id.value)) {
                                attemptLimiter.recordSuccess(method)
                                MethodExecutionResult.Success(method)
                            } else {
                                failure(AuthenticationFailureCode.ENVELOPE_CORRUPTED, request)
                            }
                        }

                        AuthenticationPurpose.RECOVER_AUTH_METHODS -> {
                            if (session.commitRecoveryUnlock(ownedDek, request.id.value)) {
                                attemptLimiter.recordSuccess(method)
                                MethodExecutionResult.Success(method)
                            } else {
                                failure(AuthenticationFailureCode.ENVELOPE_CORRUPTED, request)
                            }
                        }

                        AuthenticationPurpose.RECOVER_DATABASE -> {
                            if (session.stageDatabaseRecovery(type, ownedDek)) {
                                attemptLimiter.recordSuccess(method)
                                MethodExecutionResult.Success(method)
                            } else {
                                failure(AuthenticationFailureCode.ENVELOPE_CORRUPTED, request)
                            }
                        }

                        else -> {
                            ownedDek.discard()
                            attemptLimiter.recordSuccess(method)
                            MethodExecutionResult.Success(method)
                        }
                    }
                } catch (_: AEADBadTagException) {
                    MethodExecutionResult.Failure(attemptLimiter.recordIncorrect(method, request))
                } catch (_: GeneralSecurityException) {
                    failure(AuthenticationFailureCode.ENVELOPE_CORRUPTED, request)
                } catch (_: IllegalArgumentException) {
                    failure(AuthenticationFailureCode.ENVELOPE_CORRUPTED, request)
                }
            }
        }
    }

    private fun AuthenticationMethod.envelopeType(): EnvelopeType = when (this) {
        AuthenticationMethod.APP_PASSWORD -> EnvelopeType.APP_PASSWORD
        AuthenticationMethod.RECOVERY_CODE -> EnvelopeType.RECOVERY
        AuthenticationMethod.BIOMETRIC -> error("Biometric is not a credential KDF method")
    }

    private fun failure(
        code: AuthenticationFailureCode,
        request: AuthenticationRequest
    ) = MethodExecutionResult.Failure(AuthenticationFailure(code, request.id))
}
