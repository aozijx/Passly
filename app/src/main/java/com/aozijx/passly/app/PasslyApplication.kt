package com.aozijx.passly.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import androidx.lifecycle.ProcessLifecycleOwner
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.app.observability.LoggerRuntime
import com.aozijx.passly.app.observability.ObservabilityRuntimeController
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.port.AuthenticationRuntimeMaintenance
import com.aozijx.passly.domain.access.port.SessionActivityReporter
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PasslyApplication : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: PasslyApplication

        val context: Context
            get() = instance.applicationContext
    }

    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    @Inject
    lateinit var sessionActivityReporter: SessionActivityReporter

    @Inject
    lateinit var observabilityRuntimeController: ObservabilityRuntimeController

    @Inject
    lateinit var authenticationRuntimeMaintenance: AuthenticationRuntimeMaintenance

    @Inject
    lateinit var deviceLockController: DeviceLockController

    private val diagnosticsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()        // 检测主线程是否做了磁盘读操作
                    .detectDiskWrites()       // 检测主线程是否做了磁盘写操作
                    .detectNetwork()          // 检测主线程网络请求
                    .detectCustomSlowCalls()  // 检测自定义慢调用
                    .penaltyLog()             // 违规时只打印日志（Logcat），不闪退
                    // .penaltyDeath()        // 发现违规直接闪退（更严格，一般调试时才开）
                    .build(),
            )
        }

        super.onCreate()
        instance = this

        // 诊断与通知
        observabilityRuntimeController.start()

        // 生物识别轮换对账（后台执行）
        diagnosticsScope.launch { authenticationRuntimeMaintenance.reconcileStartupState() }

        // 加载 SQLCipher
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            LoggerRuntime.error(
                operation = Operations.ApplicationStartup,
                event = Events.Application.NativeLibraryLoadFailed,
                errorCode = ErrorCodes.NativeLibraryLoadFailed,
                reason = Reasons.NativeLibraryUnavailable,
                throwable = e,
            )
        }

        // 生命周期与全局交互监听
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        deviceLockController.start()
        registerGlobalTouchListener()
    }

    /** 监听所有 Activity 创建，在 Window 分发入口观察用户触摸。 */
    private fun registerGlobalTouchListener() {
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    installSessionActivityWindowCallback(activity)
                }

                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) =
                    Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    private fun installSessionActivityWindowCallback(activity: Activity) {
        val currentCallback = activity.window.callback ?: return
        if (currentCallback is SessionActivityWindowCallback) return
        activity.window.callback = SessionActivityWindowCallback(
            delegate = currentCallback,
            sessionActivityReporter = sessionActivityReporter,
        )
    }
}
