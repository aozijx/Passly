package com.aozijx.passly.core.crypto

import javax.inject.Inject
import javax.inject.Singleton

fun interface FieldKeyProvider {
    fun copyKey(): ByteArray
}

/** Encrypts UTF-8 fields with the active derived field key. */
@Singleton
class FieldEncryptor @Inject constructor(
    private val fieldKeyProvider: FieldKeyProvider,
    private val cryptoEngine: CryptoEngine,
) {
    fun encrypt(data: String, aad: ByteArray? = null): ByteArray {
        val key = fieldKeyProvider.copyKey()
        val plaintext = data.toByteArray(Charsets.UTF_8)
        return try {
            cryptoEngine.encrypt(plaintext, key, aad)
        } finally {
            MemoryCleaner.wipeByteArray(key)
            MemoryCleaner.wipeByteArray(plaintext)
        }
    }

    fun decrypt(encryptedData: ByteArray, aad: ByteArray? = null): String {
        val key = fieldKeyProvider.copyKey()
        var plaintext: ByteArray? = null
        return try {
            cryptoEngine.decrypt(encryptedData, key, aad).also { plaintext = it }
                .toString(Charsets.UTF_8)
        } finally {
            MemoryCleaner.wipeByteArray(key)
            MemoryCleaner.wipeByteArray(plaintext)
        }
    }
}
