package com.aozijx.passly.core.crypto.keystore

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.aozijx.passly.core.crypto.memory.MemoryCleaner
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.model.AppDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库主密钥的单一事实来源。
 *
 * 职责：创建、存储、提供数据库加密密钥。
 * 认证模块（生物识别、应用密码）仅负责验证身份，
 * 通过本 Provider 获取统一的加密密钥。
 *
 * 密钥生命周期：
 * 1. [getOrCreatePassphrase] — 首次调用时生成密钥并暂存为 pending
 * 2. 生物识别认证后调用 [encryptWithBiometric] 加密存储
 * 3. 应用密码设置后调用 [configure] 通过 app password 加密存储
 * 4. [clear] — 锁定/退出时清除内存中的明文密钥
 */
@Singleton
class MasterPassphraseProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        private const val TAG = "MasterPassphraseProvider"
    }

    private val lock = Any()

    @Volatile
    private var cachedPassphrase: ByteArray? = null

    val isUnlocked: Boolean
        get() = cachedPassphrase != null

    /**
     * 获取或创建数据库加密密钥（单一入口）。
     * 优先从 pending 存储中获取已有密钥，不存在则生成新的。
     */
    fun getOrCreatePassphrase(): ByteArray {
        synchronized(lock) {
            cachedPassphrase?.let { return it.clone() }

            val prefs =
                context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE)
            val pendingBase64 = prefs.getString(AppDefaults.Crypto.KEY_DB_PASSPHRASE_PENDING, null)

            val passphrase = if (pendingBase64 != null) {
                Logcat.i(TAG, "Restoring passphrase from pending storage")
                Base64.decode(pendingBase64, Base64.NO_WRAP)
            } else {
                Logcat.i(TAG, "Generating new master passphrase")
                ByteArray(AppDefaults.Crypto.GENERATED_PASSPHRASE_BYTES).also {
                    SecureRandom().nextBytes(it)
                }.also { newPassphrase ->
                    prefs.edit {
                        putString(
                            AppDefaults.Crypto.KEY_DB_PASSPHRASE_PENDING,
                            Base64.encodeToString(newPassphrase, Base64.NO_WRAP)
                        )
                    }
                }
            }

            cachedPassphrase = passphrase.clone()
            return passphrase
        }
    }

    /**
     * 设置内存中的明文密钥（认证成功后调用）。
     */
    fun setDecryptedPassphrase(passphrase: ByteArray) {
        synchronized(lock) {
            MemoryCleaner.wipeByteArray(cachedPassphrase)
            cachedPassphrase = passphrase.clone()
        }
    }

    /**
     * 生物识别加密：将 pending 密钥用生物识别 Cipher 加密后存为正式密钥，
     * 并清除 pending 存储。
     */
    fun encryptWithBiometric(cipher: Cipher) {
        synchronized(lock) {
            val passphrase = cachedPassphrase
                ?: throw IllegalStateException("No passphrase in memory for biometric encryption")

            val encrypted = cipher.doFinal(passphrase)
            val combined = java.nio.ByteBuffer.allocate(cipher.iv.size + encrypted.size)
                .put(cipher.iv).put(encrypted).array()

            context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(
                    AppDefaults.Crypto.KEY_DB_PASSPHRASE,
                    Base64.encodeToString(combined, Base64.NO_WRAP)
                )
                remove(AppDefaults.Crypto.KEY_DB_PASSPHRASE_PENDING)
            }
            Logcat.i(TAG, "Passphrase encrypted with biometric, pending cleared")
        }
    }

    /**
     * 从生物识别加密存储中解密密钥。
     */
    fun decryptWithBiometric(cipher: Cipher): ByteArray {
        val prefs = context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(AppDefaults.Crypto.KEY_DB_PASSPHRASE, null)
            ?: throw IllegalStateException("No biometric-encrypted passphrase found")

        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val buffer = java.nio.ByteBuffer.wrap(combined)
        buffer.position(AppDefaults.Crypto.IV_LENGTH)
        val encryptedData = ByteArray(buffer.remaining()).also { buffer.get(it) }

        return cipher.doFinal(encryptedData)
    }

    /**
     * 清除内存中的明文密钥（锁定/退出时调用）。
     */
    fun clear() {
        synchronized(lock) {
            MemoryCleaner.wipeByteArray(cachedPassphrase)
            cachedPassphrase = null
        }
    }

    /**
     * 清除 pending 密钥（用于清理或重置场景）。
     */
    fun clearPending() {
        val prefs = context.getSharedPreferences(AppDefaults.Auth.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(AppDefaults.Crypto.KEY_DB_PASSPHRASE_PENDING) }
    }
}