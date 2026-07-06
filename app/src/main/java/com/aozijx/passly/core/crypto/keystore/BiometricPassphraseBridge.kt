package com.aozijx.passly.core.crypto.keystore

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricPrompt
import androidx.core.content.edit
import com.aozijx.passly.core.crypto.memory.MemoryCleaner
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.model.AppDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 桥接层：协调生物识别认证与密钥管理。
 *
 * 认证职责（本类）：管理 AndroidKeyStore Cipher 生命周期。
 * 密钥职责（委托给 [MasterPassphraseProvider]）：密钥的创建、存储、获取。
 */
@Singleton
class BiometricPassphraseBridge @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val masterPassphraseProvider: MasterPassphraseProvider
) {
    val isLocked: Boolean
        get() = !masterPassphraseProvider.isUnlocked

    fun getPassphrase(): ByteArray = masterPassphraseProvider.getOrCreatePassphrase()

    fun setDecryptedPassphrase(passphrase: ByteArray?) {
        if (passphrase != null) {
            masterPassphraseProvider.setDecryptedPassphrase(passphrase)
        } else {
            masterPassphraseProvider.clear()
        }
    }

    fun getInitializedCipher(): Cipher? =
        AndroidKeyStoreCipherHelper.getInitializedCipher(context)

    fun clearDecryptedPassphrase() {
        masterPassphraseProvider.clear()
    }

    /**
     * 处理生物识别认证结果，获取数据库密钥。
     *
     * 逻辑：
     * - 如果已有生物识别加密的密钥 → 解密返回
     * - 如果有 pending 密钥（如 bootstrapPassword 生成）→ 加密存储后返回
     * - 如果都没有 → 通过 Provider 获取/创建密钥并加密存储
     */
    fun processResult(result: BiometricPrompt.AuthenticationResult): ByteArray {
        val cipher = result.cryptoObject?.cipher
            ?: throw IllegalStateException("CryptoObject is null")
        val prefs = context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(AppDefaults.Crypto.KEY_DB_PASSPHRASE, null)

        // 已有生物识别加密密钥 → 解密返回
        if (encryptedBase64 != null) {
            return try {
                Logcat.i("BiometricBridge", "Decrypting passphrase from biometric store")
                masterPassphraseProvider.decryptWithBiometric(cipher)
            } catch (e: AEADBadTagException) {
                Logcat.e("BiometricBridge", "密钥已失效，正在清理并重置...", e)
                val alias = AndroidKeyStoreCipherHelper.getAlias(context)
                val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (ks.containsAlias(alias)) ks.deleteEntry(alias)
                prefs.edit { remove(AppDefaults.Crypto.KEY_DB_PASSPHRASE) }
                masterPassphraseProvider.clearPending()
                throw IllegalStateException("密钥已失效，请重新认证")
            }
        }

        // 无生物识别加密密钥 → 通过 Provider 获取或创建，然后加密存储
        Logcat.i("BiometricBridge", "No biometric-encrypted passphrase, syncing from provider")
        val passphrase = masterPassphraseProvider.getOrCreatePassphrase()
        try {
            masterPassphraseProvider.setDecryptedPassphrase(passphrase)
            masterPassphraseProvider.encryptWithBiometric(cipher)
            Logcat.i("BiometricBridge", "Passphrase synced to biometric store")
            return passphrase
        } finally {
            MemoryCleaner.wipeByteArray(passphrase)
        }
    }

    fun prepareForRekey(invalidateOnBiometricChange: Boolean) {
        val alias = AndroidKeyStoreCipherHelper.getAlias(context)
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
        context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(AppDefaults.Crypto.KEY_DB_PASSPHRASE)
        }
        masterPassphraseProvider.clearPending()
        AndroidKeyStoreCipherHelper.generateMasterKey(alias, invalidateOnBiometricChange)
    }

    fun completeRekey(
        result: BiometricPrompt.AuthenticationResult,
        passphrase: ByteArray
    ) {
        val cipher = result.cryptoObject?.cipher
            ?: throw IllegalStateException("CryptoObject is null")

        val encrypted = cipher.doFinal(passphrase)
        val combined = java.nio.ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv).put(encrypted).array()
        context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(
                AppDefaults.Crypto.KEY_DB_PASSPHRASE,
                Base64.encodeToString(combined, Base64.NO_WRAP)
            )
        }
    }
}
