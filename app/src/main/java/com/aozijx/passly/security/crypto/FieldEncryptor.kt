package com.aozijx.passly.security.crypto

import com.aozijx.passly.core.error.crypto.CryptoException
import com.aozijx.passly.security.MemoryCleaner
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 字段级加密：AES-256-GCM。
 *
 * 密钥来自 [SessionKeyManager]，仅在用户已认证解锁时可用；锁定状态下调用会抛出。
 *
 * ## 输入/输出
 * - [encrypt]: String → ByteArray（IV + ciphertext，无 Base64，直接存 BLOB）
 * - [decrypt]: ByteArray → String（从 BLOB 读取后解密为明文）
 *
 * ## AAD（Additional Authenticated Data）
 * - [aad] 参数绑定 Entry UUID，防止密文跨记录替换攻击
 * - 加密/解密时 AAD 必须一致，否则 GCM Tag 验证失败
 * - 调用方（如 CryptoAccess）可传 null 表示无 AAD
 *
 * ## 安全注意事项
 * - String 不可变，明文在 GC 前无法彻底擦除；尽量缩短 String 生命周期
 * - SecretKeySpec 内部会复制 key，wipe 原始 key 不影响 SecretKeySpec 内部的副本（JVM 限制）
 * - 返回的 ByteArray 密文无 Base64 膨胀，体积减小约 33%
 */
@Singleton
class FieldEncryptor @Inject constructor(
    private val sessionKeyManager: SessionKeyManager
) {
    fun encrypt(data: String, aad: ByteArray? = null): ByteArray {
        val key = sessionKeyManager.getSessionKey()
        return try {
            val secretKey = SecretKeySpec(key, CryptoConfig.AES_KEY_ALGORITHM)
            val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            aad?.let { cipher.updateAAD(it) }

            val plaintext = data.toByteArray(Charsets.UTF_8)
            val encrypted = cipher.doFinal(plaintext)
            val iv = cipher.iv

            val combined = ByteArray(iv.size + encrypted.size).also {
                System.arraycopy(iv, 0, it, 0, iv.size)
                System.arraycopy(encrypted, 0, it, iv.size, encrypted.size)
            }

            // 尽可能擦除中间数据（受 JVM 限制，不能完全保证）
            MemoryCleaner.wipeByteArray(plaintext)
            MemoryCleaner.wipeByteArray(encrypted)

            combined
        } finally {
            // wipe key；但 SecretKeySpec 内部已复制，擦除效果有限（JVM 限制）
            MemoryCleaner.wipeByteArray(key)
        }
    }

    fun decrypt(encryptedData: ByteArray, aad: ByteArray? = null): String {
        val key = sessionKeyManager.getSessionKey()
        return try {
            val secretKey = SecretKeySpec(key, CryptoConfig.AES_KEY_ALGORITHM)

            val iv = encryptedData.copyOfRange(0, CryptoConfig.IV_LENGTH)
            val encrypted = encryptedData.copyOfRange(
                CryptoConfig.IV_LENGTH,
                encryptedData.size
            )

            val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, iv)
            )

            aad?.let { cipher.updateAAD(it) }

            try {
                val decrypted = cipher.doFinal(encrypted)
                val result = String(decrypted, Charsets.UTF_8)
                MemoryCleaner.wipeByteArray(decrypted)
                MemoryCleaner.wipeByteArray(iv)
                result
            } catch (e: AEADBadTagException) {
                // Tag 验证失败：数据库可能损坏，或密钥不匹配
                MemoryCleaner.wipeByteArray(iv)
                throw CryptoException.TagVerificationFailed(
                    "GCM 认证标签验证失败：数据可能已损坏或密钥不匹配（AAD 不一致也会触发）", e
                )
            }
        } finally {
            MemoryCleaner.wipeByteArray(key)
        }
    }
}
