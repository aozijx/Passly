package com.example.poop.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore by preferencesDataStore(name = "vault_settings")

class Preference(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        // 自动锁定超时时间 (毫秒)
        val LOCK_TIMEOUT_KEY = longPreferencesKey("vault_lock_timeout")
        // 是否开启生物识别验证
        val BIOMETRIC_AUTH_KEY = booleanPreferencesKey("vault_biometric_auth")
    }

    /**
     * 读取自动锁定超时时间，默认 60,000 毫秒 (1分钟)
     */
    val lockTimeout: Flow<Long> = appContext.vaultDataStore.data.map { preferences ->
        preferences[LOCK_TIMEOUT_KEY] ?: 60000L
    }

    /**
     * 设置自动锁定超时时间
     */
    suspend fun setLockTimeout(timeoutMs: Long) {
        appContext.vaultDataStore.edit { preferences ->
            preferences[LOCK_TIMEOUT_KEY] = timeoutMs
        }
    }

    /**
     * 读取生物识别开启状态，默认开启
     */
    val isBiometricEnabled: Flow<Boolean> = appContext.vaultDataStore.data.map { preferences ->
        preferences[BIOMETRIC_AUTH_KEY] ?: true
    }

    /**
     * 设置生物识别开启状态
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        appContext.vaultDataStore.edit { preferences ->
            preferences[BIOMETRIC_AUTH_KEY] = enabled
        }
    }
}