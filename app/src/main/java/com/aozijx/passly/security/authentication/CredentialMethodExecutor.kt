package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.security.KeyDerivation
import com.aozijx.passly.domain.auth.model.envelope.EnvelopeType
import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
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
    private val session: VaultSessionController
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
                    if (request.purpose == AuthenticationPurpose.UNLOCK_VAULT) {
                        if (session.commitUnlock(type, ownedDek, request.correlationId)) {
                            MethodExecutionResult.Success(method)
                        } else {
                            failure(AuthenticationFailureCode.ENVELOPE_CORRUPTED, request)
                        }
                    } else {
                        ownedDek.discard()
                        MethodExecutionResult.Success(method)
                    }
                } catch (_: AEADBadTagException) {
                    failure(AuthenticationFailureCode.CREDENTIAL_INCORRECT, request)
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
    ) = MethodExecutionResult.Failure(AuthenticationFailure(code, request.correlationId))
}
