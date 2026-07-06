package com.aozijx.passly

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.StrictMode
import android.view.MotionEvent
import android.view.ViewGroup
import com.aozijx.passly.core.auth.session.AppIdleMonitor
import com.aozijx.passly.core.logging.CrashHandler
import com.aozijx.passly.core.logging.Logcat
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AppContext : Application() {

    @Inject
    lateinit var idleMonitor: AppIdleMonitor

    companion object {
        private const val TAG = "AppContext"
        private var _instance: AppContext? = null

        @Deprecated(
            message = "Use Hilt injection (@ApplicationContext) instead of static access.",
            replaceWith = ReplaceWith(
                "dagger.hilt.android.qualifiers.ApplicationContext",
                "dagger.hilt.android.qualifiers.ApplicationContext"
            )
        )
        fun get(): AppContext = checkNotNull(_instance) {
            "AppContext has not been initialized."
        }
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
        _instance = this

        // 初始化崩溃处理器
        CrashHandler.init(this)

        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            Logcat.e(TAG, "Failed to load SQLCipher native library.", e)
            throw e
        }
        registerGlobalTouchListener()
    }

    /** 监听所有 Activity 创建，注入全局触摸监听，确保所有页面的交互都能重置空闲计时器 */
    private fun registerGlobalTouchListener() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.findViewById<ViewGroup>(android.R.id.content)
                    ?.setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_DOWN
                            || event.action == MotionEvent.ACTION_MOVE
                            || event.action == MotionEvent.ACTION_UP
                        ) {
                            if (event.action == MotionEvent.ACTION_UP) {
                                v.performClick()
                            }
                            idleMonitor.resetIdleTimer()
                        }
                        false
                    }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}