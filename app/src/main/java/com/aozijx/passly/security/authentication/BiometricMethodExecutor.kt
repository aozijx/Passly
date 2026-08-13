package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.security.authentication.host.AuthUiHost
import com.aozijx.passly.security.authentication.host.BiometricHostFailure
import com.aozijx.passly.security.authentication.host.BiometricHostResult
import com.aozijx.passly.security.authentication.host.BiometricPromptSpec
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.DekUnlockError
import com.aozijx.passly.security.dek.DekUnlockResult
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricMethodExecutor @Inject constructor(
    private val vaultBootstrapStore: VaultBootstrapStore,
    private val cryptoFactory: BiometricCryptoFactory,
    private val dekManager: DekManager,
    private val session: VaultSessionController
) {
    internal suspend fun execute(
        request: AuthenticationRequest,
        host: AuthUiHost
    ): MethodExecutionResult {
        val requiresDek = request.purpose == AuthenticationPurpose.UNLOCK_VAULT ||
            request.purpose == AuthenticationPurpose.RECOVER_DATABASE
        if (!requiresDek) return hostResult(request, host)
        val envelope = vaultBootstrapStore.load(EnvelopeType.BIOMETRIC)
            ?: return failure(AuthenticationFailureCode.METHOD_UNAVAILABLE, request)
        val alias = vaultBootstrapStore.loadBiometricState().binding?.activeAlias
            ?: return failure(AuthenticationFailureCode.KEY_MISSING, request)
        val preparation = cryptoFactory.createDecrypt(alias, envelope.iv)
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
            is BiometricHostResult.Success -> {
                val unlockResult = if (dekManager.isUnlocked.value) {
                    // SOFT_LOCKED: DEK 已在内存中，无需重新解密
                    DekUnlockResult.Success
                } else {
                    dekManager.unlock(EnvelopeType.BIOMETRIC, cipher)
                }
                when (unlockResult) {
                    DekUnlockResult.Success -> {
                        if (request.purpose == AuthenticationPurpose.RECOVER_DATABASE) {
                            MethodExecutionResult.Success(AuthenticationMethod.BIOMETRIC)
                        } else if (session.markAuthenticated()) {
                            MethodExecutionResult.Success(AuthenticationMethod.BIOMETRIC)
                        } else {
                            failure(AuthenticationFailureCode.SESSION_TRANSITION_FAILED, request)
                        }
                    }

                    is DekUnlockResult.Failed -> failure(unlockResult.reason.failureCode(), request)
                }
            }
            is BiometricHostResult.Cancelled -> MethodExecutionResult.Cancelled(hostResult.byUser)
            is BiometricHostResult.Failure -> failure(hostResult.reason.failureCode(), request)
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
        is BiometricHostResult.Failure -> failure(result.reason.failureCode(), request)
        BiometricHostResult.HostUnavailable -> failure(AuthenticationFailureCode.HOST_UNAVAILABLE, request)
    }

    private fun BiometricHostFailure.failureCode(): AuthenticationFailureCode = when (this) {
        BiometricHostFailure.METHOD_UNAVAILABLE -> AuthenticationFailureCode.METHOD_UNAVAILABLE
        BiometricHostFailure.RATE_LIMITED -> AuthenticationFailureCode.RATE_LIMITED
        BiometricHostFailure.CRYPTO_OBJECT_INVALID -> AuthenticationFailureCode.CRYPTO_OBJECT_INVALID
        BiometricHostFailure.AUTHENTICATION_FAILED -> AuthenticationFailureCode.CREDENTIAL_INCORRECT
    }

    private fun DekUnlockError.failureCode(): AuthenticationFailureCode = when (this) {
        DekUnlockError.AUTH_FAILED -> AuthenticationFailureCode.CREDENTIAL_INCORRECT
        DekUnlockError.DEK_VERIFY_FAILED, DekUnlockError.ENVELOPE_CORRUPTED -> AuthenticationFailureCode.ENVELOPE_CORRUPTED
        else -> AuthenticationFailureCode.SESSION_TRANSITION_FAILED
    }

    private fun failure(code: AuthenticationFailureCode, request: AuthenticationRequest) =
        MethodExecutionResult.Failure(
            AuthenticationFailure(
                code,
                request.id
            )
        )
}
