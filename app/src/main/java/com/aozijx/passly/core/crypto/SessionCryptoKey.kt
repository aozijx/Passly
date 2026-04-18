package com.aozijx.passly.core.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
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
 * 字段级加密会话密钥。
 * 持久化的 KEK 绑定生物识别授权，解锁时把 KEK 解出的 DEK 放入内存供 [CryptoManager] 直接使用，
 * 锁定时清空内存 DEK——再次解密必须重新认证。
 */
object SessionCryptoKey {
    private const val TAG = "SessionCryptoKey"
    private const val PREFS_NAME = "secure_db_prefs"
    private const val KEY_WRAPPED_DEK = "field_dek_wrapped"
    private const val KEY_ALIAS_SUFFIX = ".vault_field_kek_v2"
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val DEK_LENGTH = 32

    // BiometricPrompt 成功回调内通常毫秒级完成 KEK 操作；预留 10s 容忍慢设备。
    private const val AUTH_VALIDITY_SECONDS = 10

    @Volatile
    private var _sessionDek: ByteArray? = null

    val isSessionKeyAvailable: Boolean get() = _sessionDek != null

    fun getSessionKey(): ByteArray =
        _sessionDek ?: throw IllegalStateException("Field DEK not loaded; vault is locked.")

    /**
     * 在 BiometricPrompt 成功回调内调用：解出（或新建）DEK 并放入内存。
     * 必须在用户认证窗口仍然有效时（KEK timeout 内）执行。
     */
    fun loadOrCreate(context: Context) {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = getAlias(context)
        if (!ks.containsAlias(alias)) generateKek(alias)

        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wrappedBase64 = sharedPrefs.getString(KEY_WRAPPED_DEK, null)

        val dek = try {
            if (wrappedBase64 == null) {
                createAndWrapDek(ks, alias, sharedPrefs)
            } else {
                unwrapDek(ks, alias, wrappedBase64)
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            Logcat.w(TAG, "Field KEK invalidated, regenerating", e)
            ks.deleteEntry(alias)
            sharedPrefs.edit { remove(KEY_WRAPPED_DEK) }
            generateKek(alias)
            createAndWrapDek(ks, alias, sharedPrefs)
        }

        _sessionDek = dek
    }

    fun clearSessionKey() {
        _sessionDek?.fill(0)
        _sessionDek = null
    }

    private fun getAlias(context: Context) = context.packageName + KEY_ALIAS_SUFFIX

    private fun generateKek(alias: String) {
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
                AUTH_VALIDITY_SECONDS,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    private fun createAndWrapDek(
        ks: KeyStore,
        alias: String,
        sharedPrefs: SharedPreferences
    ): ByteArray {
        val secretKey = (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        val dek = ByteArray(DEK_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
        val encrypted = cipher.doFinal(dek)
        val combined = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()
        sharedPrefs.edit { putString(KEY_WRAPPED_DEK, Base64.encodeToString(combined, Base64.NO_WRAP)) }
        Logcat.i(TAG, "Generated and wrapped new field DEK")
        return dek
    }

    private fun unwrapDek(ks: KeyStore, alias: String, wrappedBase64: String): ByteArray {
        val secretKey = (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        val combined = Base64.decode(wrappedBase64, Base64.NO_WRAP)
        val iv = ByteArray(IV_LENGTH).also { ByteBuffer.wrap(combined).get(it) }
        val encrypted = combined.copyOfRange(IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(encrypted)
    }
}
