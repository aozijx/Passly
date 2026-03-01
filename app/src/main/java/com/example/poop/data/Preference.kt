package com.example.poop.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class Preference(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    }

    // 读取深色模式设置
    val isDarkMode: Flow<Boolean> = appContext.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    // 保存深色模式设置
    suspend fun setDarkMode(isDark: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDark
        }
    }

    // 读取动态颜色设置
    val isDynamicColor: Flow<Boolean> = appContext.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR_KEY] ?: true // 默认开启
    }

    // 保存动态颜色设置
    suspend fun setDynamicColor(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    // 读取通知启用状态
    val isNotificationsEnabled: Flow<Boolean> = appContext.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: false
    }

    // 保存通知启用状态
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }
}
