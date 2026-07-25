package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.security.KeyDerivation
import com.aozijx.passly.domain.auth.model.envelope.EnvelopeType
import com.aozijx.passly.domain.auth.model.envelope.KdfAlgorithm
import com.aozijx.passly.domain.auth.model.envelope.KeyEnvelope
import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.RecoveryCodeDraft
import com.aozijx.passly.domain.authentication.RecoveryCodeDraftCreation
import com.aozijx.passly.domain.authentication.RecoveryCodeDraftFactory
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.EnvelopeCrypto
import com.aozijx.passly.security.envelope.BootstrapStore
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
    private val bootstrapStore: BootstrapStore,
    private val authenticationManager: AuthenticationManager
) : RecoveryCodeDraftFactory {
    private val random = SecureRandom()

    override suspend fun create(): RecoveryCodeDraftCreation {
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
                    EnvelopeCrypto.wrapWithKey(
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
                bootstrapStore = bootstrapStore,
                authenticationManager = authenticationManager
            )
            ownershipTransferred = true
            envelope = null
            RecoveryCodeDraftCreation.Ready(draft)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            RecoveryCodeDraftCreation.Failed(
                AuthenticationFailure(
                    AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
                    correlationId
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
    override val generationId: String,
    sourceCode: CharArray,
    sourceEnvelope: KeyEnvelope,
    private val bootstrapStore: BootstrapStore,
    private val authenticationManager: AuthenticationManager
) : RecoveryCodeDraft {
    private val mutex = Mutex()
    private var code: CharArray? = sourceCode.copyOf()
    private var envelope: KeyEnvelope? = sourceEnvelope

    @Synchronized
    override fun reveal(): CharArray? = code?.copyOf()

    override suspend fun commit(): AuthenticationResult = mutex.withLock {
        val current = envelope ?: return@withLock AuthenticationResult.Failure(
            AuthenticationFailure(AuthenticationFailureCode.ENVELOPE_CORRUPTED, generationId)
        )
        val copy = current.copy(
            ciphertext = current.ciphertext.copyOf(),
            iv = current.iv.copyOf(),
            salt = current.salt.copyOf()
        )
        try {
            bootstrapStore.save(copy)
            clear()
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
                AuthenticationFailure(AuthenticationFailureCode.SESSION_TRANSITION_FAILED, generationId)
            )
        } finally {
            KeyEnvelope.destroy(copy)
        }
    }

    @Synchronized
    override fun clear() {
        code?.fill('\u0000')
        code = null
        envelope?.let(KeyEnvelope::destroy)
        envelope = null
    }
}
