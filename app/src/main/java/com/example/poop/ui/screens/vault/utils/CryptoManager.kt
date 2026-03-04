package com.example.poop.ui.screens.vault.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.poop.util.Logcat
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"

    // 使用新的别名以确保新的安全策略（Time-bound）生效。
    // 注意：更改别名会导致旧别名加密的数据无法解密。
    private const val KEY_ALIAS = "com.example.poop.vault_master_key_v2"

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        // 关键修复：设置 10 秒的身份验证有效期 (Time-bound)。
        // 这允许在一次生物识别验证后，10秒内执行多次加密/解密操作（如同时加解密用户名和密码）。
        // 这样就不再受限于 "每个 Cipher 只能 doFinal 一次" 的限制。
        builder.setUserAuthenticationParameters(
            10,
            KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
        )

        return KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore").apply {
            init(builder.build())
        }.generateKey()
    }

    /**
     * 获取加密 Cipher
     * 内部捕获异常并返回 null，提高调用安全性
     */
    fun getEncryptCipher(): Cipher? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            cipher
        } catch (e: Exception) {
            Logcat.e("CryptoManager", "getEncryptCipher failed", e)
            null
        }
    }

    /**
     * 获取解密 Cipher
     * 内部捕获异常并返回 null，提高调用安全性
     */
    fun getDecryptCipher(iv: ByteArray): Cipher? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
            cipher
        } catch (e: Exception) {
            Logcat.e("CryptoManager", "getDecryptCipher failed", e)
            null
        }
    }

    fun encrypt(text: String, cipher: Cipher): String {
        val encryptedBytes = cipher.doFinal(text.toByteArray())
        val combined = (cipher.iv ?: ByteArray(12)) + encryptedBytes
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedText: String, cipher: Cipher): String? {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.size <= 12) return null
            val encryptedBytes = combined.sliceArray(12 until combined.size)
            String(cipher.doFinal(encryptedBytes))
        } catch (e: KeyPermanentlyInvalidatedException) {
            Logcat.e("CryptoManager", "Key invalidated. Needs reset with PIN/Pattern.", e)
            null
        } catch (e: Exception) {
            Logcat.e("CryptoManager", "Decryption failed", e)
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