package com.aozijx.passly.security.session

import kotlinx.coroutines.flow.StateFlow

/**
 * 会话管理器接口 —— 管理认证后的会话生命周期。
 *
 * 职责边界：
 * - isAuthorized 状态查询
 * - 锁定/解锁
 * - 空闲超时管理
 *
 * 与 Auth 的分离：
 * - Auth 负责认证（生物识别、应用密码）
 * - Session 负责认证后的会话状态（锁定、超时）
 */
interface UserSessionManager : SessionStateProvider {
    /** 当前是否已认证 */
    override val isAuthorized: StateFlow<Boolean>

    /** 锁定应用，清理 DEK、会话密钥、关闭数据库 */
    suspend fun lock()

    /** 用户交互，重置空闲定时器 */
    fun onUserInteraction()

    /** 认证成功后回调：启动空闲定时器 */
    suspend fun onAuthSuccess()
}