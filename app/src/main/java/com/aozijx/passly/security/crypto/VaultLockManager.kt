package com.aozijx.passly.security.crypto

import kotlinx.coroutines.flow.StateFlow

/**
 * Vault 锁定状态枚举。
 */
enum class LockState {
    /** Vault 已锁定，需要认证才能访问 */
    LOCKED,

    /** Vault 已解锁，DEK 可用 */
    UNLOCKED
}

/**
 * Vault 锁定状态查询接口 —— 只读的 Session Observer。
 *
 * 职责边界：
 * - isUnlocked / isLocked / lockState 查询
 * - 绝不参与 unlock / createEnvelope / DEK 操作
 *
 * Repository 层通过本接口判断 Vault 是否可访问，
 * 因为解锁可能来自生物识别、应用密码、恢复码、Passkey 等任意方式。
 * 锁状态的唯一权威来源是 [DekManager]。
 */
interface VaultLockManager {
    /** Vault 当前是否已解锁 */
    fun isUnlocked(): Boolean

    /** Vault 当前是否已锁定 */
    fun isLocked(): Boolean

    /** 锁状态的响应式流（[LockState]） */
    val lockState: StateFlow<LockState>
}
