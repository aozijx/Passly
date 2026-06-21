package com.aozijx.passly.core.crypto.keystore

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricPrompt
import androidx.core.content.edit
import com.aozijx.passly.core.crypto.cryptoconstants.CryptoConstants
import com.aozijx.passly.core.crypto.memory.MemoryCleaner
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.AppDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabasePassphraseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lock = Any()

    @Volatile
    private var _decryptedPassphrase: ByteArray? = null

    val isLocked: Boolean
        get() = synchronized(lock) { _decryptedPassphrase == null }

    fun getPassphrase(): ByteArray {
        return synchronized(lock) {
            _decryptedPassphrase?.clone()
                ?: throw IllegalStateException("Database passphrase not available.")
        }
    }

    fun setDecryptedPassphrase(passphrase: ByteArray?) {
        synchronized(lock) {
            _decryptedPassphrase = passphrase?.clone()
        }
    }

    fun getInitializedCipher(): Cipher? =
        AndroidKeyStoreCipherHelper.getInitializedCipher(context)

    fun clearDecryptedPassphrase() {
        synchronized(lock) {
            MemoryCleaner.wipeByteArray(_decryptedPassphrase)
            _decryptedPassphrase = null
        }
    }

    fun processResult(result: BiometricPrompt.AuthenticationResult): ByteArray {
        val cipher = result.cryptoObject?.cipher
            ?: throw IllegalStateException("CryptoObject is null")
        val prefs = context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(CryptoConstants.KEY_DB_PASSPHRASE, null)

        return if (encryptedBase64 == null) {
            val newPassphrase =
                ByteArray(CryptoConstants.GENERATED_PASSPHRASE_BYTES).also {
                    SecureRandom().nextBytes(
                        it
                    )
                }
            encryptAndStorePassphrase(cipher, newPassphrase)
            newPassphrase
        } else {
            try {
                decryptStoredPassphrase(cipher, encryptedBase64)
            } catch (e: AEADBadTagException) {
                Logcat.e("DBPassphrase", "密钥已失效，正在清理并重置...", e)
                // 清理已失效的密钥和加密口令
                val alias = AndroidKeyStoreCipherHelper.getAlias(context)
                val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (ks.containsAlias(alias)) ks.deleteEntry(alias)
                prefs.edit { remove(CryptoConstants.KEY_DB_PASSPHRASE) }
                throw IllegalStateException("密钥已失效，请重新认证")
            }
        }
    }

    fun prepareForRekey(invalidateOnBiometricChange: Boolean) {
        val alias = AndroidKeyStoreCipherHelper.getAlias(context)
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
        context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(CryptoConstants.KEY_DB_PASSPHRASE)
        }
        AndroidKeyStoreCipherHelper.generateMasterKey(alias, invalidateOnBiometricChange)
    }

    fun completeRekey(
        result: BiometricPrompt.AuthenticationResult,
        passphrase: ByteArray
    ) {
        val cipher = result.cryptoObject?.cipher
            ?: throw IllegalStateException("CryptoObject is null")
        encryptAndStorePassphrase(cipher, passphrase)
    }

    private fun encryptAndStorePassphrase(cipher: Cipher, passphrase: ByteArray) {
        val encrypted = cipher.doFinal(passphrase)
        val combined = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv).put(encrypted).array()
        context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(
                CryptoConstants.KEY_DB_PASSPHRASE,
                Base64.encodeToString(combined, Base64.NO_WRAP)
            )
        }
    }

    private fun decryptStoredPassphrase(cipher: Cipher, encryptedBase64: String): ByteArray {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(combined)
        buffer.position(CryptoConstants.IV_LENGTH)
        val encryptedData = ByteArray(buffer.remaining()).also { buffer.get(it) }
        return cipher.doFinal(encryptedData)
    }
}