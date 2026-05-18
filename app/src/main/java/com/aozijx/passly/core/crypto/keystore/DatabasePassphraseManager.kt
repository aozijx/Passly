package com.aozijx.passly.core.crypto.keystore

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricPrompt
import androidx.core.content.edit
import com.aozijx.passly.core.crypto.cryptoconstants.CryptoConstants
import com.aozijx.passly.core.crypto.memory.MemoryCleaner
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher

object DatabasePassphraseManager {
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

    fun getInitializedCipher(context: Context): Cipher? =
        AndroidKeyStoreCipherHelper.getInitializedCipher(context)

    fun clearDecryptedPassphrase() {
        synchronized(lock) {
            MemoryCleaner.wipeByteArray(_decryptedPassphrase)
            _decryptedPassphrase = null
        }
    }

    fun processResult(context: Context, result: BiometricPrompt.AuthenticationResult): ByteArray {
        val cipher = result.cryptoObject?.cipher
            ?: throw IllegalStateException("CryptoObject is null")
        val prefs = context.getSharedPreferences(CryptoConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(CryptoConstants.KEY_DB_PASSPHRASE, null)

        return if (encryptedBase64 == null) {
            val newPassphrase =
                ByteArray(CryptoConstants.GENERATED_PASSPHRASE_BYTES).also {
                    SecureRandom().nextBytes(
                        it
                    )
                }
            encryptAndStorePassphrase(context, cipher, newPassphrase)
            newPassphrase
        } else {
            decryptStoredPassphrase(cipher, encryptedBase64)
        }
    }

    fun prepareForRekey(context: Context, invalidateOnBiometricChange: Boolean) {
        val alias = AndroidKeyStoreCipherHelper.getAlias(context)
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
        context.getSharedPreferences(CryptoConstants.PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(CryptoConstants.KEY_DB_PASSPHRASE)
        }
        AndroidKeyStoreCipherHelper.generateMasterKey(alias, invalidateOnBiometricChange)
    }

    fun completeRekey(
        context: Context,
        result: BiometricPrompt.AuthenticationResult,
        passphrase: ByteArray
    ) {
        val cipher = result.cryptoObject?.cipher
            ?: throw IllegalStateException("CryptoObject is null")
        encryptAndStorePassphrase(context, cipher, passphrase)
    }

    private fun encryptAndStorePassphrase(context: Context, cipher: Cipher, passphrase: ByteArray) {
        val encrypted = cipher.doFinal(passphrase)
        val combined = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv).put(encrypted).array()
        context.getSharedPreferences(CryptoConstants.PREFS_NAME, Context.MODE_PRIVATE).edit {
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