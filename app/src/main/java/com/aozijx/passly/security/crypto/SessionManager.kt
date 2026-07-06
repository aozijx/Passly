package com.aozijx.passly.security.crypto

import com.aozijx.passly.domain.model.AppDefaults
import com.aozijx.passly.security.crypto.SessionManager.sessionAgeMs
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 字段级加密会话密钥管理器。
 *
 * ## 生命周期
 *
 * ```
 * 认证成功（生物识别 / 应用密码 / 恢复码）
 *   --> DekManager 解密 DEK
 *     --> [deriveAndSet]  派生会话密钥并记录时间戳
 *       --> [isActive] == true，FieldEncryptor 可正常加解密
 *         ... 用户操作中 ...
 *       --> 空闲超时（AppIdleMonitor）触发 DekManager.lock()
 *         --> [clearSessionKey] 清零并置 [isActive] == false
 *       --> 或：手动锁定 / 退后台锁定 / 强力终止
 *         --> 下次解密必须重新认证
 * ```
 *
 * ## 约束
 * - 密钥不在磁盘上明文存在
 * - 锁定后内存清零，不可恢复
 * - 最大会话时长由 AppIdleMonitor 管理，本类提供 [sessionAgeMs] 用于审计
 */
object SessionManager {
    /** 默认最大会话时长（5 分钟），由 AppIdleMonitor 实际管控 */
    const val DEFAULT_MAX_SESSION_MS: Long = 5 * 60 * 1000L

    private val lock = Any()

    @Volatile
    private var _sessionDek: ByteArray? = null

    /** 会话密钥派生时间（epoch millis），0 表示未激活 */
    @Volatile
    private var createdAt: Long = 0L

    /** 会话是否有效 */
    val isActive: Boolean
        get() = _sessionDek != null

    /** 会话已存在时长（毫秒），未激活时返回 0 */
    val sessionAgeMs: Long
        get() {
            val created = createdAt
            return if (created == 0L) 0L else System.currentTimeMillis() - created
        }

    /**
     * 获取当前会话密钥（clone）。
     * @throws IllegalStateException 如果会话未激活（Vault 已锁定）
     */
    fun getSessionKey(): ByteArray = synchronized(lock) {
        _sessionDek?.clone()
            ?: throw IllegalStateException("会话已锁定，请重新认证")
    }

    /**
     * 从 DEK 派生会话密钥并激活会话。
     *
     * 使用 HMAC-SHA256(dek, "passly-vault-field-key-v1") 作为派生方式。
     * 每次认证成功后由 [DekManager] 调用。
     */
    fun deriveAndSet(dek: ByteArray) {
        synchronized(lock) {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(dek, "HmacSHA256"))
            val derivedKey = mac.doFinal(AppDefaults.Crypto.DERIVE_LABEL.toByteArray())
            MemoryCleaner.wipeByteArray(_sessionDek)
            _sessionDek = derivedKey
            createdAt = System.currentTimeMillis()
        }
    }

    /**
     * 清除会话密钥，标记会话结束。
     *
     * 由 [DekManager.lock] 调用。
     */
    fun clearSessionKey() {
        synchronized(lock) {
            MemoryCleaner.wipeByteArray(_sessionDek)
            _sessionDek = null
            createdAt = 0L
        }
    }
}