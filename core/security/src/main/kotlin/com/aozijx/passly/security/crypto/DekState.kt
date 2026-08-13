package com.aozijx.passly.security.crypto

/**
 * DEK 状态机。
 *
 * 状态转换：
 *   Locked → Unlocked（认证成功）
 *   Unlocked → Locking → Locked（锁定）
 *   Unlocked → Deleting → Locked（删除 Vault）
 */
sealed interface DekState {
    /** DEK 未加载，Vault 已锁定 */
    data object Locked : DekState

    /** DEK 已加载到内存，Vault 可访问 */
    class Unlocked(val dek: ByteArray) : DekState

    /** 锁定进行中，DEK 正在擦除 */
    data object Locking : DekState

    /** Vault 删除进行中 */
    data object Deleting : DekState
}