package com.aozijx.passly.security.crypto

import com.aozijx.passly.security.MemoryCleaner
import com.aozijx.passly.security.SecurityCoroutineScope
import com.aozijx.passly.security.envelope.BootstrapStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensitiveDataKeyManager @Inject constructor(
    private val bootstrapStore: BootstrapStore,
    private val dekManager: DekManager,
    @param:SecurityCoroutineScope private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private val random = SecureRandom()
    private var cachedKey: ByteArray? = null
    private var expiresAtNanos: Long = 0L
    private var clearJob: Job? = null

    suspend fun unlockAfterFreshAuthentication(ttlMs: Long = DEFAULT_TTL_MS) = mutex.withLock {
        val key = loadOrCreateKey()
        clearCachedKey()
        cachedKey = key
        expiresAtNanos = System.nanoTime() + ttlMs * 1_000_000L
        clearJob = scope.launch {
            delay(ttlMs)
            clear()
        }
    }

    suspend fun <T> withUnlockedKey(block: (ByteArray) -> T): T = mutex.withLock {
        check(System.nanoTime() < expiresAtNanos) { "High-sensitivity access requires fresh authentication" }
        val key = cachedKey?.clone()
            ?: error("High-sensitivity access requires fresh authentication")
        try {
            block(key)
        } finally {
            MemoryCleaner.wipeByteArray(key)
        }
    }

    suspend fun <T> withProvisionedKey(block: (ByteArray) -> T): T = mutex.withLock {
        val key = loadOrCreateKey()
        try {
            block(key)
        } finally {
            MemoryCleaner.wipeByteArray(key)
        }
    }

    suspend fun clear() = mutex.withLock { clearCachedKey() }

    private suspend fun loadOrCreateKey(): ByteArray {
        val wrappingKey = deriveWrappingKey()
        try {
            val envelope = bootstrapStore.loadSensitiveKeyEnvelope()
            if (envelope != null) return unwrap(envelope, wrappingKey)
            val key = ByteArray(KEY_BYTES).also(random::nextBytes)
            val wrapped = wrap(key, wrappingKey)
            try {
                bootstrapStore.saveSensitiveKeyEnvelope(wrapped)
            } finally {
                MemoryCleaner.wipeByteArray(wrapped)
            }
            return key
        } finally {
            MemoryCleaner.wipeByteArray(wrappingKey)
        }
    }

    private suspend fun deriveWrappingKey(): ByteArray = dekManager.withDek { dek ->
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(dek, "HmacSHA256"))
        mac.doFinal(WRAP_LABEL.toByteArray(Charsets.UTF_8))
    }

    private fun wrap(key: ByteArray, wrappingKey: ByteArray): ByteArray {
        val nonce = ByteArray(CryptoConfig.IV_LENGTH).also(random::nextBytes)
        val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(wrappingKey, CryptoConfig.AES_KEY_ALGORITHM),
            GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, nonce)
        )
        cipher.updateAAD(WRAP_LABEL.toByteArray(Charsets.UTF_8))
        return byteArrayOf(ENVELOPE_VERSION) + nonce + cipher.doFinal(key)
    }

    private fun unwrap(envelope: ByteArray, wrappingKey: ByteArray): ByteArray {
        require(envelope.size >= MIN_ENVELOPE_BYTES) { "Invalid sensitive key envelope" }
        require(envelope.firstOrNull() == ENVELOPE_VERSION) { "Unsupported sensitive key envelope" }
        val nonce = envelope.copyOfRange(1, 1 + CryptoConfig.IV_LENGTH)
        val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(wrappingKey, CryptoConfig.AES_KEY_ALGORITHM),
            GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, nonce)
        )
        cipher.updateAAD(WRAP_LABEL.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(envelope, 1 + CryptoConfig.IV_LENGTH, envelope.size - 1 - CryptoConfig.IV_LENGTH)
    }

    private fun clearCachedKey() {
        clearJob?.cancel()
        clearJob = null
        MemoryCleaner.wipeByteArray(cachedKey)
        cachedKey = null
        expiresAtNanos = 0L
    }

    private companion object {
        const val DEFAULT_TTL_MS = 30_000L
        const val KEY_BYTES = 32
        const val ENVELOPE_VERSION: Byte = 1
        const val MIN_ENVELOPE_BYTES = 1 + CryptoConfig.IV_LENGTH + 16
        const val WRAP_LABEL = "passly-sensitive-data-key-wrap-v1"
    }
}
