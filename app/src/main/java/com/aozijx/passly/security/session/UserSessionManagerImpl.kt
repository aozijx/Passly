package com.aozijx.passly.security.session

import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.security.crypto.DekManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话状态管理器 —— 管理认证会话生命周期（锁定/解锁/空闲超时）。
 *
 * 职责：
 * - isAuthorized 状态从 DekManager 派生（唯一真相源）
 * - 协调 AppIdleMonitor 自动锁定
 * - 手动锁定/解锁
 *
 * 不属于 Auth 模块 —— Auth 只负责认证，Session 负责会话生命周期。
 */
@Singleton
class UserSessionManagerImpl @Inject constructor(
    private val dekManager: DekManager,
    private val idleMonitor: AppIdleMonitor
) : UserSessionManager {

    private companion object {
        private const val TAG = "UserSessionManagerImpl"
    }

    private val stateMutex = Mutex()

    override val isAuthorized: StateFlow<Boolean> = dekManager.isUnlocked

    init {
        idleMonitor.configure { lock() }
    }

    /**
     * 认证成功后回调：重置空闲定时器。
     */
    override suspend fun onAuthSuccess() {
        stateMutex.withLock {
            if (!dekManager.isUnlocked.value) {
                Logcat.w(TAG, "Ignoring auth success because no DEK is loaded")
                return
            }

            Logcat.i(TAG, "Auth success, resetting idle timer")
            idleMonitor.resetIdleTimer()
        }
    }

    /**
     * 锁定应用：清理 DEK、会话密钥、关闭数据库、取消空闲定时器。
     */
    override suspend fun lock() {
        stateMutex.withLock {
            if (!dekManager.isUnlocked.value) return

            Logcat.i(TAG, "Locking application")
            dekManager.lock()
            idleMonitor.cancel()
        }
    }

    /**
     * 用户交互时重置空闲定时器。
     */
    override fun onUserInteraction() {
        if (!dekManager.isUnlocked.value) return
        idleMonitor.resetIdleTimer()
    }

    override fun isLocked(): Boolean = !dekManager.isUnlocked.value

    override fun isUnlocked(): Boolean = dekManager.isUnlocked.value
}
