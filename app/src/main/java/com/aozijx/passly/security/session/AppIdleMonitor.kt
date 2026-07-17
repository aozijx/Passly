package com.aozijx.passly.security.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aozijx.passly.domain.repository.settings.IdleTimeoutSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局空闲自动锁屏管理器。
 *
 * 生命周期：@Singleton，与 App 进程共存。
 * 通过 [IdleTimeoutSettings] 监听 Settings 层提供的超时配置。
 * 通过 [ProcessLifecycleOwner] 监听前后台切换。
 *
 * 架构流向：Settings 管理 AutoLock 时长 → Session 监听 Flow<Long> → 超时触发锁定。
 */
@Singleton
class AppIdleMonitor @Inject constructor(
    private val idleTimeoutSettings: IdleTimeoutSettings
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var idleJob: Job? = null
    private var timeoutMs: Long = 30_000L
    private var onLockRequested: (suspend () -> Unit)? = null
    private var isLockOnBackground: Boolean = true

    init {
        scope.launch {
            idleTimeoutSettings.isLockOnBackground.collect {
                isLockOnBackground = it
            }
        }
        scope.launch {
            idleTimeoutSettings.lockTimeout.collect {
                timeoutMs = it
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                idleJob?.cancel()
                if (isLockOnBackground) {
                    scope.launch { onLockRequested?.invoke() }
                } else {
                    idleJob = scope.launch {
                        delay(timeoutMs)
                        onLockRequested?.invoke()
                    }
                }
            }

            override fun onStart(owner: LifecycleOwner) {
                idleJob?.cancel()
                resetIdleTimer()
            }
        })
    }

    /** 配置锁定回调，由 UserSessionManager 在认证成功时调用 */
    fun configure(onLock: suspend () -> Unit) {
        this.onLockRequested = onLock
    }

    /** 用户任意交互，重置倒计时 */
    fun resetIdleTimer() {
        if (onLockRequested == null) return
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(timeoutMs)
            onLockRequested?.invoke()
        }
    }

    /** 取消当前计时，认证锁定时调用 */
    fun cancel() {
        idleJob?.cancel()
    }
}