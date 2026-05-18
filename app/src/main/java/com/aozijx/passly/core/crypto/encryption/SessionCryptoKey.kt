package com.aozijx.passly.core.crypto.encryption

import com.aozijx.passly.core.crypto.cryptoconstants.CryptoConstants
import com.aozijx.passly.core.crypto.memory.MemoryCleaner
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 字段级加密会话密钥。
 * 通过 HMAC-SHA256 从 DB passphrase 派生 DEK，与 passphrase 同生命周期：
 * 解锁时派生并持有，锁定时清零——再次解密必须重新认证。
 */
object SessionCryptoKey {
    private val lock = Any()

    @Volatile
    private var _sessionDek: ByteArray? = null

    val isSessionKeyAvailable: Boolean
        get() = synchronized(lock) { _sessionDek != null }

    fun getSessionKey(): ByteArray = synchronized(lock) {
        _sessionDek?.clone()
            ?: throw IllegalStateException("Field DEK not loaded; vault is locked.")
    }

    fun deriveAndSet(dbPassphrase: ByteArray) {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(dbPassphrase, "HmacSHA256"))
        synchronized(lock) {
            _sessionDek = mac.doFinal(CryptoConstants.DERIVE_LABEL.toByteArray())
        }
    }

    fun clearSessionKey() {
        synchronized(lock) {
            MemoryCleaner.wipeByteArray(_sessionDek)
            _sessionDek = null
        }
    }
}