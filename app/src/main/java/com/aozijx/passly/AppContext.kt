package com.aozijx.passly

import android.app.Application
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry

class AppContext : Application() {

    companion object {
        private const val TAG = "AppContext"
        private var _instance: AppContext? = null
        fun get(): AppContext = checkNotNull(_instance) {
            "AppContext has not been initialized. " +
                "Ensure android:name=\".AppContext\" is declared in AndroidManifest.xml"
        }
    }

    override fun onCreate() {
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