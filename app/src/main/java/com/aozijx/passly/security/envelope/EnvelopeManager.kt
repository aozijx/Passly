package com.aozijx.passly.security.envelope

import com.aozijx.passly.core.log.Logcat
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 信封管理器 —— 负责信封的创建和生命周期管理。
 *
 * 封装了 AES-GCM 加密逻辑和 EnvelopeStore 交互，
 * 由 [com.aozijx.passly.security.crypto.DekManager] 委托使用。
 */
@Singleton
class EnvelopeManager @Inject constructor(
    private val envelopeStore: EnvelopeStore
) {
    companion object {
        private const val TAG = "EnvelopeManager"
        private const val DEK_LENGTH = 32
        private const val IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private const val ALGORITHM = "AES/GCM/NoPadding"
    }

    /**
     * 创建新信封：AES-GCM 加密 DEK 并持久化。
     *
     * @param type 信封类型
     * @param wrappingKey 加密 DEK 的密钥（从认证方式获取）
     * @param kdfParams KDF 参数（仅 APP_PASSWORD / RECOVERY 需要）
     * @param dek 要加密的 DEK
     * @return 新创建的信封
     */
    fun create(
        type: EnvelopeType,
        wrappingKey: SecretKeySpec,
        kdfParams: KdfParams?,
        dek: ByteArray
    ): Envelope {
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(dek)

        val envelope = Envelope(
            id = type.value,
            type = type,
            dekCiphertext = ciphertext,
            iv = iv,
            kdfParams = kdfParams
        )

        envelopeStore.save(envelope)
        Logcat.i(TAG, "Envelope persisted: ${envelope.id}")

        return envelope
    }

    // ─────────────────────────────────────────────────────────
    //  信封查询（无锁委托）
    // ─────────────────────────────────────────────────────────

    /** 获取指定类型的信封 */
    fun get(type: EnvelopeType): Envelope? = envelopeStore.get(type.value)

    /** 获取所有信封 ID */
    fun getAllIds(): Set<String> = envelopeStore.getAllIds()

    /** 是否存在任何信封 */
    fun hasAny(): Boolean = envelopeStore.hasAny()

    /** 删除指定信封 */
    fun remove(type: EnvelopeType) {
        envelopeStore.remove(type.value)
        Logcat.i(TAG, "Envelope removed: ${type.value}")
    }

    /** 删除所有信封 */
    fun removeAll() {
        envelopeStore.clearAll()
    }

    /**
     * 直接用 Android Keystore Cipher 的加密结果创建信封（不经过 SecretKeySpec）。
     *
     * 用于 Biometric / DeviceCredential 类型信封的创建，其密钥永不出 AndroidKeyStore。
     */
    fun createFromCipher(
        type: EnvelopeType,
        iv: ByteArray,
        ciphertext: ByteArray,
        kdfParams: KdfParams? = null
    ): Envelope {
        val envelope = Envelope(
            id = type.value,
            type = type,
            dekCiphertext = ciphertext,
            iv = iv,
            kdfParams = kdfParams
        )
        envelopeStore.save(envelope)
        Logcat.i(TAG, "Envelope created from cipher: ${envelope.id}")
        return envelope
    }
}