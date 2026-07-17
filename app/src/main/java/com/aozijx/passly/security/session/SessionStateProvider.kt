package com.aozijx.passly.security.session

import kotlinx.coroutines.flow.StateFlow

/**
 * 会话状态只读接口 —— Repository 层通过本接口查询 Vault 是否可访问。
 *
 * 职责边界：
 * - isAuthorized / isLocked / isUnlocked 查询
 * - 绝不参与 unlock / lock / 任何修改操作
 *
 * 这是 [UserSessionManager] 的只读投影，Repository 层只应依赖此接口。
 */
interface SessionStateProvider {
    /** 当前是否已认证（Vault 可访问） */
    val isAuthorized: StateFlow<Boolean>

    /** Vault 当前是否已锁定 */
    fun isLocked(): Boolean

    /** Vault 当前是否已解锁 */
    fun isUnlocked(): Boolean
}