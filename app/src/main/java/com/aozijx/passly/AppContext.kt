package com.aozijx.passly

import android.app.Application
import com.example.passly.data.local.AppPrefs

class AppContext : Application() {
    // 全局单例 VaultPrefs
    val preference: AppPrefs by lazy { AppPrefs(this) }

    companion object {
        private var _instance: AppContext? = null
        fun get(): AppContext = _instance!!
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this
    }
}