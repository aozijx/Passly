package com.aozijx.passly.data.repository.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.aozijx.passly.data.mapper.SettingsMapper
import com.aozijx.passly.data.repository.settings.internal.AUTOFILL_UI_MODE_KEY
import com.aozijx.passly.data.repository.settings.internal.AUTO_DOWNLOAD_ICONS_KEY
import com.aozijx.passly.data.repository.settings.internal.AUTO_HIDE_STATUS_BAR_KEY
import com.aozijx.passly.data.repository.settings.internal.CARD_STYLE_KEY
import com.aozijx.passly.data.repository.settings.internal.CARD_STYLE_MAP_KEY
import com.aozijx.passly.data.repository.settings.internal.COLLAPSE_TAB_BAR_KEY
import com.aozijx.passly.data.repository.settings.internal.COLLAPSE_TOP_BAR_KEY
import com.aozijx.passly.data.repository.settings.internal.DARK_MODE_KEY
import com.aozijx.passly.data.repository.settings.internal.DEFAULT_STYLE_KEY
import com.aozijx.passly.data.repository.settings.internal.DYNAMIC_COLOR_KEY
import com.aozijx.passly.data.repository.settings.internal.SWIPE_ENABLED_KEY
import com.aozijx.passly.data.repository.settings.internal.SWIPE_LEFT_ACTION_KEY
import com.aozijx.passly.data.repository.settings.internal.SWIPE_RIGHT_ACTION_KEY
import com.aozijx.passly.data.repository.settings.internal.TAB_BAR_MAX_TABS_WITHOUT_SCROLL_KEY
import com.aozijx.passly.data.repository.settings.internal.VISIBLE_VAULT_TABS_KEY
import com.aozijx.passly.data.repository.settings.internal.settingsDataStore
import com.aozijx.passly.domain.config.AppDefaults
import com.aozijx.passly.domain.config.AutofillUiMode
import com.aozijx.passly.domain.config.UserConfig
import com.aozijx.passly.domain.config.UserConfig.Vault.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.domain.repository.settings.SystemSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemSettingsRepositoryImpl @Inject constructor(@ApplicationContext context: Context) :
    SystemSettingsRepository {
    private val appContext = context.applicationContext
    private val defaultConfig = UserConfig()

    override val isDarkMode: Flow<Boolean?> =
        appContext.settingsDataStore.data.map { it[DARK_MODE_KEY] }
    override val isDynamicColor: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[DYNAMIC_COLOR_KEY] ?: true
        }
    override val cardStyle: Flow<VaultCardStyle> = appContext.settingsDataStore.data.map { prefs ->
        val globalStyle =
            SettingsMapper.parseCardStyleMap(prefs[CARD_STYLE_MAP_KEY])[DEFAULT_STYLE_KEY]
                ?: VaultCardStyle.fromKey(prefs[CARD_STYLE_KEY])
        AppDefaults.CardStyle.normalizeGlobalStyle(globalStyle)
    }
    override val cardStyleByEntryType: Flow<Map<Int, VaultCardStyle>> =
        appContext.settingsDataStore.data.map { prefs ->
            val parsed = SettingsMapper.parseCardStyleMap(prefs[CARD_STYLE_MAP_KEY]).toMutableMap()
            if (parsed[DEFAULT_STYLE_KEY] == null) {
                parsed[DEFAULT_STYLE_KEY] =
                    AppDefaults.CardStyle.normalizeGlobalStyle(VaultCardStyle.fromKey(prefs[CARD_STYLE_KEY]))
            } else {
                parsed[DEFAULT_STYLE_KEY] =
                    AppDefaults.CardStyle.normalizeGlobalStyle(parsed[DEFAULT_STYLE_KEY]!!)
            }
            parsed.toMap()
        }
    override val isStatusBarAutoHide: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[AUTO_HIDE_STATUS_BAR_KEY] ?: defaultConfig.display.isStatusBarAutoHide
        }
    override val isTopBarCollapsible: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[COLLAPSE_TOP_BAR_KEY] ?: defaultConfig.display.isTopBarCollapsible
        }
    override val isTabBarCollapsible: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[COLLAPSE_TAB_BAR_KEY] ?: defaultConfig.display.isTabBarCollapsible
        }
    override val isSwipeEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[SWIPE_ENABLED_KEY] ?: defaultConfig.vault.isSwipeEnabled
        }
    override val swipeLeftAction: Flow<SwipeActionType> = appContext.settingsDataStore.data.map {
        SwipeActionType.entries.find { e -> e.name == it[SWIPE_LEFT_ACTION_KEY] }
            ?: defaultConfig.vault.swipeLeftAction
    }
    override val swipeRightAction: Flow<SwipeActionType> = appContext.settingsDataStore.data.map {
        SwipeActionType.entries.find { e -> e.name == it[SWIPE_RIGHT_ACTION_KEY] }
            ?: defaultConfig.vault.swipeRightAction
    }
    override val visibleVaultTabs: Flow<Set<String>?> = appContext.settingsDataStore.data.map {
        SettingsMapper.decodeVisibleTabs(it[VISIBLE_VAULT_TABS_KEY])
    }
    override val autofillUiMode: Flow<AutofillUiMode> = appContext.settingsDataStore.data.map {
        when (it[AUTOFILL_UI_MODE_KEY]) {
            "inline", "SYSTEM_INLINE" -> AutofillUiMode.SYSTEM_INLINE
            "bottom_sheet", "BOTTOM_SHEET" -> AutofillUiMode.BOTTOM_SHEET
            else -> defaultConfig.vault.autofillUiMode
        }
    }
    override val tabBarMaxTabsWithoutScroll: Flow<Int> = appContext.settingsDataStore.data.map {
        (it[TAB_BAR_MAX_TABS_WITHOUT_SCROLL_KEY] ?: defaultConfig.vault.tabBarMaxTabsWithoutScroll)
            .coerceIn(AppDefaults.TAB_THRESHOLD_MIN, AppDefaults.TAB_THRESHOLD_MAX)
    }
    override val isAutoDownloadIcons: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[AUTO_DOWNLOAD_ICONS_KEY] ?: defaultConfig.display.isAutoDownloadIcons
        }

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
                AppDefaults.CardStyle.normalizeGlobalStyle(VaultCardStyle.fromKey(prefs[CARD_STYLE_KEY]))
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
        appContext.settingsDataStore.edit { it[AUTOFILL_UI_MODE_KEY] = mode.name }
    }

    override suspend fun setVisibleVaultTabs(keys: Set<String>) {
        appContext.settingsDataStore.edit { it[VISIBLE_VAULT_TABS_KEY] = keys.joinToString(",") }
    }

    override suspend fun setTabBarMaxTabsWithoutScroll(maxTabs: Int) {
        appContext.settingsDataStore.edit {
            it[TAB_BAR_MAX_TABS_WITHOUT_SCROLL_KEY] = maxTabs.coerceIn(
                AppDefaults.TAB_THRESHOLD_MIN,
                AppDefaults.TAB_THRESHOLD_MAX
            )
        }
    }

    override suspend fun setAutoDownloadIcons(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[AUTO_DOWNLOAD_ICONS_KEY] = enabled }
    }
}