package com.aozijx.passly.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.ProcessLifecycleOwner
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.app.diagnostics.DiagnosticsRuntimeController
import com.aozijx.passly.app.message.contract.AppNoticePublisher
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.feature.autofill.credential.service.ModernCredentialService
import com.aozijx.passly.feature.autofill.legacy.service.LegacyAutofillService
import com.aozijx.passly.security.authentication.BiometricRotationReconciler
import com.aozijx.passly.security.authentication.VaultSessionController
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
    lateinit var authenticationManager: AuthenticationManager

    @Inject
    lateinit var sessionController: VaultSessionController

    @Inject
    lateinit var diagnosticsRuntimeController: DiagnosticsRuntimeController

    @Inject
    lateinit var appNoticePublisher: AppNoticePublisher

    @Inject
    lateinit var biometricRotationReconciler: BiometricRotationReconciler

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
                    .build()
            )
        }

        super.onCreate()
        instance = this

        // 诊断与通知
        diagnosticsRuntimeController.start(diagnosticsScope)
        ClipboardUtils.installNoticePublisher(appNoticePublisher)

        // 生物识别轮换对账（后台执行）
        diagnosticsScope.launch { biometricRotationReconciler.reconcile() }

        // 加载 SQLCipher
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            AppTelemetry.e(EventCategory.DATABASE, "sqlcipher.load_failed", throwable = e)
        }

        // 生命周期与全局交互监听
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        registerGlobalTouchListener()

        // Autofill 服务配置
        configureAutofillServices()
        enableModernCredentialService()
    }

    /**
     * - LegacyAutofillService：所有支持版本启用
     * - ModernCredentialService（CredentialProvider）：仅 API 34+ 启用，否则禁用
     */
    private fun configureAutofillServices() {
        val pm = packageManager
        val legacyComponent = ComponentName(this, LegacyAutofillService::class.java)
        try {
            // Legacy：始终启用
            pm.setComponentEnabledSetting(
                legacyComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            AppTelemetry.i(EventCategory.AUTOFILL, "autofill.legacy_enabled")
        } catch (e: Exception) {
            AppTelemetry.e(EventCategory.AUTOFILL, "autofill.configure_failed", throwable = e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun enableModernCredentialService() {
        val pm = packageManager
        val modernComponent = ComponentName(this, ModernCredentialService::class.java)
        try {
            pm.setComponentEnabledSetting(
                modernComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            AppTelemetry.i(EventCategory.AUTOFILL, "autofill.modern_enabled")
        } catch (e: Exception) {
            AppTelemetry.e(EventCategory.AUTOFILL, "autofill.modern_failed", throwable = e)
        }
    }

    /** 监听所有 Activity 创建，注入全局触摸监听，用于重置空闲计时器 */
    private fun registerGlobalTouchListener() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                setupIdleTouchListener(activity)
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    /** 为 Activity 注入触摸监听，仅重置空闲计时器（不触发 click） */
    private fun setupIdleTouchListener(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        content.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.performClick()
                sessionController.onUserInteraction()
            }
            false // 不消费事件，继续分发
        }
    }
}