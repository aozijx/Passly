package com.aozijx.passly.security.crypto

import android.util.Base64
import com.aozijx.passly.domain.model.AppDefaults
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 字段级加密：AES-256-GCM。
 * 密钥来自 [SessionManager]，仅在用户已认证解锁时可用；锁定状态下调用会抛出。
 */
object FieldEncryptor {
    fun encrypt(data: String): String {
        val key = SessionManager.getSessionKey()
        return try {
            val secretKey = SecretKeySpec(key, AppDefaults.Crypto.AES_KEY_ALGORITHM)
            val cipher = Cipher.getInstance(AppDefaults.Crypto.ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(data.toByteArray())
            val iv = cipher.iv
            val combined = ByteBuffer.allocate(iv.size + encrypted.size)
                .put(iv)
                .put(encrypted)
                .array()
            val result = Base64.encodeToString(combined, Base64.NO_WRAP)
            MemoryCleaner.wipeByteArray(encrypted)
            MemoryCleaner.wipeByteArray(combined)
            result
        } finally {
            MemoryCleaner.wipeByteArray(key)
        }
    }

    fun decrypt(encryptedData: String): String {
        val key = SessionManager.getSessionKey()
        return try {
            val secretKey = SecretKeySpec(key, AppDefaults.Crypto.AES_KEY_ALGORITHM)
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
            val buffer = ByteBuffer.wrap(combined)
            val iv = ByteArray(AppDefaults.Crypto.IV_LENGTH).also { buffer.get(it) }
            val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val cipher = Cipher.getInstance(AppDefaults.Crypto.ALGORITHM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(AppDefaults.Crypto.GCM_TAG_BITS, iv)
            )
            val decrypted = cipher.doFinal(encrypted)
            val result = String(decrypted)
            MemoryCleaner.wipeByteArray(encrypted)
            MemoryCleaner.wipeByteArray(decrypted)
            MemoryCleaner.wipeByteArray(combined)
            result
        } finally {
            MemoryCleaner.wipeByteArray(key)
        }
    }
}