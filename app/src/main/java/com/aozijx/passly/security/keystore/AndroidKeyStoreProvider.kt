package com.aozijx.passly.security.keystore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.model.AppDefaults
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

internal object AndroidKeyStoreProvider {
    private const val TAG = "KeystoreProvider"
    private val keyGenLock = Any()

    fun getAlias(context: Context) =
        "${context.packageName}.${AppDefaults.Crypto.KEYSTORE_ALIAS_SUFFIX}"

    /** 获取 ENCRYPT_MODE Cipher（用于新建信封） */
    fun getCipherForEncrypt(context: Context): Cipher? {
        val alias = getAlias(context)
        ensureKeyExists(alias, invalidateOnBiometricChange = true)
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val secretKey = (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
            val cipher = Cipher.getInstance(AppDefaults.Crypto.ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            Logcat.i(TAG, "Cipher initialized in ENCRYPT mode")
            cipher
        } catch (e: Exception) {
            Logcat.cryptoError(TAG, "Get encrypt cipher", e)
            null
        }
    }

    /** 获取 DECRYPT_MODE Cipher（用于解密已有信封） */
    fun getCipherForDecrypt(context: Context, iv: ByteArray): Cipher? {
        val alias = getAlias(context)
        ensureKeyExists(alias, invalidateOnBiometricChange = true)
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val secretKey = (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
            val cipher = Cipher.getInstance(AppDefaults.Crypto.ALGORITHM)
            val spec = GCMParameterSpec(AppDefaults.Crypto.GCM_TAG_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            Logcat.i(TAG, "Cipher initialized in DECRYPT mode")
            cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            Logcat.cryptoError(TAG, "Key invalidated", e)
            synchronized(keyGenLock) {
                val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                ks.deleteEntry(alias)
            }
            null
        } catch (e: Exception) {
            Logcat.cryptoError(TAG, "Get decrypt cipher", e)
            null
        }
    }

    /** 确保 AndroidKeyStore 中存在该 alias 的 AES 密钥 */
    private fun ensureKeyExists(alias: String, invalidateOnBiometricChange: Boolean) {
        synchronized(keyGenLock) {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!ks.containsAlias(alias)) {
                Logcat.i(TAG, "Generating new AndroidKeyStore key: $alias")
                generateMasterKey(alias, invalidateOnBiometricChange)
            }
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
        Logcat.i(TAG, "Master key generated: $alias")
    }
}
