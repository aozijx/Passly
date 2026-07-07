package com.aozijx.passly.data.repository.settings.internal

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

internal val Context.settingsDataStore by preferencesDataStore(name = "vault_settings")

internal const val DEFAULT_STYLE_KEY = -1

internal val LOCK_TIMEOUT_KEY = longPreferencesKey("vault_lock_timeout")
internal val BIOMETRIC_AUTH_KEY = booleanPreferencesKey("vault_biometric_auth")
internal val INVALIDATE_KEY_ON_BIO_CHANGE_KEY =
    booleanPreferencesKey("security_invalidate_key_on_bio_change")
internal val DARK_MODE_KEY = booleanPreferencesKey("vault_dark_mode")
internal val DYNAMIC_COLOR_KEY = booleanPreferencesKey("vault_dynamic_color")
internal val SWIPE_ENABLED_KEY = booleanPreferencesKey("vault_swipe_enabled")
internal val SWIPE_LEFT_ACTION_KEY = stringPreferencesKey("vault_swipe_left_action")
internal val SWIPE_RIGHT_ACTION_KEY = stringPreferencesKey("vault_swipe_right_action")
internal val AUTO_HIDE_STATUS_BAR_KEY = booleanPreferencesKey("ui_auto_hide_status_bar")
internal val COLLAPSE_TOP_BAR_KEY = booleanPreferencesKey("ui_collapse_top_bar")
internal val COLLAPSE_TAB_BAR_KEY = booleanPreferencesKey("ui_collapse_tab_bar")
internal val SECURE_CONTENT_KEY = booleanPreferencesKey("ui_secure_content")
internal val FLIP_TO_LOCK_KEY = booleanPreferencesKey("security_flip_to_lock")
internal val FLIP_EXIT_AND_CLEAR_STACK_KEY =
    booleanPreferencesKey("security_flip_exit_and_clear_stack")
internal val LOCK_ON_BACKGROUND_KEY =
    booleanPreferencesKey("security_lock_on_background")
internal val CARD_STYLE_KEY = stringPreferencesKey("ui_card_style")
internal val CARD_STYLE_MAP_KEY = stringPreferencesKey("ui_card_style_map_v2")
internal val AUTOFILL_UI_MODE_KEY = stringPreferencesKey("autofill_ui_mode")
internal val BACKUP_DIRECTORY_URI_KEY = stringPreferencesKey("backup_directory_uri")
internal val LAST_BACKUP_EXPORT_FILE_NAME_KEY = stringPreferencesKey("last_backup_export_file_name")
internal val VISIBLE_VAULT_TABS_KEY = stringPreferencesKey("vault_visible_tabs")
internal val TAB_BAR_MAX_TABS_WITHOUT_SCROLL_KEY =
    intPreferencesKey("vault_tab_bar_max_tabs_without_scroll")
internal val AUTO_DOWNLOAD_ICONS_KEY = booleanPreferencesKey("data_auto_download_icons")
internal val FAVICON_DOWNLOAD_WHITELIST_KEY =
    stringPreferencesKey("data_favicon_download_whitelist")
internal val VAULT_SORT_OPTION_KEY = stringPreferencesKey("vault_sort_option")
internal val LANGUAGE_CODE_KEY = stringPreferencesKey("app_language_code")
internal val THEME_COLOR_KEY = stringPreferencesKey("app_theme_color")