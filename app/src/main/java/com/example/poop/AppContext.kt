package com.example.poop

import android.app.Application

class AppContext : Application() {
    companion object {
        private lateinit var instance: AppContext
        fun get(): AppContext = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}