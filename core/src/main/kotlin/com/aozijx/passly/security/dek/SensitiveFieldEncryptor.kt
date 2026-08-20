package com.aozijx.passly.security.dek

import com.aozijx.passly.core.crypto.CryptoConfig
import com.aozijx.passly.core.crypto.MemoryCleaner
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensitiveFieldEncryptor @Inject constructor(
    private val keyManager: SensitiveDataKeyManager
) {
    private val random = SecureRandom()

    suspend fun encrypt(data: String, aad: ByteArray): ByteArray =
        keyManager.withProvisionedKey { encryptWithKey(data, aad, it) }

    suspend fun decrypt(encryptedData: ByteArray, aad: ByteArray): String =
        keyManager.withUnlockedKey { decryptWithKey(encryptedData, aad, it) }

    suspend fun decryptProvisioned(encryptedData: ByteArray, aad: ByteArray): String =
        keyManager.withProvisionedKey { decryptWithKey(encryptedData, aad, it) }

    private fun encryptWithKey(data: String, aad: ByteArray, key: ByteArray): ByteArray {
        val nonce = ByteArray(CryptoConfig.IV_LENGTH).also(random::nextBytes)
        val plaintext = data.toByteArray(Charsets.UTF_8)
        return try {
            val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(aad)
            nonce + cipher.doFinal(plaintext)
        } finally {
            MemoryCleaner.wipeByteArray(plaintext)
        }
    }

    private fun decryptWithKey(data: ByteArray, aad: ByteArray, key: ByteArray): String {
        require(data.size > CryptoConfig.IV_LENGTH)
        val nonce = data.copyOfRange(0, CryptoConfig.IV_LENGTH)
        val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        val plaintext = cipher.doFinal(data, CryptoConfig.IV_LENGTH, data.size - CryptoConfig.IV_LENGTH)
        return try { String(plaintext, Charsets.UTF_8) } finally { MemoryCleaner.wipeByteArray(plaintext) }
    }
}
