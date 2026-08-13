package com.aozijx.passly.security.dek

import com.aozijx.passly.core.error.boundary.CryptoException
import com.aozijx.passly.core.crypto.CryptoConfig
import com.aozijx.passly.core.crypto.MemoryCleaner
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentContentCrypto @Inject constructor(
    private val keyManager: AttachmentDataKeyManager,
) {
    private val random = SecureRandom()

    suspend fun contentId(content: ByteArray): String = keyManager.withKey { masterKey ->
        val idKey = derive(masterKey, ID_KEY_LABEL)
        try {
            hmac(idKey, content).toHex()
        } finally {
            MemoryCleaner.wipeByteArray(idKey)
        }
    }

    suspend fun encrypt(content: ByteArray, resourceId: String): ByteArray =
        keyManager.withKey { masterKey ->
            val encryptionKey = derive(masterKey, ENCRYPTION_KEY_LABEL)
            val nonce = ByteArray(CryptoConfig.IV_LENGTH).also(random::nextBytes)
            try {
                val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    SecretKeySpec(encryptionKey, CryptoConfig.AES_KEY_ALGORITHM),
                    GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, nonce),
                )
                cipher.updateAAD(resourceId.toByteArray(Charsets.US_ASCII))
                nonce + cipher.doFinal(content)
            } finally {
                MemoryCleaner.wipeByteArray(encryptionKey)
                MemoryCleaner.wipeByteArray(nonce)
            }
        }

    suspend fun decrypt(encrypted: ByteArray, resourceId: String): ByteArray {
        require(encrypted.size >= CryptoConfig.IV_LENGTH + CryptoConfig.GCM_TAG_BITS / Byte.SIZE_BITS) {
            "Invalid encrypted attachment length"
        }
        return keyManager.withKey { masterKey ->
            val encryptionKey = derive(masterKey, ENCRYPTION_KEY_LABEL)
            val nonce = encrypted.copyOfRange(0, CryptoConfig.IV_LENGTH)
            val ciphertext = encrypted.copyOfRange(CryptoConfig.IV_LENGTH, encrypted.size)
            try {
                val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(encryptionKey, CryptoConfig.AES_KEY_ALGORITHM),
                    GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, nonce),
                )
                cipher.updateAAD(resourceId.toByteArray(Charsets.US_ASCII))
                try {
                    cipher.doFinal(ciphertext)
                } catch (error: AEADBadTagException) {
                    throw CryptoException.TagVerificationFailed("Attachment content verification failed", error)
                }
            } finally {
                MemoryCleaner.wipeByteArray(encryptionKey)
                MemoryCleaner.wipeByteArray(nonce)
                MemoryCleaner.wipeByteArray(ciphertext)
            }
        }
    }

    suspend fun verifyContentId(content: ByteArray, resourceId: String): Boolean =
        contentId(content).equals(resourceId, ignoreCase = false)

    private fun derive(masterKey: ByteArray, label: String): ByteArray =
        hmac(masterKey, label.toByteArray(Charsets.UTF_8))

    private fun hmac(key: ByteArray, input: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(input)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val ID_KEY_LABEL = "passly-attachment-content-id-v1"
        const val ENCRYPTION_KEY_LABEL = "passly-attachment-content-encryption-v1"
    }
}
