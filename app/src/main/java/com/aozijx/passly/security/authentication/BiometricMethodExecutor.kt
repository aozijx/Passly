package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.model.envelope.EnvelopeType
import com.aozijx.passly.security.authentication.host.AuthUiHost
import com.aozijx.passly.security.authentication.host.BiometricHostResult
import com.aozijx.passly.security.authentication.host.BiometricPromptSpec
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.UnlockError
import com.aozijx.passly.security.crypto.UnlockResult
import com.aozijx.passly.security.envelope.BootstrapStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricMethodExecutor @Inject constructor(
    private val bootstrapStore: BootstrapStore,
    private val cryptoFactory: BiometricCryptoFactory,
    private val dekManager: DekManager,
    private val session: VaultSessionController
) {
    internal suspend fun execute(
        request: AuthenticationRequest,
        host: AuthUiHost
    ): MethodExecutionResult {
        if (request.purpose != AuthenticationPurpose.UNLOCK_VAULT) {
            return hostResult(request, host)
        }
        val envelope = bootstrapStore.load(EnvelopeType.BIOMETRIC)
            ?: return failure(AuthenticationFailureCode.METHOD_UNAVAILABLE, request)
        val preparation = cryptoFactory.createDecrypt(envelope.iv)
        val cipher = when (preparation) {
            is BiometricCryptoPreparation.Ready -> preparation.cipher
            BiometricCryptoPreparation.KeyMissing -> return failure(AuthenticationFailureCode.KEY_MISSING, request)
            BiometricCryptoPreparation.KeyInvalidated -> return failure(AuthenticationFailureCode.KEY_INVALIDATED, request)
            BiometricCryptoPreparation.Invalid -> return failure(AuthenticationFailureCode.CRYPTO_OBJECT_INVALID, request)
        }
        val hostResult = host.authenticateBiometric(
            BiometricPromptSpec(request.purpose),
            androidx.biometric.BiometricPrompt.CryptoObject(cipher)
        )
        return when (hostResult) {
            is BiometricHostResult.Success -> when (val unlock = dekManager.unlock(EnvelopeType.BIOMETRIC, cipher)) {
                UnlockResult.Success -> {
                    session.markAuthenticated()
                    MethodExecutionResult.Success(AuthenticationMethod.BIOMETRIC)
                }
                is UnlockResult.Failed -> failure(unlock.reason.failureCode(), request)
            }
            is BiometricHostResult.Cancelled -> MethodExecutionResult.Cancelled(hostResult.byUser)
            is BiometricHostResult.Error -> failure(AuthenticationFailureCode.CREDENTIAL_INCORRECT, request)
            BiometricHostResult.HostUnavailable -> failure(AuthenticationFailureCode.HOST_UNAVAILABLE, request)
        }
    }

    private suspend fun hostResult(
        request: AuthenticationRequest,
        host: AuthUiHost
    ): MethodExecutionResult = when (
        val result = host.authenticateBiometric(BiometricPromptSpec(request.purpose), null)
    ) {
        is BiometricHostResult.Success -> MethodExecutionResult.Success(AuthenticationMethod.BIOMETRIC)
        is BiometricHostResult.Cancelled -> MethodExecutionResult.Cancelled(result.byUser)
        is BiometricHostResult.Error -> failure(AuthenticationFailureCode.CREDENTIAL_INCORRECT, request)
        BiometricHostResult.HostUnavailable -> failure(AuthenticationFailureCode.HOST_UNAVAILABLE, request)
    }

    private fun UnlockError.failureCode(): AuthenticationFailureCode = when (this) {
        UnlockError.AUTH_FAILED -> AuthenticationFailureCode.CREDENTIAL_INCORRECT
        UnlockError.DEK_VERIFY_FAILED, UnlockError.ENVELOPE_CORRUPTED -> AuthenticationFailureCode.ENVELOPE_CORRUPTED
        else -> AuthenticationFailureCode.SESSION_TRANSITION_FAILED
    }

    private fun failure(code: AuthenticationFailureCode, request: AuthenticationRequest) =
        MethodExecutionResult.Failure(AuthenticationFailure(code, request.correlationId))
}
