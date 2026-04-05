package com.aozijx.passly

import android.app.Application
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.AppPrefs
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContext : Application() {
    // 全局单例 VaultPrefs
    val preference: AppPrefs by lazy { AppPrefs(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        // Pre-warm SQLCipher open/keying to reduce first vault screen latency.
        appScope.launch {
            runCatching { AppDatabase.preWarm(this@AppContext) }
                .onFailure { Logcat.e(TAG, "Database prewarm failed", it) }
        }
    }
}
