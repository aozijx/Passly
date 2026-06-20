package com.aozijx.passly.core.crypto.encryption

import android.util.Base64
import com.aozijx.passly.core.crypto.cryptoconstants.CryptoConstants
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 字段级加密：AES-256-GCM。
 * 密钥来自 [SessionCryptoKey]，仅在用户已认证解锁时可用；锁定状态下调用会抛出。
 */
object FieldEncryptor {
    fun encrypt(data: String): String {
        val key = SecretKeySpec(SessionCryptoKey.getSessionKey(), CryptoConstants.AES_KEY_ALGORITHM)
        val cipher = Cipher.getInstance(CryptoConstants.ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(data.toByteArray())
        val combined = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedData: String): String {
        val key = SecretKeySpec(SessionCryptoKey.getSessionKey(), CryptoConstants.AES_KEY_ALGORITHM)
        val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(combined)
        val iv = ByteArray(CryptoConstants.IV_LENGTH).also { buffer.get(it) }
        val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val cipher = Cipher.getInstance(CryptoConstants.ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(CryptoConstants.GCM_TAG_BITS, iv))
        return String(cipher.doFinal(encrypted))
    }
}