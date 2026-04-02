package com.aozijx.passly.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * 通用设置，属于 main 源码集
 */
class AppPreference(private val context: Context) {

    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE_KEY] ?: false }
    suspend fun setDarkMode(isDark: Boolean) = context.dataStore.edit { it[DARK_MODE_KEY] = isDark }

    val isDynamicColor: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLOR_KEY] ?: true }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }

    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE_KEY] ?: "" }
    suspend fun setLanguage(lang: String) = context.dataStore.edit { it[LANGUAGE_KEY] = lang }

    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED_KEY] ?: false }
    suspend fun setNotificationsEnabled(enabled: Boolean) = context.dataStore.edit { it[NOTIFICATIONS_ENABLED_KEY] = enabled }
}
