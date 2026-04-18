package com.aozijx.passly.core.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 字段级加密会话密钥。
 * 通过 HMAC-SHA256 从 DB passphrase 派生 DEK，与 passphrase 同生命周期：
 * 解锁时派生并持有，锁定时清零——再次解密必须重新认证。
 */
object SessionCryptoKey {
    private const val DERIVE_LABEL = "passly-vault-field-key-v1"

    @Volatile
    private var _sessionDek: ByteArray? = null

    val isSessionKeyAvailable: Boolean get() = _sessionDek != null

    fun getSessionKey(): ByteArray =
        _sessionDek ?: throw IllegalStateException("Field DEK not loaded; vault is locked.")

    fun deriveAndSet(dbPassphrase: ByteArray) {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(dbPassphrase, "HmacSHA256"))
        _sessionDek = mac.doFinal(DERIVE_LABEL.toByteArray())
    }

    fun clearSessionKey() {
        _sessionDek?.fill(0)
        _sessionDek = null
    }
}
