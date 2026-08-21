package com.aozijx.passly.core.crypto

import com.aozijx.passly.core.error.boundary.CryptoException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AesGcmCryptoEngine @Inject constructor() : CryptoEngine {
    private val random = SecureRandom()

    override fun encrypt(plaintext: ByteArray, key: ByteArray, aad: ByteArray?): ByteArray {
        val nonce = ByteArray(CryptoConfig.IV_LENGTH).also(random::nextBytes)
        return try {
            val cipher = cipher(Cipher.ENCRYPT_MODE, key, nonce, aad)
            nonce + cipher.doFinal(plaintext)
        } finally {
            MemoryCleaner.wipeByteArray(nonce)
        }
    }

    override fun decrypt(payload: ByteArray, key: ByteArray, aad: ByteArray?): ByteArray {
        require((payload.size >= (CryptoConfig.IV_LENGTH + (CryptoConfig.GCM_TAG_BITS / Byte.SIZE_BITS)))) {
            "Invalid AES-GCM payload length"
        }
        val nonce = payload.copyOfRange(0, CryptoConfig.IV_LENGTH)
        val ciphertext = payload.copyOfRange(CryptoConfig.IV_LENGTH, payload.size)
        return try {
            cipher(Cipher.DECRYPT_MODE, key, nonce, aad).doFinal(ciphertext)
        } catch (error: AEADBadTagException) {
            throw CryptoException.TagVerificationFailed("Encrypted data verification failed", error)
        } finally {
            MemoryCleaner.wipeByteArray(nonce)
            MemoryCleaner.wipeByteArray(ciphertext)
        }
    }

    private fun cipher(mode: Int, key: ByteArray, nonce: ByteArray, aad: ByteArray?): Cipher =
        Cipher.getInstance(CryptoConfig.ALGORITHM).apply {
            init(
                mode,
                SecretKeySpec(key, CryptoConfig.AES_KEY_ALGORITHM),
                GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, nonce),
            )
            aad?.let(::updateAAD)
        }
}
