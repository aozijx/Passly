package com.example.poop.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.poop.util.Logcat
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"

    private const val KEY_ALIAS_SECURE = "com.example.poop.vault_master_key_v2"
    private const val KEY_ALIAS_SILENT = "com.example.poop.vault_silent_key_v1"

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun getKey(isSilent: Boolean): SecretKey {
        val alias = if (isSilent) KEY_ALIAS_SILENT else KEY_ALIAS_SECURE
        val existingKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey(isSilent)
    }

    private fun createKey(isSilent: Boolean): SecretKey {
        val alias = if (isSilent) KEY_ALIAS_SILENT else KEY_ALIAS_SECURE
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setInvalidatedByBiometricEnrollment(true)

        if (isSilent) {
            builder.setUserAuthenticationRequired(false)
        } else {
            builder.setUserAuthenticationRequired(true)
            builder.setUserAuthenticationParameters(
                10,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
        }

        return KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore").apply {
            init(builder.build())
        }.generateKey()
    }

    /**
     * 获取加密 Cipher (兼容旧版调用)
     * 每次调用都会返回一个新的、已初始化的 Cipher 实例，避免 IV 重用错误
     */
    fun getEncryptCipher(isSilent: Boolean = false): Cipher? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getKey(isSilent))
            cipher
        } catch (e: Exception) {
            Logcat.cryptoError("CryptoManager", "getEncryptCipher(silent=$isSilent)", e)
            null
        }
    }

    /**
     * 获取解密 Cipher (IV 是必须的)
     */
    fun getDecryptCipher(iv: ByteArray, isSilent: Boolean = false): Cipher? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getKey(isSilent), spec)
            cipher
        } catch (e: Exception) {
            Logcat.cryptoError("CryptoManager", "getDecryptCipher", e)
            null
        }
    }

    /**
     * 加密方法重载 1：使用外部提供的 Cipher (兼容旧版)
     * 注意：传入的 cipher 只能执行一次此方法
     */
    fun encrypt(text: String, cipher: Cipher): String {
        val encryptedBytes = cipher.doFinal(text.toByteArray())
        // 将 IV (12字节) 和密文拼接
        val combined = (cipher.iv ?: ByteArray(12)) + encryptedBytes
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * 加密方法重载 2：快捷单次加密 (自动处理 Cipher 生命周期)
     */
    fun encrypt(text: String, isSilent: Boolean = false): String? {
        val cipher = getEncryptCipher(isSilent) ?: return null
        return try {
            encrypt(text, cipher)
        } catch (e: Exception) {
            Logcat.cryptoError("CryptoManager", "quick encrypt", e)
            null
        }
    }

    /**
     * 解密方法
     */
    fun decrypt(encryptedText: String, cipher: Cipher): String? {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size <= 12) return null
            val encryptedBytes = combined.sliceArray(12 until combined.size)
            String(cipher.doFinal(encryptedBytes))
        } catch (e: Exception) {
            if (e is AEADBadTagException) {
                Logcat.w("CryptoManager", "Decryption tag mismatch")
            } else {
                Logcat.cryptoError("CryptoManager", "decrypt", e)
            }
            null
        }
    }

    fun getIvFromCipherText(encryptedText: String): ByteArray? {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size >= 12) combined.sliceArray(0 until 12) else null
        } catch (e: Exception) {
            Logcat.e("CryptoManager", "Failed to get IV from cipher text", e)
            null
        }
    }
}
