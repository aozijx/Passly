package com.aozijx.passly.security.crypto

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [VaultLockManager] 的默认实现。
 *
 * 委托 [DekManager] 的 [DekManager.lockState] 进行状态映射。
 */
@Singleton
internal class VaultLockManagerImpl @Inject constructor(
    private val dekManager: DekManager
) : VaultLockManager {

    override fun isUnlocked(): Boolean = dekManager.isUnlocked
    override fun isLocked(): Boolean = !dekManager.isUnlocked

    override val lockState: StateFlow<LockState>
        get() = dekManager.lockState
}
