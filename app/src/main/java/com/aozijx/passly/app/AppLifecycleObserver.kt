package com.aozijx.passly.app

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.app.diagnostics.DiagnosticsRuntimeController
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.LockReason
import com.aozijx.passly.domain.settings.repository.IdleTimeoutSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 监听 ProcessLifecycleOwner 的前后台切换，管理会话生命周期。
 *
 * - onStop：应用进入后台 → 恢复模式强制锁定，普通会话按 [isLockOnBackground] 设置决定
 * - onDestroy：应用销毁 → 封存会话
 *
 * 前台到后台的切换将触发完整的 [LockReason.BACKGROUND] 锁流程，
 * 确保后台时不可访问数据库，且认证状态正确切换为 Locked。
 * 回到前台时 UI 层会检测到状态变更并展示认证页面。
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val diagnosticsRuntime: DiagnosticsRuntimeController,
    private val idleTimeoutSettings: IdleTimeoutSettings,
    @param:ApplicationContext private val context: Context,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "AppLifecycleObserver"

    override fun onStart(owner: LifecycleOwner) {
        // 前台无需额外操作，Auth page 会在认证状态为 Locked 时自动展示
    }

    override fun onStop(owner: LifecycleOwner) {
        ClipboardUtils.clearIfOwned(context)
        scope.launch {
            val lockOnBackground = idleTimeoutSettings.isLockOnBackground.first()
            val recoveryMode = authenticationManager.state.value is AuthenticationState.RecoveryMode
            if (!lockOnBackground && !recoveryMode) {
                AppTelemetry.i(tag, "Lock on background disabled by settings, skipping")
                return@launch
            }
            // 封存会话：排干租约 → 关闭数据库 → 同步认证状态
            authenticationManager.lock(LockReason.BACKGROUND)
        }
        // 确保应用进入后台时，所有待写入的日志落盘
        diagnosticsRuntime.flush()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        scope.launch {
            authenticationManager.lock(LockReason.APP_EXIT)
        }
        diagnosticsRuntime.shutdown()
    }
}
