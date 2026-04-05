package com.aozijx.passly

import android.app.Application
import com.aozijx.passly.data.local.AppPrefs
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry
import net.zetetic.database.sqlcipher.SQLiteDatabase

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
        SQLiteDatabase.loadLibs(this)
        EntryTypeStrategyRegistry.ensureRegistered()
    }
}
