package com.aozijx.passly.core.crypto

/** Stateless authenticated-encryption primitive. Key ownership remains with the caller. */
interface CryptoEngine {
    fun encrypt(plaintext: ByteArray, key: ByteArray, aad: ByteArray? = null): ByteArray

    fun decrypt(payload: ByteArray, key: ByteArray, aad: ByteArray? = null): ByteArray
}
