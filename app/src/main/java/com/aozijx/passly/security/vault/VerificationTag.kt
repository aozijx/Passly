package com.aozijx.passly.security.vault

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Creates and verifies an authenticated marker for the vault DEK.
 *
 * Persistence belongs to BootstrapStore; this object only performs crypto.
 */
object VerificationTag {
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private val VERIFY_PLAINTEXT = "PASSLY_DEK_VERIFY_V1".toByteArray(Charsets.UTF_8)

    fun create(dek: ByteArray): ByteArray {
        val iv = ByteArray(IV_LENGTH).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(dek, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        val ciphertext = cipher.doFinal(VERIFY_PLAINTEXT)
        return ByteBuffer.allocate(iv.size + ciphertext.size)
            .put(iv)
            .put(ciphertext)
            .array()
    }

    fun verify(dek: ByteArray, encodedTag: ByteArray, envelopeId: String) {
        require(encodedTag.size > IV_LENGTH) {
            "Invalid DEK verification tag for envelope '$envelopeId'"
        }
        val buffer = ByteBuffer.wrap(encodedTag)
        val iv = ByteArray(IV_LENGTH).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(dek, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        require(cipher.doFinal(ciphertext).contentEquals(VERIFY_PLAINTEXT)) {
            "DEK verification failed for envelope '$envelopeId'"
        }
    }
}
