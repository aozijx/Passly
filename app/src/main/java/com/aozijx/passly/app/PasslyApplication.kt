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
import com.aozijx.passly.core.auth.session.AppIdleMonitor
import com.aozijx.passly.core.log.CrashHandler
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.core.log.SensitiveDataFilter
import com.aozijx.passly.data.repository.settings.internal.LANGUAGE_CODE_KEY
import com.aozijx.passly.data.repository.settings.internal.settingsDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltAndroidApp
class PasslyApplication : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: PasslyApplication

        val logcat: Logcat
            get() = instance.logcatInstance

        val context: Context
            get() = instance.applicationContext

        /** 全局缓存的语言设置，避免在 Activity 的 attachBaseContext 中阻塞读取 DataStore */
        @Volatile
        var appLanguageCode: String = ""
            private set
    }

    @Inject
    lateinit var idleMonitor: AppIdleMonitor

    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    lateinit var logcatInstance: Logcat
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // 预加载语言设置，避免后续 Activity 启动时重复阻塞主线程
        // 设置较短的超时时间（200ms），防止 DataStore 初始化异常导致启动长时间黑屏
        appLanguageCode = runCatching {
            runBlocking(Dispatchers.IO) {
                withTimeout(200) {
                    base.settingsDataStore.data.first()[LANGUAGE_CODE_KEY] ?: ""
                }
            }
        }.getOrDefault("")
    }

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

        logcatInstance = Logcat(context = applicationContext, filter = SensitiveDataFilter())
        Logcat.init(logcatInstance)
        CrashHandler.init(logcatInstance)
        Logcat.clearOldLogs()

        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            Logcat.e("PasslyApplication", "Failed to load SQLCipher native library.", e)
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
            "${BuildConfig.APPLICATION_ID}.service.autofill.LegacyAutofillService"
        )
        val modernComponent = ComponentName(
            this,
            "${BuildConfig.APPLICATION_ID}.service.credential.ModernCredentialService"
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
                Logcat.i("PasslyApplication", "Autofill: ModernCredentialService enabled (API 34+)")
            } else {
                // API 31-33：启用 Legacy
                pm.setComponentEnabledSetting(
                    legacyComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                Logcat.i("PasslyApplication", "Autofill: LegacyAutofillService enabled (API < 34)")
            }
        } catch (e: Exception) {
            Logcat.e("PasslyApplication", "Failed to configure autofill services", e)
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