package com.aozijx.passly.data.crypto

import com.aozijx.passly.core.security.KeyDerivation
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

object KeyEnvelopeCipher {

    private const val GCM_TAG_BITS = 128
    private const val IV_LENGTH = 12
    private val secureRandom = SecureRandom()

    /**
     * 用应用密码加密 DEK
     * @param dek 32 字节的 DEK（明文）
     * @param password 用户输入的应用密码
     * @return Pair(ciphertext, salt)  —— salt 用于 KDF，ciphertext = IV + 加密内容
     */
    fun encryptDekWithAppPassword(dek: ByteArray, password: CharArray): Pair<ByteArray, ByteArray> {
        val salt = ByteArray(32).also { secureRandom.nextBytes(it) }
        val key = deriveKeyFromPassword(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
        val encrypted = cipher.doFinal(dek)
        val iv = cipher.iv
        val ciphertext = ByteArray(iv.size + encrypted.size).apply {
            System.arraycopy(iv, 0, this, 0, iv.size)
            System.arraycopy(encrypted, 0, this, iv.size, encrypted.size)
        }
        return Pair(ciphertext, salt)
    }

    /**
     * 用应用密码解密 DEK
     * @param ciphertext 存储的密文（IV + 加密内容）
     * @param salt KDF 盐
     * @param password 用户输入的应用密码
     */
    fun decryptDekWithAppPassword(
        ciphertext: ByteArray,
        salt: ByteArray,
        password: CharArray
    ): ByteArray {
        val key = deriveKeyFromPassword(password, salt)
        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(ciphertext, 0, iv, 0, IV_LENGTH)
        val encrypted = ByteArray(ciphertext.size - IV_LENGTH)
        System.arraycopy(ciphertext, IV_LENGTH, encrypted, 0, encrypted.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(encrypted)
    }

    private fun deriveKeyFromPassword(
        password: CharArray,
        salt: ByteArray
    ): javax.crypto.SecretKey {
        return KeyDerivation.deriveKeyArgon2id(password, salt)
    }
}