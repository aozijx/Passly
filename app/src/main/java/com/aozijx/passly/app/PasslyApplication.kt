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
import androidx.lifecycle.ProcessLifecycleOwner
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.diagnostics.DiagnosticsRuntime
import com.aozijx.passly.security.session.AppIdleMonitor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    lateinit var idleMonitor: AppIdleMonitor

    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    private val diagnosticsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyLog()
                    .build()
            )
        }

        super.onCreate()
        instance = this

        DiagnosticsRuntime.start(applicationContext, diagnosticsScope)

        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            com.aozijx.passly.core.diagnostics.AppLog.e(
                com.aozijx.passly.core.diagnostics.LogCategory.DATABASE,
                "sqlcipher.load_failed",
                throwable = e
            )
            throw e
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        registerGlobalTouchListener()
        configureAutofillServices()
    }

    /**
     * 双轨切换：根据 API 级别动态启用/禁用填充服务。
     *
     * - API >= 34：禁用 LegacyAutofillService，启用 ModernCredentialService
     * - API <  34：启用 LegacyAutofillService（ModernCredentialService 默认 disabled，无需处理）
     *
     * 使用 PackageManager.setComponentEnabledSetting 而非硬编码 enabled="true"，
     * 避免在低版本设备上触发 NoClassDefFoundError。
     */
    private fun configureAutofillServices() {
        val pm = packageManager
        val legacyComponent = ComponentName(
            this,
            "${packageName}.service.autofill.framework.LegacyAutofillService"
        )
        val modernComponent = ComponentName(
            this,
            "${packageName}.service.autofill.credential.ModernCredentialService"
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // API 34+：启用 Modern，禁用 Legacy
                pm.setComponentEnabledSetting(
                    modernComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    legacyComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                com.aozijx.passly.core.diagnostics.AppLog.i(
                    com.aozijx.passly.core.diagnostics.LogCategory.AUTOFILL,
                    "autofill.modern_enabled"
                )
            } else {
                // API 31-33：启用 Legacy
                pm.setComponentEnabledSetting(
                    legacyComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                com.aozijx.passly.core.diagnostics.AppLog.i(
                    com.aozijx.passly.core.diagnostics.LogCategory.AUTOFILL,
                    "autofill.legacy_enabled"
                )
            }
        } catch (e: Exception) {
            com.aozijx.passly.core.diagnostics.AppLog.e(
                com.aozijx.passly.core.diagnostics.LogCategory.AUTOFILL,
                "autofill.configure_failed",
                throwable = e
            )
        }
    }

    /** 监听所有 Activity 创建，注入全局触摸监听，确保所有页面的交互都能重置空闲计时器 */
    private fun registerGlobalTouchListener() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                setupIdleTouchListener(activity)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /** 为 Activity 注入触摸监听，重置空闲计时器 */
    private fun setupIdleTouchListener(activity: Activity) {
        activity.findViewById<ViewGroup>(android.R.id.content)
            ?.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    v.performClick()
                    idleMonitor.resetIdleTimer()
                }
                false
            }
    }
}
