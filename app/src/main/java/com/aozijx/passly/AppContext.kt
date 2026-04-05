package com.aozijx.passly

import android.app.Application
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.local.AppPrefs
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry

class AppContext : Application() {
    // 全局单例 VaultPrefs
    val preference: AppPrefs by lazy { AppPrefs(this) }

    companion object {
        private const val TAG = "AppContext"
        private var _instance: AppContext? = null
        fun get(): AppContext = _instance!!
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            Logcat.e(TAG, "Failed to load SQLCipher native library. Verify build.gradle SQLCipher dependency and target ABI .so files are packaged in the APK", e)
            throw e
        }
        EntryTypeStrategyRegistry.ensureRegistered()
    }
}
