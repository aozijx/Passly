package com.aozijx.passly.core.auth.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局空闲自动锁屏管理器
 *
 * 生命周期：@Singleton，与 App 进程共存。
 * 外部调用 [resetIdleTimer] 重置倒计时；超时触发 [onLockRequested] 回调。
 * 通过 [ProcessLifecycleOwner] 监听前后台切换：退后台立即锁定，回前台重启计时。
 */
@Singleton
class AppIdleMonitor @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var idleJob: Job? = null
    private var timeoutMs: Long = 30_000L
    private var onLockRequested: (() -> Unit)? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                idleJob?.cancel()
                onLockRequested?.invoke()
            }

            override fun onStart(owner: LifecycleOwner) {
                resetIdleTimer()
            }
        })
    }

    /** 配置超时时间和锁定回调，由 [AuthRepositoryImpl] 在认证成功时调用 */
    fun configure(timeoutMs: Long, onLock: () -> Unit) {
        this.timeoutMs = timeoutMs
        this.onLockRequested = onLock
    }

    /** 更新超时时间，由设置变更时调用 */
    fun updateTimeout(timeoutMs: Long) {
        this.timeoutMs = timeoutMs
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