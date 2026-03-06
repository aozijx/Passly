package com.example.poop

import android.app.Application
import com.example.poop.data.Preference

class AppContext : Application() {
    
    // 全局单例 Preference
    val preference: Preference by lazy { Preference(this) }

    companion object {
        private lateinit var instance: AppContext
        fun get(): AppContext = instance
    }

    override fun onCreate() {
        instance = this // 提前初始化，避免 Logcat 等工具类在 super.onCreate 中调用时报错
        super.onCreate()
    }
}
