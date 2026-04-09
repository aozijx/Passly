package com.aozijx.passly.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.aozijx.passly.core.logging.Logcat
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/**
 * 管理加密数据库所需的 256 位随机口令。
 * 口令本身由 Android KeyStore 保护。
 */
object DatabasePassphraseManager {
    private const val TAG = "PassphraseManager"
    private const val PREFS_NAME = "secure_db_prefs"
    private const val KEY_PASSPHRASE = "db_phrase"

    fun getPassphrase(context: Context): ByteArray {
        val alias = "${context.packageName}.vault_db_passphrase_key"
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        // 1. 确保 KeyStore 中存在主密钥
        if (!ks.containsAlias(alias)) {
            generateMasterKey(alias)
        }

        val secretKey = (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPassphrase = sharedPrefs.getString(KEY_PASSPHRASE, null)

        // 2. 如果已存在加密的口令，则解密并返回
        if (encryptedPassphrase != null) {
            try {
                return decryptPassphrase(encryptedPassphrase, secretKey)
            } catch (e: Exception) {
                Logcat.e(TAG, "Database passphrase decryption failed!", e)
                throw e
            }
        }

        // 3. 否则生成新口令，加密后存储
        val newPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        encryptAndSavePassphrase(newPassphrase, secretKey, sharedPrefs)
        return newPassphrase
    }

    private fun generateMasterKey(alias: String) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    private fun decryptPassphrase(encryptedBase64: String, secretKey: java.security.Key): ByteArray {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(combined)
        val iv = ByteArray(12).also { buffer.get(it) }
        val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private fun encryptAndSavePassphrase(
        passphrase: ByteArray,
        secretKey: java.security.Key,
        prefs: android.content.SharedPreferences
    ) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(passphrase)
        
        val combined = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv).put(encrypted).array()

        prefs.edit {
            putString(KEY_PASSPHRASE, Base64.encodeToString(combined, Base64.NO_WRAP))
        }
    }
}
