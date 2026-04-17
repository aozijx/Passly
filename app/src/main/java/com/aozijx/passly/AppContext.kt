package com.aozijx.passly

import android.app.Application
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.local.AppPrefs
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry

class AppContext : Application() {
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
            Logcat.e(TAG, "Failed to load SQLCipher native library.", e)
            throw e
        }
        EntryTypeStrategyRegistry.ensureRegistered()

        // 重要：在硬件级认证模式下，此处严禁执行 AppDatabase.preWarm。
        // 因为此时用户尚未认证，解密口令不可用，后台预热会导致应用启动即崩溃。
        Logcat.i(TAG, "Passly App Context initialized. Waiting for user authentication...")
    }
}