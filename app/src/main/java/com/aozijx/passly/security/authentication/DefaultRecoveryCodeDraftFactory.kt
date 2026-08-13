package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.crypto.KeyDerivation
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KdfAlgorithm
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.RecoveryCredentialDraft
import com.aozijx.passly.domain.access.model.RecoveryCredentialCreation
import com.aozijx.passly.domain.access.model.RecoveryCredentialFactory
import com.aozijx.passly.domain.access.model.RecoveryCredentialId
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.envelope.EnvelopeManager
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultRecoveryCodeDraftFactory @Inject constructor(
    private val kdfRunner: KdfRunner,
    private val dekManager: DekManager,
    private val vaultBootstrapStore: VaultBootstrapStore,
    private val authenticationManager: AuthenticationManager
) : RecoveryCredentialFactory {
    private val random = SecureRandom()

    override suspend fun create(): RecoveryCredentialCreation {
        val correlationId = UuidCreator.getTimeOrderedEpoch().toString()
        val code = CharArray(CODE_LENGTH) { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }
        val salt = KeyDerivation.generateSalt()
        val secret = SecretChars.copyOf(code)
        var envelope: KeyEnvelope? = null
        var ownershipTransferred = false
        return try {
            val ownedKey = kdfRunner.execute(secret) { chars ->
                OwnedBytes(KeyDerivation.deriveKeyBytesArgon2id(chars, salt))
            }
            val rawKey = ownedKey.consume()
            try {
                envelope = dekManager.withDek { dek ->
                    EnvelopeManager.wrapWithKey(
                        type = EnvelopeType.RECOVERY,
                        dek = dek,
                        wrappingKey = SecretKeySpec(rawKey, "AES"),
                        salt = salt,
                        algorithm = KdfAlgorithm.ARGON2ID
                    )
                }
            } finally {
                rawKey.fill(0)
                ownedKey.discard()
            }
            val draft = SecureRecoveryCodeDraft(
                generationId = correlationId,
                sourceCode = code,
                sourceEnvelope = requireNotNull(envelope),
                vaultBootstrapStore = vaultBootstrapStore,
                authenticationManager = authenticationManager
            )
            ownershipTransferred = true
            envelope = null
            RecoveryCredentialCreation.Ready(draft)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            RecoveryCredentialCreation.Failed(
                AuthenticationFailure(
                    AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
                    AuthenticationRequestId(correlationId),
                )
            )
        } finally {
            code.fill('\u0000')
            envelope?.let(KeyEnvelope::destroy)
            if (!ownershipTransferred) salt.fill(0)
            secret.close()
        }
    }

    private companion object {
        const val CODE_LENGTH = 20
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}

private class SecureRecoveryCodeDraft(
    generationId: String,
    sourceCode: CharArray,
    sourceEnvelope: KeyEnvelope,
    private val vaultBootstrapStore: VaultBootstrapStore,
    private val authenticationManager: AuthenticationManager
) : RecoveryCredentialDraft {
    override val id = RecoveryCredentialId(generationId)
    private val mutex = Mutex()
    private var code: CharArray? = sourceCode.copyOf()
    private var envelope: KeyEnvelope? = sourceEnvelope

    @Synchronized
    override fun reveal(): CharArray? = code?.copyOf()

    override suspend fun commit(): AuthenticationResult = mutex.withLock {
        val current = envelope ?: return@withLock AuthenticationResult.Failure(
            AuthenticationFailure(AuthenticationFailureCode.ENVELOPE_CORRUPTED, AuthenticationRequestId(id.value))
        )
        val copy = current.copy(
            ciphertext = current.ciphertext.copyOf(),
            iv = current.iv.copyOf(),
            salt = current.salt.copyOf()
        )
        try {
            vaultBootstrapStore.save(copy)
            close()
            try {
                authenticationManager.refreshAvailability()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // Envelope 已提交；认证入口会在下次请求前再次刷新。
            }
            AuthenticationResult.Success(AuthenticationMethod.RECOVERY_CODE)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            AuthenticationResult.Failure(
                AuthenticationFailure(
                    AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
                    AuthenticationRequestId(id.value),
                )
            )
        } finally {
            KeyEnvelope.destroy(copy)
        }
    }

    @Synchronized
    override fun close() {
        code?.fill('\u0000')
        code = null
        envelope?.let(KeyEnvelope::destroy)
        envelope = null
    }
}
