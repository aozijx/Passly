package com.aozijx.passly.security.crypto

import com.aozijx.passly.security.MemoryCleaner
import com.aozijx.passly.security.envelope.BootstrapStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/** Owns the attachment-only master key. The plaintext key exists only for one operation. */
@Singleton
class AttachmentDataKeyManager @Inject constructor(
    private val bootstrapStore: BootstrapStore,
    private val dekManager: DekManager,
) {
    private val mutex = Mutex()
    private val random = SecureRandom()

    suspend fun <T> withKey(block: (ByteArray) -> T): T = mutex.withLock {
        val key = loadOrCreateKey()
        try {
            block(key)
        } finally {
            MemoryCleaner.wipeByteArray(key)
        }
    }

    private suspend fun loadOrCreateKey(): ByteArray {
        val wrappingKey = deriveWrappingKey()
        try {
            bootstrapStore.loadAttachmentKeyEnvelope()?.let { return unwrap(it, wrappingKey) }
            val key = ByteArray(KEY_BYTES).also(random::nextBytes)
            val envelope = wrap(key, wrappingKey)
            try {
                bootstrapStore.saveAttachmentKeyEnvelope(envelope)
            } finally {
                MemoryCleaner.wipeByteArray(envelope)
            }
            return key
        } finally {
            MemoryCleaner.wipeByteArray(wrappingKey)
        }
    }

    private suspend fun deriveWrappingKey(): ByteArray = dekManager.withDek { dek ->
        hmac(dek, WRAP_LABEL.toByteArray())
    }

    private fun wrap(key: ByteArray, wrappingKey: ByteArray): ByteArray {
        val nonce = ByteArray(CryptoConfig.IV_LENGTH).also(random::nextBytes)
        return try {
            val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(wrappingKey, CryptoConfig.AES_KEY_ALGORITHM),
                GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, nonce),
            )
            cipher.updateAAD(WRAP_LABEL.toByteArray())
            byteArrayOf(ENVELOPE_VERSION) + nonce + cipher.doFinal(key)
        } finally {
            MemoryCleaner.wipeByteArray(nonce)
        }
    }

    private fun unwrap(envelope: ByteArray, wrappingKey: ByteArray): ByteArray {
        require(envelope.size >= MIN_ENVELOPE_BYTES) { "Invalid attachment key envelope" }
        require(envelope[0] == ENVELOPE_VERSION) { "Unsupported attachment key envelope" }
        val nonce = envelope.copyOfRange(1, 1 + CryptoConfig.IV_LENGTH)
        return try {
            val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(wrappingKey, CryptoConfig.AES_KEY_ALGORITHM),
                GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, nonce),
            )
            cipher.updateAAD(WRAP_LABEL.toByteArray())
            cipher.doFinal(envelope, 1 + nonce.size, envelope.size - 1 - nonce.size)
        } finally {
            MemoryCleaner.wipeByteArray(nonce)
        }
    }

    private fun hmac(key: ByteArray, input: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(input)
    }

    private companion object {
        const val KEY_BYTES = 32
        const val ENVELOPE_VERSION: Byte = 1
        const val MIN_ENVELOPE_BYTES = 1 + CryptoConfig.IV_LENGTH + 16
        const val WRAP_LABEL = "passly-attachment-data-key-wrap-v1"
    }
}
