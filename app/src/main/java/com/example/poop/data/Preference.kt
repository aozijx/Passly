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
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
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
}
