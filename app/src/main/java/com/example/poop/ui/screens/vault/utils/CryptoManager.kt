package com.example.poop.ui.screens.vault.utils

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

    // 标准安全密钥（受 10s 验证限制）
    private const val KEY_ALIAS_SECURE = "com.example.poop.vault_master_key_v2"
    // 免验证密钥（用于 TOTP 自动刷新等静默场景）
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
            // 静默密钥不要求生物识别验证，只要设备已解锁即可使用
            builder.setUserAuthenticationRequired(false)
        } else {
            // 安全密钥要求 10 秒内有过生物识别验证
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
     * 获取加密 Cipher
     * @param isSilent 是否使用免验证密钥
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
     * 获取解密 Cipher
     * @param isSilent 是否使用免验证密钥
     */
    fun getDecryptCipher(iv: ByteArray, isSilent: Boolean = false): Cipher? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getKey(isSilent), spec)
            cipher
        } catch (e: Exception) {
            Logcat.cryptoError("CryptoManager", "getDecryptCipher(silent=$isSilent)", e)
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
        } catch (e: Exception) {
            if (e is AEADBadTagException) {
                // 如果是标签不匹配，说明密钥用错了，记录为 Warn 即可，不要记录为 Error 抛堆栈
                Logcat.w("CryptoManager", "Decryption tag mismatch (Possible wrong key alias)")
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
