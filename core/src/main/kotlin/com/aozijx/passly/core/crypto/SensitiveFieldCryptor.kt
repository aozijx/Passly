package com.aozijx.passly.core.crypto

/** Encrypts sensitive field values without exposing the key or its lifecycle to storage callers. */
interface SensitiveFieldCryptor {
    suspend fun encrypt(
        data: String,
        aad: ByteArray,
    ): ByteArray

    suspend fun decrypt(
        encryptedData: ByteArray,
        aad: ByteArray,
    ): String
}
