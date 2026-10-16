package com.aozijx.passly.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aozijx.passly.app.observability.ObservabilityRuntimeController
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 监听 ProcessLifecycleOwner 的前后台切换，管理会话生命周期。
 *
 * - onStart/onStop：同步翻转传感器与应用前后台生命周期
 * - onStop：恢复模式强制锁定，普通会话按 [com.aozijx.passly.domain.settings.port.IdleTimeoutSettings.isLockOnBackground] 设置决定
 * - onDestroy：应用销毁 → 封存会话
 *
 * 前台到后台的切换将触发完整的 [LockReason.BACKGROUND] 锁流程，
 * 确保后台时不可访问数据库，且认证状态正确切换为 Locked。
 * 回到前台时 UI 层会检测到状态变更并展示认证页面。
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
    private val sessionLockController: SessionLockController,
    private val observabilityRuntime: ObservabilityRuntimeController,
    private val idleTimeoutSettings: IdleTimeoutSettings,
    private val deviceLockController: DeviceLockController,
    private val logger: Logger,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycleQueueLock = Any()
    private var lastLifecycleJob: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        deviceLockController.onAppForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        deviceLockController.onAppBackground()
        enqueueLifecycleOperation {
            try {
                val lockOnBackground = idleTimeoutSettings.isLockOnBackground.first()
                val recoveryMode = secureSessionAccessState.isRecoveryMode()
                if (!lockOnBackground && !recoveryMode) {
                    logger.withRootTrace(Operations.ApplicationLifecycle) {
                        logger.log(LogStatements.Application.backgroundLockSkipped())
                    }
                } else {
                    // 封存会话：排干租约 → 关闭数据库 → 同步认证状态
                    sessionLockController.lock(
                        SessionLockRequest(LockReason.BACKGROUND, LockTrigger.APP_BACKGROUND),
                    )
                }
            } finally {
                // 等待锁流程结束再排干日志，避免生命周期尾部事件丢失。
                observabilityRuntime.flush()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        enqueueLifecycleOperation {
            try {
                sessionLockController.lock(
                    SessionLockRequest(LockReason.APP_EXIT, LockTrigger.APP_EXIT),
                )
            } finally {
                observabilityRuntime.shutdown()
            }
        }
    }

    private fun enqueueLifecycleOperation(operation: suspend () -> Unit) {
        synchronized(lifecycleQueueLock) {
            val predecessor = lastLifecycleJob
            lastLifecycleJob = scope.launch {
                predecessor?.join()
                operation()
            }
        }
    }
}
