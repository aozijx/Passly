package com.aozijx.passly.security.vault

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.aozijx.passly.core.logging.Logcat
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * DEK 校验标签管理器。
 *
 * 校验标签用于验证从信封解密出的 DEK 是否正确。
 * DEK 对固定明文做 AES-GCM 加密 → 持久化到 SharedPreferences。
 * 解锁时用 DEK 解密校验标签 → 比对明文。
 */
class VerificationTag(
    private val context: Context,
    private val prefsName: String,
    private val keyVerifyTag: String
) {
    companion object {
        private const val TAG = "VerificationTag"
        private const val IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val VERIFY_PLAINTEXT = "PASSLY_DEK_VERIFY_V1"
    }

    /**
     * 生成并持久化 DEK 校验标签。
     *
     * 格式：[IV(12B)][ciphertext+tag]
     */
    fun save(dek: ByteArray) {
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = SecretKeySpec(dek, "AES")
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(VERIFY_PLAINTEXT.toByteArray())

        val combined =
            ByteBuffer.allocate(iv.size + ciphertext.size).put(iv).put(ciphertext).array()

        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit {
            putString(keyVerifyTag, Base64.encodeToString(combined, Base64.NO_WRAP))
        }
    }

    /**
     * 验证 DEK 正确性。
     *
     * @param dek 待验证的 DEK
     * @param envelopeId 用于日志的信封标识
     * @throws AEADBadTagException 如果 DEK 错误导致 GCM 认证失败
     * @throws IllegalArgumentException 如果校验明文不匹配
     */
    fun verify(dek: ByteArray, envelopeId: String) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val tagBase64 = prefs.getString(keyVerifyTag, null)

        if (tagBase64 == null) {
            Logcat.w(
                TAG,
                "Verification tag missing for envelope '$envelopeId', skipping verification (migration scenario)"
            )
            return
        }

        val combined = Base64.decode(tagBase64, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(combined)
        val iv = ByteArray(IV_LENGTH).also { buffer.get(it) }
        val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }

        val key = SecretKeySpec(dek, "AES")
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

        val plaintext = String(cipher.doFinal(encrypted))

        if (plaintext != VERIFY_PLAINTEXT) {
            throw IllegalArgumentException(
                "DEK verification failed for envelope '$envelopeId': " +
                        "plaintext mismatch (database may be corrupted)"
            )
        }
    }

    /**
     * 删除校验标签。
     */
    fun delete() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit {
            remove(keyVerifyTag)
        }
    }
}