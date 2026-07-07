package com.aozijx.passly.security.crypto

/**
 * Centralized decrypt facade to avoid scattered direct decrypt calls.
 */
object CryptoAccess {
    fun decryptOrNull(ciphertext: String?): String? {
        if (ciphertext == null) return null
        if (ciphertext.isEmpty()) return ""
        return runCatching { FieldEncryptor.decrypt(ciphertext) }.getOrNull()
    }
}