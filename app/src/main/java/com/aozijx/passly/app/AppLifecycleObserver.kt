package com.aozijx.passly.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aozijx.passly.core.diagnostics.DiagnosticsRuntime
import com.aozijx.passly.core.session.UnifiedSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 监听 ProcessLifecycleOwner 的前后台切换，管理数据库连接生命周期。
 *
 * - onStop：应用进入后台 → 关闭数据库连接
 * - onDestroy：应用销毁 → 关闭数据库连接
 *
 * 数据库连接**不**因锁定而关闭 —— 仅在此处受应用生命周期控制。
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    private val sessionManager: UnifiedSessionManager
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStart(owner: LifecycleOwner) {
        // 不再注册锁回调 —— DB 生命周期由 UnifiedSessionManager 独立管理
    }

    override fun onStop(owner: LifecycleOwner) {
        scope.launch {
            sessionManager.closeDatabase()
        }
        // 确保应用进入后台时，所有待写入的日志落盘，但不关闭线程池
        DiagnosticsRuntime.flush()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        scope.launch {
            sessionManager.closeDatabase()
        }
        DiagnosticsRuntime.shutdown()
    }
}
