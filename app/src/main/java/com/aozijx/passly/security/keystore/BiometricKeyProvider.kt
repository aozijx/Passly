package com.aozijx.passly.security.keystore

import android.content.Context
import androidx.biometric.BiometricPrompt
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.UnlockError
import com.aozijx.passly.security.crypto.UnlockResult
import com.aozijx.passly.domain.model.envelope.EnvelopeType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 生物识别密钥提供者 —— 职责仅限于 BiometricPrompt 和 AndroidKeystore 交互。
 *
 * ## 职责边界
 * - BiometricPrompt 调用
 * - Cipher 初始化（加密/解密模式）
 * - AuthenticationResult 处理（转发给 [DekManager]）
 * - AuthenticationCallback
 *
 * ## 绝不
 * - 缓存 DEK
 * - 判断锁状态
 * - 参与 AES 加密/解密
 * - 创建/查询/删除信封
 *
 * DEK 的提取、校验、缓存全部由 [DekManager] 负责。
 */
@Singleton
class BiometricKeyProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dekManager: DekManager
) {
    companion object {
        private const val TAG = "BiometricKey"
    }

    /**
     * 获取初始化的 Cipher，根据是否已有 Biometric 信封自动选择加解密模式。
     *
     * - 已有信封 → 解密模式（用于解锁）
     * - 无信封   → 加密模式（用于首次引导）
     */
    suspend fun getInitializedCipher(): Cipher? {
        val envelope = dekManager.getEnvelope(EnvelopeType.BIOMETRIC)
        return if (envelope != null) {
            AndroidKeyStoreProvider.getCipherForDecrypt(context, envelope.iv)
        } else {
            AndroidKeyStoreProvider.getCipherForEncrypt(context)
        }
    }

    /**
     * 处理生物识别认证结果。
     *
     * 根据是否已有 Biometric 信封，委托 [DekManager] 执行解锁或首次引导。
     * BiometricKeyProvider 只传递 Cipher，不接触 DEK 字节。
     */
    suspend fun processResult(result: BiometricPrompt.AuthenticationResult): UnlockResult {
        val cipher = result.cryptoObject?.cipher
            ?: return UnlockResult.Failed(
                UnlockError.AUTH_FAILED
            )

        val hasEnvelope = dekManager.getEnvelope(EnvelopeType.BIOMETRIC) != null

        return if (hasEnvelope) {
            Logcat.i(TAG, "Delegating biometric unlock to DekManager")
            withContext(Dispatchers.IO) { dekManager.unlock(EnvelopeType.BIOMETRIC, cipher) }
        } else {
            Logcat.i(TAG, "No biometric envelope, delegating bootstrap to DekManager")
            withContext(Dispatchers.IO) {
                runCatching {
                    dekManager.initializeWithCipher(EnvelopeType.BIOMETRIC, cipher)
                    UnlockResult.Success
                }.getOrElse { e ->
                    UnlockResult.Failed(UnlockError.UNKNOWN)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Rekey（重新加密生物识别信封）
    // ─────────────────────────────────────────────────────────

    /**
     * 准备 Rekey：删除旧 AndroidKeyStore 密钥和 Biometric 信封。
     */
    suspend fun prepareForRekey(invalidateOnBiometricChange: Boolean) {
        val alias = AndroidKeyStoreProvider.getAlias(context)
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(alias)) {
            ks.deleteEntry(alias)
            Logcat.i(TAG, "Old Keystore key deleted for rekey")
        }
        dekManager.removeEnvelope(EnvelopeType.BIOMETRIC)
        Logcat.i(TAG, "Old biometric envelope removed for rekey")
        AndroidKeyStoreProvider.generateMasterKey(alias, invalidateOnBiometricChange)
    }

    /**
     * 完成 Rekey：用新 Cipher 加密当前 DEK，保存为新的 Biometric 信封。
     *
     * DEK 加密操作由 [DekManager.rekeyBiometric] 内部完成，
     * BiometricKeyProvider 只传递 Cipher。
     */
    suspend fun completeRekey(result: BiometricPrompt.AuthenticationResult) {
        val cipher = result.cryptoObject?.cipher
            ?: throw IllegalStateException("CryptoObject is null")

        withContext(Dispatchers.IO) {
            dekManager.rekey(EnvelopeType.BIOMETRIC, cipher)
        }
    }
}