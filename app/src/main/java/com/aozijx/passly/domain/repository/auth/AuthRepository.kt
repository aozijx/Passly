package com.aozijx.passly.domain.repository.auth

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.StateFlow

/**
 * 认证仓库接口。
 */
interface AuthRepository {
    /** 全局授权状态 */
    val isAuthorized: StateFlow<Boolean>

    /** 应用密码是否已启用 */
    val isAppPasswordEnabled: StateFlow<Boolean>

    /**
     * 触发生物识别/系统凭据认证。
     * 认证成功后自动完成授权与口令注入。
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): Result<Unit>

    /** 强制二次验证身份（不依赖当前 isAuthorized 状态） */
    suspend fun verifyIdentity(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): Result<Unit>

    /** 使用应用密码解锁（不依赖系统生物识别） */
    suspend fun authenticateWithAppPassword(password: CharArray): Result<Unit>

    /** 设置应用密码（需要当前已解锁） */
    suspend fun setAppPassword(password: CharArray): Result<Unit>

    /** 冷启动首次设置应用密码（无生物识别时） */
    suspend fun bootstrapAppPassword(password: CharArray): Result<Unit>

    /** 修改应用密码（需要旧密码） */
    suspend fun changeAppPassword(oldPassword: CharArray, newPassword: CharArray): Result<Unit>

    /** 关闭应用密码（需要当前密码） */
    suspend fun disableAppPassword(password: CharArray): Result<Unit>

    /**
     * 通知仓库：外部流程（如自动填充）已独立完成生物识别并注入口令。
     * 用于接管授权状态与自动锁定计时器，避免口令无限期滞留内存。
     */
    fun onExternalAuthorized()

    /** 执行锁定，清除所有敏感授权材料 */
    fun lock()

    /** 记录用户交互，用于重置自动锁定计时器 */
    fun onUserInteraction()

    /** 检查是否需要立即锁定 */
    fun checkAndLock()

    /** 应用新的锁定超时设置 */
    fun updateLockTimeout(timeoutMs: Long)

    /**
     * 切换生物识别变更时是否销毁密钥的策略。
     * 会重新生成 Keystore 密钥并重新加密数据库口令。
     */
    suspend fun rekeyWithInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean
    ): Result<Unit>
}