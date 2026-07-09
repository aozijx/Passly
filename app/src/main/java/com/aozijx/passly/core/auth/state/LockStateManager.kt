package com.aozijx.passly.core.auth.state

import com.aozijx.passly.core.auth.session.AppIdleMonitor
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.domain.model.AppDefaults
import com.aozijx.passly.security.crypto.DekManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 锁定状态管理器，负责：
 * 1. 维护 isAuthorized 状态
 * 2. 配置和触发自动锁定
 * 3. 手动锁定/解锁
 *
 * 使用 Mutex 保护状态更新，避免并发问题。
 */
@Singleton
class LockStateManager @Inject constructor(
    private val dekManager: DekManager,
    private val idleMonitor: AppIdleMonitor
) {
    private companion object {
        private const val TAG = "LockStateManager"
    }

    private val stateMutex = Mutex()
    private var currentTimeoutMs: Long = AppDefaults.Lock.DEFAULT_TIMEOUT_MS

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    /**
     * 标记认证成功，配置自动锁定定时器。
     */
    suspend fun markAuthorized() {
        stateMutex.withLock {
            if (_isAuthorized.value) return

            Logcat.i(TAG, "Marking authorized, configuring idle monitor")
            _isAuthorized.update { true }
            idleMonitor.configure(currentTimeoutMs) { lock() }
            idleMonitor.resetIdleTimer()
        }
    }

    /**
     * 锁定应用，清理敏感状态。
     */
    suspend fun lock() {
        stateMutex.withLock {
            Logcat.i(TAG, "Locking application")
            dekManager.lock()
            _isAuthorized.update { false }
            idleMonitor.cancel()
        }
    }

    /**
     * 更新锁定超时时间。
     */
    suspend fun updateTimeout(timeoutMs: Long) {
        stateMutex.withLock {
            currentTimeoutMs = timeoutMs
            idleMonitor.updateTimeout(timeoutMs)
            if (_isAuthorized.value) {
                idleMonitor.resetIdleTimer()
            }
        }
    }

    /**
     * 用户交互时重置空闲定时器。
     */
    fun onUserInteraction() {
        if (!_isAuthorized.value) return
        idleMonitor.resetIdleTimer()
    }

    /**
     * 检查并确保锁定状态一致。
     */
    suspend fun ensureLockedState() {
        stateMutex.withLock {
            if (_isAuthorized.value) return
            Logcat.i(TAG, "Ensuring locked state")
            dekManager.lock()
        }
    }
}