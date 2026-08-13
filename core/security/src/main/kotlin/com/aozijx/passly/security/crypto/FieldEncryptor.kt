package com.aozijx.passly.security.crypto

import com.aozijx.passly.core.error.boundary.CryptoException
import com.aozijx.passly.security.MemoryCleaner
import java.security.SecureRandom
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
 * - [encrypt]: String → ByteArray（96-bit random nonce + ciphertext，无 Base64）
 * - [decrypt]: ByteArray → String（从 BLOB 读取后解密为明文）
 *
 * ## AAD（Additional Authenticated Data）
 * - [aad] 参数绑定 Entry UUID，防止密文跨记录替换攻击
 * - 加密/解密时 AAD 必须一致，否则 GCM Tag 验证失败
 * - 持久化业务数据应提供稳定 AAD；null 只用于明确不需要记录绑定的场景
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
    private val secureRandom = SecureRandom()

    fun encrypt(data: String, aad: ByteArray? = null): ByteArray {
        val key = sessionKeyManager.getSessionKey()
        val nonce = ByteArray(CryptoConfig.IV_LENGTH).also(secureRandom::nextBytes)
        val plaintext = data.toByteArray(Charsets.UTF_8)
        var encrypted: ByteArray? = null
        var secretKey: SecretKeySpec? = null
        return try {
            secretKey = SecretKeySpec(key, CryptoConfig.AES_KEY_ALGORITHM)
            val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                secretKey,
                GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, nonce)
            )

            aad?.let { cipher.updateAAD(it) }

            val ciphertext = cipher.doFinal(plaintext)
            encrypted = ciphertext

            ByteArray(nonce.size + ciphertext.size).also {
                System.arraycopy(nonce, 0, it, 0, nonce.size)
                System.arraycopy(ciphertext, 0, it, nonce.size, ciphertext.size)
            }
        } finally {
            MemoryCleaner.wipeByteArray(key)
            MemoryCleaner.wipeByteArray(nonce)
            MemoryCleaner.wipeByteArray(plaintext)
            encrypted?.let(MemoryCleaner::wipeByteArray)
            secretKey?.let { runCatching { it.destroy() } }
        }
    }

    fun decrypt(encryptedData: ByteArray, aad: ByteArray? = null): String {
        require(
            encryptedData.size >=
                    CryptoConfig.IV_LENGTH + CryptoConfig.GCM_TAG_BITS / Byte.SIZE_BITS
        ) {
            "加密字段长度无效"
        }
        val key = sessionKeyManager.getSessionKey()
        val nonce = encryptedData.copyOfRange(0, CryptoConfig.IV_LENGTH)
        val ciphertext = encryptedData.copyOfRange(
            CryptoConfig.IV_LENGTH,
            encryptedData.size
        )
        var plaintext: ByteArray? = null
        var secretKey: SecretKeySpec? = null
        return try {
            secretKey = SecretKeySpec(key, CryptoConfig.AES_KEY_ALGORITHM)

            val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, nonce)
            )

            aad?.let { cipher.updateAAD(it) }

            try {
                val decrypted = cipher.doFinal(ciphertext)
                plaintext = decrypted
                String(decrypted, Charsets.UTF_8)
            } catch (e: AEADBadTagException) {
                throw CryptoException.TagVerificationFailed(
                    "加密字段验证失败", e
                )
            }
        } finally {
            MemoryCleaner.wipeByteArray(key)
            MemoryCleaner.wipeByteArray(nonce)
            MemoryCleaner.wipeByteArray(ciphertext)
            plaintext?.let(MemoryCleaner::wipeByteArray)
            secretKey?.let { runCatching { it.destroy() } }
        }
    }
}
