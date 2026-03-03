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
        super.onCreate()
        instance = this
    }
}
