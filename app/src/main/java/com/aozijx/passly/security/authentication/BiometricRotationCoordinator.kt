package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.model.envelope.EnvelopeType
import com.aozijx.passly.security.authentication.host.AuthUiHost
import com.aozijx.passly.security.authentication.host.BiometricHostResult
import com.aozijx.passly.security.authentication.host.BiometricHostFailure
import com.aozijx.passly.security.authentication.host.BiometricPromptSpec
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.EnvelopeCrypto
import com.aozijx.passly.security.envelope.BiometricBinding
import com.aozijx.passly.security.envelope.BiometricRotationJournal
import com.aozijx.passly.security.envelope.BiometricRotationPhase
import com.aozijx.passly.security.envelope.BootstrapStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricRotationCoordinator @Inject constructor(
    private val bootstrapStore: BootstrapStore,
    private val cryptoFactory: BiometricCryptoFactory,
    private val dekManager: DekManager
) {
    suspend fun rotate(
        host: AuthUiHost,
        invalidateOnEnrollment: Boolean,
        correlationId: String
    ): AuthenticationResult {
        if (!dekManager.isUnlocked.value) {
            return fail(AuthenticationFailureCode.SESSION_TRANSITION_FAILED, correlationId)
        }
        val oldAlias = bootstrapStore.loadBiometricState().binding?.activeAlias
        val candidateAlias = "${cryptoFactory.baseAlias()}.candidate.${UUID.randomUUID()}"
        bootstrapStore.prepareBiometricRotation(
            BiometricRotationJournal(
                phase = BiometricRotationPhase.PREPARED,
                oldAlias = oldAlias,
                candidateAlias = candidateAlias
            )
        )
        val preparation = cryptoFactory.createEncrypt(candidateAlias, invalidateOnEnrollment)
        val cipher = (preparation as? BiometricCryptoPreparation.Ready)?.cipher
            ?: return fail(preparation.failureCode(), correlationId).also {
                rollbackCandidate(candidateAlias)
            }
        return when (
            val prompt = host.authenticateBiometric(
                BiometricPromptSpec(AuthenticationPurpose.CHANGE_BIOMETRIC_POLICY),
                androidx.biometric.BiometricPrompt.CryptoObject(cipher)
            )
        ) {
            is BiometricHostResult.Success -> {
                val envelope = dekManager.withDek { dek ->
                    EnvelopeCrypto.wrapWithCipher(EnvelopeType.BIOMETRIC, dek, cipher)
                }
                bootstrapStore.commitBiometricRotation(
                    envelope = envelope,
                    binding = BiometricBinding(candidateAlias, invalidateOnEnrollment),
                    obsoleteAlias = oldAlias?.takeIf { it != candidateAlias }
                )
                if (oldAlias != null &&
                    oldAlias != candidateAlias &&
                    cryptoFactory.deleteAlias(oldAlias)
                ) {
                    bootstrapStore.clearBiometricCleanupAlias(oldAlias)
                }
                bootstrapStore.clearBiometricRotationJournal()
                AuthenticationResult.Success(com.aozijx.passly.domain.authentication.AuthenticationMethod.BIOMETRIC)
            }
            is BiometricHostResult.Cancelled -> {
                rollbackCandidate(candidateAlias)
                AuthenticationResult.Cancelled(prompt.byUser)
            }
            is BiometricHostResult.Failure -> fail(prompt.reason.failureCode(), correlationId).also {
                rollbackCandidate(candidateAlias)
            }
            BiometricHostResult.HostUnavailable -> {
                rollbackCandidate(candidateAlias)
                fail(AuthenticationFailureCode.HOST_UNAVAILABLE, correlationId)
            }
        }
    }

    private suspend fun rollbackCandidate(candidateAlias: String) {
        if (cryptoFactory.deleteAlias(candidateAlias)) {
            bootstrapStore.clearBiometricRotationJournal()
        }
    }

    private fun BiometricCryptoPreparation.failureCode(): AuthenticationFailureCode = when (this) {
        BiometricCryptoPreparation.KeyMissing -> AuthenticationFailureCode.KEY_MISSING
        BiometricCryptoPreparation.KeyInvalidated -> AuthenticationFailureCode.KEY_INVALIDATED
        else -> AuthenticationFailureCode.CRYPTO_OBJECT_INVALID
    }

    private fun BiometricHostFailure.failureCode(): AuthenticationFailureCode = when (this) {
        BiometricHostFailure.METHOD_UNAVAILABLE -> AuthenticationFailureCode.METHOD_UNAVAILABLE
        BiometricHostFailure.RATE_LIMITED -> AuthenticationFailureCode.RATE_LIMITED
        BiometricHostFailure.CRYPTO_OBJECT_INVALID,
        BiometricHostFailure.AUTHENTICATION_FAILED -> AuthenticationFailureCode.CRYPTO_OBJECT_INVALID
    }

    private fun fail(code: AuthenticationFailureCode, correlationId: String) =
        AuthenticationResult.Failure(
            AuthenticationFailure(
                code,
                correlationId,
                safeFields = mapOf(
                    "method" to com.aozijx.passly.domain.authentication.AuthenticationMethod.BIOMETRIC.name
                )
            )
        )
}
