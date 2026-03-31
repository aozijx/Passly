package com.example.poop.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore by preferencesDataStore(name = "vault_settings")

/**
 * 基于 DataStore 的偏好设置管理
 */
class AppPrefs(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        val LOCK_TIMEOUT_KEY = longPreferencesKey("vault_lock_timeout")
        val BIOMETRIC_AUTH_KEY = booleanPreferencesKey("vault_biometric_auth")
        val DARK_MODE_KEY = booleanPreferencesKey("vault_dark_mode") // null = 跟随系统
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("vault_dynamic_color")
        val SWIPE_ENABLED_KEY = booleanPreferencesKey("vault_swipe_enabled")
    }

    val lockTimeout: Flow<Long> = appContext.vaultDataStore.data.map { it[LOCK_TIMEOUT_KEY] ?: 60000L }
    val isBiometricEnabled: Flow<Boolean> = appContext.vaultDataStore.data.map { it[BIOMETRIC_AUTH_KEY] ?: true }
    
    // 深色模式：true, false, 或 null (跟随系统)
    val isDarkMode: Flow<Boolean?> = appContext.vaultDataStore.data.map { it[DARK_MODE_KEY] }
    
    // 动态颜色：默认开启 (Android 12+)
    val isDynamicColor: Flow<Boolean> = appContext.vaultDataStore.data.map { it[DYNAMIC_COLOR_KEY] ?: true }
    
    // 滑动操作相关设置
    val isSwipeEnabled: Flow<Boolean> = appContext.vaultDataStore.data.map { it[SWIPE_ENABLED_KEY] ?: true }

    suspend fun setLockTimeout(timeoutMs: Long) {
        appContext.vaultDataStore.edit { it[LOCK_TIMEOUT_KEY] = timeoutMs }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        appContext.vaultDataStore.edit { it[BIOMETRIC_AUTH_KEY] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean?) {
        appContext.vaultDataStore.edit { 
            if (enabled == null) it.remove(DARK_MODE_KEY) else it[DARK_MODE_KEY] = enabled 
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        appContext.vaultDataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }
}
