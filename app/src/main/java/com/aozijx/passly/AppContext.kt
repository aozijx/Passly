package com.aozijx.passly

import android.app.Application
import android.os.StrictMode
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry
import kotlinx.coroutines.CoroutineExceptionHandler

class AppContext : Application() {

    companion object {
        private const val TAG = "AppContext"
        private var _instance: AppContext? = null
        fun get(): AppContext = checkNotNull(_instance) {
            "AppContext has not been initialized. " +
                "Ensure android:name=\".AppContext\" is declared in AndroidManifest.xml"
        }
    }

    val globalExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Logcat.e(TAG, "Uncaught coroutine exception", throwable)
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
        EntryTypeStrategyRegistry.ensureRegistered()
    }
}