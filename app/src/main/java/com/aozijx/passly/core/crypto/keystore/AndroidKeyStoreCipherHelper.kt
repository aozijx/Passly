package com.aozijx.passly.core.crypto.keystore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.AppDefaults
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

internal object AndroidKeyStoreCipherHelper {
    private const val TAG = "KeystoreCipher"

    fun getAlias(context: Context) =
        "${context.packageName}.${AppDefaults.Crypto.KEYSTORE_ALIAS_SUFFIX}"

    fun getInitializedCipher(context: Context): Cipher? =
        getInitializedCipher(context, isRetry = false)

    private fun getInitializedCipher(context: Context, isRetry: Boolean): Cipher? {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = getAlias(context)

        if (!ks.containsAlias(alias)) generateMasterKey(alias, invalidateOnBiometricChange = true)

        val secretKey = (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        val prefs = context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(AppDefaults.Crypto.KEY_DB_PASSPHRASE, null)

        return try {
            val cipher = Cipher.getInstance(AppDefaults.Crypto.ALGORITHM)
            if (encryptedBase64 == null) {
                Logcat.i(TAG, "Init ENCRYPT mode for new passphrase.")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            } else {
                Logcat.i(TAG, "Init DECRYPT mode for existing passphrase.")
                val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val iv = ByteArray(AppDefaults.Crypto.IV_LENGTH).also {
                    ByteBuffer.wrap(combined).get(it)
                }
                cipher.init(
                    Cipher.DECRYPT_MODE, secretKey,
                    GCMParameterSpec(AppDefaults.Crypto.GCM_TAG_BITS, iv)
                )
            }
            cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            if (isRetry) {
                Logcat.e(TAG, "Key still invalid after reset, giving up.", e)
                return null
            }
            Logcat.e(TAG, "Key invalidated. Resetting...", e)
            ks.deleteEntry(alias)
            prefs.edit { remove(AppDefaults.Crypto.KEY_DB_PASSPHRASE) }
            getInitializedCipher(context, isRetry = true)
        } catch (e: Exception) {
            Logcat.e(TAG, "Failed to init cipher", e)
            null
        }
    }

    fun generateMasterKey(alias: String, invalidateOnBiometricChange: Boolean) {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AppDefaults.Crypto.KEY_SIZE_BITS)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
            .setInvalidatedByBiometricEnrollment(invalidateOnBiometricChange)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }
}