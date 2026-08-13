package com.aozijx.passly.data.settings.port

import kotlinx.coroutines.flow.Flow

/**
 * 空闲超时设置接口 —— 由 Settings 层提供，供 Session 层监听。
 *
 * 依赖方向：Session 监听 Settings 的 Flow，而非 Settings 归 Session 管。
 */
interface IdleTimeoutSettings {
    /** 退后台是否立即锁定 */
    val isLockOnBackground: Flow<Boolean>

    /** 自动锁定超时时间（毫秒） */
    val lockTimeout: Flow<Long>
}
