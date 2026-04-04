package com.aozijx.passly.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.common.ui.VaultCardStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore by preferencesDataStore(name = "vault_settings")

class AppPrefs(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        const val DEFAULT_STYLE_KEY = -1

        val LOCK_TIMEOUT_KEY = longPreferencesKey("vault_lock_timeout")
        val BIOMETRIC_AUTH_KEY = booleanPreferencesKey("vault_biometric_auth")
        val DARK_MODE_KEY = booleanPreferencesKey("vault_dark_mode")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("vault_dynamic_color")
        val SWIPE_ENABLED_KEY = booleanPreferencesKey("vault_swipe_enabled")
        val SWIPE_LEFT_ACTION_KEY = stringPreferencesKey("vault_swipe_left_action")
        val SWIPE_RIGHT_ACTION_KEY = stringPreferencesKey("vault_swipe_right_action")
        
        // --- 沉浸式折叠控制 ---
        val AUTO_HIDE_STATUS_BAR_KEY = booleanPreferencesKey("ui_auto_hide_status_bar")
        val COLLAPSE_TOP_BAR_KEY = booleanPreferencesKey("ui_collapse_top_bar")
        val COLLAPSE_TAB_BAR_KEY = booleanPreferencesKey("ui_collapse_tab_bar")
        
        // --- 安全增强 (合并：截屏保护 + 隐私屏) ---
        val SECURE_CONTENT_KEY = booleanPreferencesKey("ui_secure_content")

        // --- 传感器交互 ---
        val FLIP_TO_LOCK_KEY = booleanPreferencesKey("security_flip_to_lock")
        val FLIP_EXIT_AND_CLEAR_STACK_KEY = booleanPreferencesKey("security_flip_exit_and_clear_stack")

        // --- 列表卡片效果 ---
        val CARD_STYLE_KEY = stringPreferencesKey("ui_card_style")
        val CARD_STYLE_MAP_KEY = stringPreferencesKey("ui_card_style_map_v2")

        // --- 自动填充展示模式 ---
        val AUTOFILL_UI_MODE_KEY = stringPreferencesKey("autofill_ui_mode")
    }

    val isStatusBarAutoHide: Flow<Boolean> = appContext.vaultDataStore.data.map { it[AUTO_HIDE_STATUS_BAR_KEY] ?: true }
    val isTopBarCollapsible: Flow<Boolean> = appContext.vaultDataStore.data.map { it[COLLAPSE_TOP_BAR_KEY] ?: true }
    val isTabBarCollapsible: Flow<Boolean> = appContext.vaultDataStore.data.map { it[COLLAPSE_TAB_BAR_KEY] ?: true }
    val isSecureContentEnabled: Flow<Boolean> = appContext.vaultDataStore.data.map { it[SECURE_CONTENT_KEY] ?: true }
    val isFlipToLockEnabled: Flow<Boolean> = appContext.vaultDataStore.data.map { it[FLIP_TO_LOCK_KEY] ?: false }
    val isFlipExitAndClearStackEnabled: Flow<Boolean> = appContext.vaultDataStore.data.map { it[FLIP_EXIT_AND_CLEAR_STACK_KEY] ?: false }
    val cardStyle: Flow<VaultCardStyle> = appContext.vaultDataStore.data.map { prefs ->
        parseStyleMap(prefs[CARD_STYLE_MAP_KEY])[DEFAULT_STYLE_KEY]
            ?: VaultCardStyle.fromKey(prefs[CARD_STYLE_KEY])
    }
    val cardStyleByEntryType: Flow<Map<Int, VaultCardStyle>> = appContext.vaultDataStore.data.map { prefs ->
        val parsed = parseStyleMap(prefs[CARD_STYLE_MAP_KEY]).toMutableMap()
        if (parsed[DEFAULT_STYLE_KEY] == null) {
            parsed[DEFAULT_STYLE_KEY] = VaultCardStyle.fromKey(prefs[CARD_STYLE_KEY])
        }
        parsed.toMap()
    }
    val autofillUiMode: Flow<AutofillUiMode> = appContext.vaultDataStore.data.map { prefs ->
        AutofillUiMode.fromKey(prefs[AUTOFILL_UI_MODE_KEY])
    }

    // 原有设置
    val lockTimeout: Flow<Long> = appContext.vaultDataStore.data.map { it[LOCK_TIMEOUT_KEY] ?: 60000L }
    val isBiometricEnabled: Flow<Boolean> = appContext.vaultDataStore.data.map { it[BIOMETRIC_AUTH_KEY] ?: true }
    val isDarkMode: Flow<Boolean?> = appContext.vaultDataStore.data.map { it[DARK_MODE_KEY] }
    val isDynamicColor: Flow<Boolean> = appContext.vaultDataStore.data.map { it[DYNAMIC_COLOR_KEY] ?: true }
    val isSwipeEnabled: Flow<Boolean> = appContext.vaultDataStore.data.map { it[SWIPE_ENABLED_KEY] ?: true }
    val swipeLeftAction: Flow<SwipeActionType> = appContext.vaultDataStore.data.map { 
        SwipeActionType.fromString(it[SWIPE_LEFT_ACTION_KEY] ?: SwipeActionType.COPY_PASSWORD.name)
    }
    val swipeRightAction: Flow<SwipeActionType> = appContext.vaultDataStore.data.map { 
        SwipeActionType.fromString(it[SWIPE_RIGHT_ACTION_KEY] ?: SwipeActionType.DETAIL.name)
    }

    suspend fun setStatusBarAutoHide(autoHide: Boolean) = appContext.vaultDataStore.edit { it[AUTO_HIDE_STATUS_BAR_KEY] = autoHide }
    suspend fun setTopBarCollapsible(collapsible: Boolean) = appContext.vaultDataStore.edit { it[COLLAPSE_TOP_BAR_KEY] = collapsible }
    suspend fun setTabBarCollapsible(collapsible: Boolean) = appContext.vaultDataStore.edit { it[COLLAPSE_TAB_BAR_KEY] = collapsible }
    suspend fun setSecureContentEnabled(enabled: Boolean) = appContext.vaultDataStore.edit { it[SECURE_CONTENT_KEY] = enabled }
    suspend fun setFlipToLockEnabled(enabled: Boolean) = appContext.vaultDataStore.edit { it[FLIP_TO_LOCK_KEY] = enabled }
    suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) = appContext.vaultDataStore.edit { it[FLIP_EXIT_AND_CLEAR_STACK_KEY] = enabled }
    suspend fun setCardStyle(style: VaultCardStyle) = appContext.vaultDataStore.edit { prefs ->
        prefs[CARD_STYLE_KEY] = style.key
        val map = parseStyleMap(prefs[CARD_STYLE_MAP_KEY]).toMutableMap()
        map[DEFAULT_STYLE_KEY] = style
        prefs[CARD_STYLE_MAP_KEY] = encodeStyleMap(map)
    }
    suspend fun setCardStyleForEntryType(entryTypeValue: Int, style: VaultCardStyle) = appContext.vaultDataStore.edit { prefs ->
        val map = parseStyleMap(prefs[CARD_STYLE_MAP_KEY]).toMutableMap()
        if (style == VaultCardStyle.DEFAULT) {
            map.remove(entryTypeValue)
        } else {
            map[entryTypeValue] = style
        }
        if (map[DEFAULT_STYLE_KEY] == null) {
            map[DEFAULT_STYLE_KEY] = VaultCardStyle.fromKey(prefs[CARD_STYLE_KEY])
        }
        prefs[CARD_STYLE_MAP_KEY] = encodeStyleMap(map)
    }
    suspend fun setAutofillUiMode(mode: AutofillUiMode) = appContext.vaultDataStore.edit { it[AUTOFILL_UI_MODE_KEY] = mode.key }

    suspend fun setLockTimeout(timeoutMs: Long) = appContext.vaultDataStore.edit { it[LOCK_TIMEOUT_KEY] = timeoutMs }
    suspend fun setBiometricEnabled(enabled: Boolean) = appContext.vaultDataStore.edit { it[BIOMETRIC_AUTH_KEY] = enabled }
    suspend fun setDarkMode(enabled: Boolean?) = appContext.vaultDataStore.edit { if (enabled == null) it.remove(DARK_MODE_KEY) else it[DARK_MODE_KEY] = enabled }
    suspend fun setDynamicColor(enabled: Boolean) = appContext.vaultDataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    suspend fun setSwipeEnabled(enabled: Boolean) = appContext.vaultDataStore.edit { it[SWIPE_ENABLED_KEY] = enabled }
    suspend fun setSwipeLeftAction(action: SwipeActionType) = appContext.vaultDataStore.edit { it[SWIPE_LEFT_ACTION_KEY] = action.name }
    suspend fun setSwipeRightAction(action: SwipeActionType) = appContext.vaultDataStore.edit { it[SWIPE_RIGHT_ACTION_KEY] = action.name }

    private fun parseStyleMap(raw: String?): Map<Int, VaultCardStyle> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(";")
            .mapNotNull { token ->
                val parts = token.split(":")
                if (parts.size != 2) return@mapNotNull null
                val key = parts[0].toIntOrNull() ?: return@mapNotNull null
                key to VaultCardStyle.fromKey(parts[1])
            }
            .toMap()
    }

    private fun encodeStyleMap(map: Map<Int, VaultCardStyle>): String {
        return map.entries
            .sortedBy { it.key }
            .joinToString(";") { "${it.key}:${it.value.key}" }
    }
}



