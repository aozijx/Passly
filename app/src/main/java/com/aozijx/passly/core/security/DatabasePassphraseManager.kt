package com.aozijx.passly.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricPrompt
import androidx.core.content.edit
import com.aozijx.passly.core.logging.Logcat
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/**
 * 管理加密数据库所需的口令。
 * 保持硬件级认证绑定，支持配置生物识别变更时是否销毁密钥。
 */
object DatabasePassphraseManager {
    private const val TAG = "PassphraseManager"
    private const val PREFS_NAME = "secure_db_prefs"
    private const val KEY_PASSPHRASE = "db_phrase"
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128

    @Volatile
    private var _decryptedPassphrase: ByteArray? = null

    val isLocked: Boolean get() = _decryptedPassphrase == null

    private fun getAlias(context: Context) = "${context.packageName}.vault_db_hard_auth"

    fun getInitializedCipher(context: Context): Cipher? =
        getInitializedCipher(context, isRetry = false)

    private fun getInitializedCipher(context: Context, isRetry: Boolean): Cipher? {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = getAlias(context)

        if (!ks.containsAlias(alias)) generateMasterKey(alias, invalidateOnBiometricChange = true)

        val secretKey = (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = sharedPrefs.getString(KEY_PASSPHRASE, null)

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            if (encryptedBase64 == null) {
                Logcat.i(TAG, "Init ENCRYPT mode for new passphrase.")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            } else {
                Logcat.i(TAG, "Init DECRYPT mode for existing passphrase.")
                val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val iv = ByteArray(IV_LENGTH).also { ByteBuffer.wrap(combined).get(it) }
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            if (isRetry) {
                Logcat.e(TAG, "Key still invalid after reset, giving up.", e)
                return null
            }
            Logcat.e(TAG, "Key invalidated. Resetting...", e)
            ks.deleteEntry(alias)
            sharedPrefs.edit { remove(KEY_PASSPHRASE) }
            getInitializedCipher(context, isRetry = true)
        } catch (e: Exception) {
            Logcat.e(TAG, "Failed to init cipher", e)
            null
        }
    }

    fun processResult(context: Context, result: BiometricPrompt.AuthenticationResult): ByteArray {
        val cipher =
            result.cryptoObject?.cipher ?: throw IllegalStateException("CryptoObject is null")
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = sharedPrefs.getString(KEY_PASSPHRASE, null)

        return if (encryptedBase64 == null) {
            val newPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
            encryptAndStorePassphrase(context, cipher, newPassphrase)
            newPassphrase
        } else {
            decryptStoredPassphrase(cipher, encryptedBase64)
        }
    }

    /**
     * 重新生成密钥并用新策略重新加密口令。
     * 用于切换 invalidatedByBiometricEnrollment 策略时的重新密钥操作。
     */
    fun prepareForRekey(context: Context, invalidateOnBiometricChange: Boolean) {
        val alias = getAlias(context)
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_PASSPHRASE)
        }
        generateMasterKey(alias, invalidateOnBiometricChange)
        Logcat.i(
            TAG,
            "Prepared for rekey: invalidateOnBiometricChange=$invalidateOnBiometricChange"
        )
    }

    /**
     * 使用认证后的 CryptoObject 完成重新加密。
     */
    fun completeRekey(
        context: Context,
        result: BiometricPrompt.AuthenticationResult,
        passphrase: ByteArray
    ) {
        val cipher = result.cryptoObject?.cipher
            ?: throw IllegalStateException("CryptoObject is null")
        encryptAndStorePassphrase(context, cipher, passphrase)
        Logcat.i(TAG, "Rekey completed.")
    }

    private fun encryptAndStorePassphrase(context: Context, cipher: Cipher, passphrase: ByteArray) {
        val encrypted = cipher.doFinal(passphrase)
        val combined = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv).put(encrypted).array()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_PASSPHRASE, Base64.encodeToString(combined, Base64.NO_WRAP))
        }
    }

    private fun decryptStoredPassphrase(cipher: Cipher, encryptedBase64: String): ByteArray {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(combined)
        buffer.position(IV_LENGTH)
        val encryptedData = ByteArray(buffer.remaining()).also { buffer.get(it) }
        return cipher.doFinal(encryptedData)
    }

    private fun generateMasterKey(alias: String, invalidateOnBiometricChange: Boolean) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
            .setInvalidatedByBiometricEnrollment(invalidateOnBiometricChange)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
        Logcat.i(
            TAG,
            "Generated master key: invalidateOnBiometricChange=$invalidateOnBiometricChange"
        )
    }

    fun getPassphrase(): ByteArray {
        return _decryptedPassphrase
            ?: throw IllegalStateException("Database passphrase not available.")
    }

    fun setDecryptedPassphrase(passphrase: ByteArray?) {
        _decryptedPassphrase = passphrase
    }

    fun clearDecryptedPassphrase() {
        _decryptedPassphrase?.fill(0)
        _decryptedPassphrase = null
    }
}