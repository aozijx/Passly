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
 * 保持硬件级认证绑定及指纹变更自动恢复能力。
 */
object DatabasePassphraseManager {
    private const val TAG = "PassphraseManager"
    private const val PREFS_NAME = "secure_db_prefs"
    private const val KEY_PASSPHRASE = "db_phrase"
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128

    @Volatile
    private var _decryptedPassphrase: ByteArray? = null

    /**
     * 数据库是否处于锁定状态（口令未解密到内存）。
     */
    val isLocked: Boolean get() = _decryptedPassphrase == null

    private fun getAlias(context: Context) = "${context.packageName}.vault_db_hard_auth"

    /**
     * 获取初始化的 Cipher。
     * 自动处理密钥失效（如用户更改了系统指纹）。
     */
    fun getInitializedCipher(context: Context): Cipher? =
        getInitializedCipher(context, isRetry = false)

    private fun getInitializedCipher(context: Context, isRetry: Boolean): Cipher? {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = getAlias(context)

        if (!ks.containsAlias(alias)) generateMasterKey(alias)

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

    /**
     * 认证通过后，执行最终的解密或创建逻辑。
     */
    fun processResult(context: Context, result: BiometricPrompt.AuthenticationResult): ByteArray {
        val cipher =
            result.cryptoObject?.cipher ?: throw IllegalStateException("CryptoObject is null")
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = sharedPrefs.getString(KEY_PASSPHRASE, null)

        return if (encryptedBase64 == null) {
            val newPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val encrypted = cipher.doFinal(newPassphrase)
            val combined = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
                .put(cipher.iv).put(encrypted).array()
            sharedPrefs.edit {
                putString(KEY_PASSPHRASE, Base64.encodeToString(combined, Base64.NO_WRAP))
            }
            newPassphrase
        } else {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val buffer = ByteBuffer.wrap(combined)
            buffer.position(IV_LENGTH) // 跳过 IV（已在 getInitializedCipher 中使用）
            val encryptedData = ByteArray(buffer.remaining()).also { buffer.get(it) }
            cipher.doFinal(encryptedData)
        }
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
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
            .setInvalidatedByBiometricEnrollment(true) 
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
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