package com.aozijx.passly.security.dek

import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.core.crypto.FieldKeyProvider
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 持有由 DEK 派生的字段加密密钥；只负责派生、复制和擦除。
 * 认证时长与锁定策略由 security.lock 负责。
 */
@Singleton
class FieldKeyManager @Inject constructor() : FieldKeyProvider {
    private val lock = Any()

    @Volatile
    private var key: ByteArray? = null

    /**
     * 获取当前会话密钥（clone）。
     * @throws IllegalStateException 如果会话未激活（Vault 已锁定）
     */
    override fun copyKey(): ByteArray = synchronized(lock) {
        key?.clone()
            ?: throw IllegalStateException("会话已锁定，请重新认证")
    }

    /**
     * 从 DEK 派生会话密钥并激活会话。
     *
     * 使用 HMAC-SHA256(dek, "passly-entry-field-key-v1") 作为派生方式。
     * 每次认证成功后由 [DekManager] 调用。
     */
    fun deriveAndSet(dek: ByteArray) {
        synchronized(lock) {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(dek, "HmacSHA256"))
            val derivedKey = mac.doFinal(DERIVE_LABEL.toByteArray())
            MemoryCleaner.wipeByteArray(key)
            key = derivedKey
        }
    }

    /**
     * 清除会话密钥，标记会话结束。
     *
     * 由 [DekManager.lock] 调用。
     */
    fun clear() {
        synchronized(lock) {
            MemoryCleaner.wipeByteArray(key)
            key = null
        }
    }

    private companion object {
        const val DERIVE_LABEL = "passly-entry-field-key-v1"
    }
}
