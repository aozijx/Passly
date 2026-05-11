package com.aozijx.passly.data.repository.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.data.mapper.SettingsMapper
import com.aozijx.passly.domain.repository.settings.BackupSettingsRepository
import com.aozijx.passly.domain.repository.settings.SecuritySettingsRepository
import com.aozijx.passly.domain.repository.settings.SystemSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "vault_settings")

class SettingsRepositoryImpl(context: Context) : SecuritySettingsRepository,
    SystemSettingsRepository, BackupSettingsRepository {
    private val appContext = context.applicationContext

    private companion object {
        const val DEFAULT_STYLE_KEY = -1

        val LOCK_TIMEOUT_KEY = longPreferencesKey("vault_lock_timeout")
        val BIOMETRIC_AUTH_KEY = booleanPreferencesKey("vault_biometric_auth")
        val INVALIDATE_KEY_ON_BIO_CHANGE_KEY =
            booleanPreferencesKey("security_invalidate_key_on_bio_change")
        val DARK_MODE_KEY = booleanPreferencesKey("vault_dark_mode")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("vault_dynamic_color")
        val SWIPE_ENABLED_KEY = booleanPreferencesKey("vault_swipe_enabled")
        val SWIPE_LEFT_ACTION_KEY = stringPreferencesKey("vault_swipe_left_action")
        val SWIPE_RIGHT_ACTION_KEY = stringPreferencesKey("vault_swipe_right_action")
        val AUTO_HIDE_STATUS_BAR_KEY = booleanPreferencesKey("ui_auto_hide_status_bar")
        val COLLAPSE_TOP_BAR_KEY = booleanPreferencesKey("ui_collapse_top_bar")
        val COLLAPSE_TAB_BAR_KEY = booleanPreferencesKey("ui_collapse_tab_bar")
        val SECURE_CONTENT_KEY = booleanPreferencesKey("ui_secure_content")
        val FLIP_TO_LOCK_KEY = booleanPreferencesKey("security_flip_to_lock")
        val FLIP_EXIT_AND_CLEAR_STACK_KEY =
            booleanPreferencesKey("security_flip_exit_and_clear_stack")
        val PASSWORD_PREFERRED_AUTH_FIRST_KEY =
            booleanPreferencesKey("security_password_preferred_auth_first")
        val DEVICE_CREDENTIAL_FALLBACK_KEY =
            booleanPreferencesKey("security_device_credential_fallback")
        val CARD_STYLE_KEY = stringPreferencesKey("ui_card_style")
        val CARD_STYLE_MAP_KEY = stringPreferencesKey("ui_card_style_map_v2")
        val AUTOFILL_UI_MODE_KEY = stringPreferencesKey("autofill_ui_mode")
        val BACKUP_DIRECTORY_URI_KEY = stringPreferencesKey("backup_directory_uri")
        val LAST_BACKUP_EXPORT_FILE_NAME_KEY = stringPreferencesKey("last_backup_export_file_name")
        val VISIBLE_VAULT_TABS_KEY = stringPreferencesKey("vault_visible_tabs")
        val AUTO_DOWNLOAD_ICONS_KEY = booleanPreferencesKey("data_auto_download_icons")
    }

    // --- SecuritySettingsRepository ---
    override val lockTimeout: Flow<Long> =
        appContext.settingsDataStore.data.map { it[LOCK_TIMEOUT_KEY] ?: 60000L }
    override val isBiometricEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[BIOMETRIC_AUTH_KEY] ?: true }
    override val isInvalidateKeyOnBioChange: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[INVALIDATE_KEY_ON_BIO_CHANGE_KEY] ?: true }
    override val isSecureContentEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[SECURE_CONTENT_KEY] ?: true }
    override val isFlipToLockEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[FLIP_TO_LOCK_KEY] ?: false }
    override val isFlipExitAndClearStackEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[FLIP_EXIT_AND_CLEAR_STACK_KEY] ?: false }
    override val isPasswordPreferredAuthFirst: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[PASSWORD_PREFERRED_AUTH_FIRST_KEY] ?: true }
    override val isDeviceCredentialFallbackEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[DEVICE_CREDENTIAL_FALLBACK_KEY] ?: true }

    override suspend fun setLockTimeout(timeoutMs: Long) {
        appContext.settingsDataStore.edit { it[LOCK_TIMEOUT_KEY] = timeoutMs }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[BIOMETRIC_AUTH_KEY] = enabled }
    }

    override suspend fun setInvalidateKeyOnBioChange(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[INVALIDATE_KEY_ON_BIO_CHANGE_KEY] = enabled }
    }

    override suspend fun setSecureContentEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[SECURE_CONTENT_KEY] = enabled }
    }

    override suspend fun setFlipToLockEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[FLIP_TO_LOCK_KEY] = enabled }
    }

    override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[FLIP_EXIT_AND_CLEAR_STACK_KEY] = enabled }
    }

    override suspend fun setPasswordPreferredAuthFirst(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[PASSWORD_PREFERRED_AUTH_FIRST_KEY] = enabled }
    }

    override suspend fun setDeviceCredentialFallbackEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[DEVICE_CREDENTIAL_FALLBACK_KEY] = enabled }
    }

    // --- SystemSettingsRepository ---
    override val isDarkMode: Flow<Boolean?> =
        appContext.settingsDataStore.data.map { it[DARK_MODE_KEY] }
    override val isDynamicColor: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[DYNAMIC_COLOR_KEY] ?: true }
    override val cardStyle: Flow<VaultCardStyle> = appContext.settingsDataStore.data.map { prefs ->
        val globalStyle =
            SettingsMapper.parseCardStyleMap(prefs[CARD_STYLE_MAP_KEY])[DEFAULT_STYLE_KEY]
                ?: VaultCardStyle.fromKey(prefs[CARD_STYLE_KEY])
        VaultCardStyle.normalizeGlobalStyle(globalStyle)
    }
    override val cardStyleByEntryType: Flow<Map<Int, VaultCardStyle>> =
        appContext.settingsDataStore.data.map { prefs ->
            val parsed = SettingsMapper.parseCardStyleMap(prefs[CARD_STYLE_MAP_KEY]).toMutableMap()
            if (parsed[DEFAULT_STYLE_KEY] == null) {
                parsed[DEFAULT_STYLE_KEY] =
                    VaultCardStyle.normalizeGlobalStyle(VaultCardStyle.fromKey(prefs[CARD_STYLE_KEY]))
            } else {
                parsed[DEFAULT_STYLE_KEY] =
                    VaultCardStyle.normalizeGlobalStyle(parsed[DEFAULT_STYLE_KEY]!!)
            }
            parsed.toMap()
        }
    override val isStatusBarAutoHide: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[AUTO_HIDE_STATUS_BAR_KEY] ?: true }
    override val isTopBarCollapsible: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[COLLAPSE_TOP_BAR_KEY] ?: true }
    override val isTabBarCollapsible: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[COLLAPSE_TAB_BAR_KEY] ?: true }
    override val isSwipeEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[SWIPE_ENABLED_KEY] ?: true }
    override val swipeLeftAction: Flow<SwipeActionType> = appContext.settingsDataStore.data.map {
        SwipeActionType.fromString(it[SWIPE_LEFT_ACTION_KEY] ?: SwipeActionType.COPY_PASSWORD.name)
    }
    override val swipeRightAction: Flow<SwipeActionType> = appContext.settingsDataStore.data.map {
        SwipeActionType.fromString(it[SWIPE_RIGHT_ACTION_KEY] ?: SwipeActionType.DETAIL.name)
    }
    override val autofillUiMode: Flow<AutofillUiMode> = appContext.settingsDataStore.data.map {
        AutofillUiMode.fromKey(it[AUTOFILL_UI_MODE_KEY])
    }
    override val visibleVaultTabs: Flow<Set<String>?> = appContext.settingsDataStore.data.map {
        SettingsMapper.decodeVisibleTabs(it[VISIBLE_VAULT_TABS_KEY])
    }
    override val isAutoDownloadIcons: Flow<Boolean> =
        appContext.settingsDataStore.data.map { it[AUTO_DOWNLOAD_ICONS_KEY] ?: true }

    override suspend fun setDarkMode(enabled: Boolean?) {
        appContext.settingsDataStore.edit {
            if (enabled == null) it.remove(DARK_MODE_KEY) else it[DARK_MODE_KEY] = enabled
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    override suspend fun setCardStyle(style: VaultCardStyle) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[CARD_STYLE_KEY] = style.key
            val map = SettingsMapper.parseCardStyleMap(prefs[CARD_STYLE_MAP_KEY]).toMutableMap()
            map[DEFAULT_STYLE_KEY] = style
            prefs[CARD_STYLE_MAP_KEY] = SettingsMapper.encodeCardStyleMap(map)
        }
    }

    override suspend fun setCardStyleForEntryType(entryTypeValue: Int, style: VaultCardStyle) {
        appContext.settingsDataStore.edit { prefs ->
            val map = SettingsMapper.parseCardStyleMap(prefs[CARD_STYLE_MAP_KEY]).toMutableMap()
            if (style == VaultCardStyle.DEFAULT) map.remove(entryTypeValue) else map[entryTypeValue] =
                style
            if (map[DEFAULT_STYLE_KEY] == null) map[DEFAULT_STYLE_KEY] =
                VaultCardStyle.normalizeGlobalStyle(VaultCardStyle.fromKey(prefs[CARD_STYLE_KEY]))
            prefs[CARD_STYLE_MAP_KEY] = SettingsMapper.encodeCardStyleMap(map)
        }
    }

    override suspend fun setStatusBarAutoHide(autoHide: Boolean) {
        appContext.settingsDataStore.edit { it[AUTO_HIDE_STATUS_BAR_KEY] = autoHide }
    }

    override suspend fun setTopBarCollapsible(collapsible: Boolean) {
        appContext.settingsDataStore.edit { it[COLLAPSE_TOP_BAR_KEY] = collapsible }
    }

    override suspend fun setTabBarCollapsible(collapsible: Boolean) {
        appContext.settingsDataStore.edit { it[COLLAPSE_TAB_BAR_KEY] = collapsible }
    }

    override suspend fun setSwipeEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[SWIPE_ENABLED_KEY] = enabled }
    }

    override suspend fun setSwipeLeftAction(action: SwipeActionType) {
        appContext.settingsDataStore.edit { it[SWIPE_LEFT_ACTION_KEY] = action.name }
    }

    override suspend fun setSwipeRightAction(action: SwipeActionType) {
        appContext.settingsDataStore.edit { it[SWIPE_RIGHT_ACTION_KEY] = action.name }
    }

    override suspend fun setAutofillUiMode(mode: AutofillUiMode) {
        appContext.settingsDataStore.edit { it[AUTOFILL_UI_MODE_KEY] = mode.key }
    }

    override suspend fun setVisibleVaultTabs(keys: Set<String>) {
        appContext.settingsDataStore.edit { it[VISIBLE_VAULT_TABS_KEY] = keys.joinToString(",") }
    }

    override suspend fun setAutoDownloadIcons(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[AUTO_DOWNLOAD_ICONS_KEY] = enabled }
    }

    // --- BackupSettingsRepository ---
    override val backupDirectoryUri: Flow<String?> =
        appContext.settingsDataStore.data.map { it[BACKUP_DIRECTORY_URI_KEY] }
    override val lastBackupExportFileName: Flow<String?> =
        appContext.settingsDataStore.data.map { it[LAST_BACKUP_EXPORT_FILE_NAME_KEY] }

    override suspend fun setBackupDirectoryUri(uri: String) {
        appContext.settingsDataStore.edit { it[BACKUP_DIRECTORY_URI_KEY] = uri }
    }

    override suspend fun clearBackupDirectoryUri() {
        appContext.settingsDataStore.edit { it.remove(BACKUP_DIRECTORY_URI_KEY) }
    }

    override suspend fun setLastBackupExportFileName(fileName: String) {
        appContext.settingsDataStore.edit { it[LAST_BACKUP_EXPORT_FILE_NAME_KEY] = fileName }
    }
}