package com.aozijx.passly.data.repository.security

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.AuthFailed
import com.aozijx.passly.core.security.KeyDerivation
import com.aozijx.passly.domain.model.envelope.EnvelopeType
import com.aozijx.passly.domain.model.envelope.KdfAlgorithm
import com.aozijx.passly.domain.repository.security.RecoveryCodeRepository
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.UnlockResult
import com.aozijx.passly.security.session.UserSessionManager
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RecoveryCodeRepositoryImpl @Inject constructor(
    private val dekManager: DekManager,
    private val sessionManager: UserSessionManager
) : RecoveryCodeRepository {

    private companion object {
        const val CODE_LENGTH = 20
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val GCM_TAG_BITS = 128
    }

    private val random = SecureRandom()

    override suspend fun create(): CharArray {
        check(!hasRecoveryCode()) { "Recovery code already exists." }
        return generateAndPersist()
    }

    override suspend fun regenerate(): CharArray {
        check(hasRecoveryCode()) { "Recovery code has not been created." }
        return generateAndPersist()
    }

    override suspend fun hasRecoveryCode(): Boolean =
        dekManager.getEnvelope(EnvelopeType.RECOVERY) != null

    override suspend fun verify(code: CharArray): Boolean {
        var key: javax.crypto.spec.SecretKeySpec? = null
        return try {
            if (code.isEmpty()) return false
            val envelope = dekManager.getEnvelope(EnvelopeType.RECOVERY) ?: return false
            val derivedKey = KeyDerivation.deriveKeyArgon2id(code, envelope.salt)
            key = derivedKey
            dekManager.verifyEnvelope(EnvelopeType.RECOVERY, derivedKey)
        } finally {
            key?.encoded?.fill(0)
            code.fill('\u0000')
        }
    }

    override suspend fun unlock(code: CharArray): AppResult<Unit> {
        var key: javax.crypto.spec.SecretKeySpec? = null
        return try {
            if (code.isEmpty()) return AppResult.failure(AuthFailed("请输入恢复码"))
            val envelope = dekManager.getEnvelope(EnvelopeType.RECOVERY)
                ?: return AppResult.failure(AuthFailed("尚未创建恢复码"))
            val derivedKey = KeyDerivation.deriveKeyArgon2id(code, envelope.salt)
            key = derivedKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                derivedKey,
                GCMParameterSpec(GCM_TAG_BITS, envelope.iv)
            )
            when (dekManager.unlock(EnvelopeType.RECOVERY, cipher)) {
                UnlockResult.Success -> {
                    sessionManager.onAuthSuccess()
                    AppResult.success(Unit)
                }

                is UnlockResult.Failed -> AppResult.failure(AuthFailed("恢复码错误"))
            }
        } finally {
            key?.encoded?.fill(0)
            code.fill('\u0000')
        }
    }

    private suspend fun generateAndPersist(): CharArray {
        check(dekManager.isUnlocked.value) { "Vault must be unlocked." }
        val code = CharArray(CODE_LENGTH) {
            CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]
        }
        val salt = KeyDerivation.generateSalt()
        val key = KeyDerivation.deriveKeyArgon2id(code, salt)
        try {
            dekManager.rekeyWithKey(
                type = EnvelopeType.RECOVERY,
                wrappingKey = key,
                salt = salt,
                algorithm = KdfAlgorithm.ARGON2ID
            )
            return code
        } catch (error: Throwable) {
            code.fill('\u0000')
            throw error
        } finally {
            key.encoded.fill(0)
        }
    }
}
