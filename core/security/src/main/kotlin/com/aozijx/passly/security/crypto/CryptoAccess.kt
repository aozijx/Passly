package com.aozijx.passly.security.crypto

/**
 * 集中化解密门面，避免各处直接调用 [FieldEncryptor]。
 */
object CryptoAccess {
    fun decryptOrNull(ciphertext: ByteArray?, fieldEncryptor: FieldEncryptor): String? {
        if (ciphertext == null) return null
        if (ciphertext.isEmpty()) return ""
        return runCatching { fieldEncryptor.decrypt(ciphertext) }.getOrNull()
    }
}
