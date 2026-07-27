package com.aozijx.passly.domain.auth.model

/**
 * 锁状态强度枚举。
 *
 * UNLOCKED < SOFT_LOCKED < SEALED
 * 请求的目标强度高于当前状态时必须继续执行锁升级。
 * 仅当目标强度 ≤ 当前强度时才能跳过。
 */
enum class VaultLockState(val strength: Int) {
    UNLOCKED(0),
    SOFT_LOCKED(1),
    SEALED(2);

    /** 是否需要升级到 [target]（当前状态弱于目标） */
    infix fun shouldEscalateTo(target: VaultLockState): Boolean =
        this.strength < target.strength
}
