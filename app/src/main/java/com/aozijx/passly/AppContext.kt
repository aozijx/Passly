package com.aozijx.passly

import android.app.Application
import android.os.StrictMode
import com.aozijx.passly.core.logging.Logcat
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppContext : Application() {

    /**
     * 仅用于 [Logcat] 等静态工具类。其他代码应通过 [dagger.hilt.android.qualifiers.ApplicationContext]
     * 注入 Application 或 AppContext。
     */
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
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            Logcat.e(TAG, "Failed to load SQLCipher native library.", e)
            throw e
        }
    }
}