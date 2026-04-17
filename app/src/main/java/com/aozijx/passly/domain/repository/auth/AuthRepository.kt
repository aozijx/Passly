package com.aozijx.passly.domain.repository.auth

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.StateFlow

/**
 * 认证仓库接口。
 */
interface AuthRepository {
    /** 全局授权状态 */
    val isAuthorized: StateFlow<Boolean>

    /**
     * 触发生物识别/系统凭据认证。
     * 认证成功后自动完成授权与口令注入。
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): Result<Unit>

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
}
