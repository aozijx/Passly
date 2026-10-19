package com.aozijx.passly.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aozijx.passly.app.observability.ObservabilityRuntimeController
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.access.port.SessionVisibilityController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 监听 ProcessLifecycleOwner 的前后台切换，管理会话生命周期。
 *
 * - onStart/onStop：同步翻转传感器与应用前后台生命周期
 * - onStop：通知安全会话进入后台，由安全会话结合后台锁定开关与自动锁定时间调度
 * - onDestroy：应用销毁 → 封存会话
 *
 * 后台停留达到配置时间后触发完整的 [LockReason.BACKGROUND] 锁流程。
 * 回到前台时 UI 层会检测到状态变更并展示认证页面。
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    private val sessionLockController: SessionLockController,
    private val sessionVisibilityController: SessionVisibilityController,
    private val observabilityRuntime: ObservabilityRuntimeController,
    private val deviceLockController: DeviceLockController,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycleQueueLock = Any()
    private var lastLifecycleJob: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        sessionVisibilityController.onAppForeground()
        deviceLockController.onAppForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        deviceLockController.onAppBackground()
        sessionVisibilityController.onAppBackground()
        enqueueLifecycleOperation { observabilityRuntime.flush() }
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
