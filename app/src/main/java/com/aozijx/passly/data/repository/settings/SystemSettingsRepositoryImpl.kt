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
import com.aozijx.passly.data.repository.settings.internal.VAULT_SORT_OPTION_KEY
import com.aozijx.passly.data.repository.settings.internal.VISIBLE_VAULT_TABS_KEY
import com.aozijx.passly.data.repository.settings.internal.settingsDataStore
import com.aozijx.passly.domain.AppDefaults
import com.aozijx.passly.domain.model.AutofillUiMode
import com.aozijx.passly.domain.model.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.domain.repository.settings.SystemSettingsRepository
import com.aozijx.passly.ui.features.vault.model.SortOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemSettingsRepositoryImpl @Inject constructor(@ApplicationContext context: Context) :
    SystemSettingsRepository {
    private val appContext = context.applicationContext

    override val isDarkMode: Flow<Boolean?> =
        appContext.settingsDataStore.data.map { it[DARK_MODE_KEY] }
    override val isDynamicColor: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[DYNAMIC_COLOR_KEY] ?: AppDefaults.DISPLAY_DYNAMIC_COLOR
        }
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
        appContext.settingsDataStore.data.map {
            it[AUTO_HIDE_STATUS_BAR_KEY] ?: AppDefaults.DISPLAY_STATUS_BAR_AUTO_HIDE
        }
    override val isTopBarCollapsible: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[COLLAPSE_TOP_BAR_KEY] ?: AppDefaults.DISPLAY_TOP_BAR_COLLAPSIBLE
        }
    override val isTabBarCollapsible: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[COLLAPSE_TAB_BAR_KEY] ?: AppDefaults.DISPLAY_TAB_BAR_COLLAPSIBLE
        }
    override val isSwipeEnabled: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[SWIPE_ENABLED_KEY] ?: AppDefaults.VAULT_SWIPE_ENABLED
        }
    override val swipeLeftAction: Flow<SwipeActionType> = appContext.settingsDataStore.data.map {
        SwipeActionType.entries.find { e -> e.name == it[SWIPE_LEFT_ACTION_KEY] }
            ?: AppDefaults.VAULT_SWIPE_LEFT_ACTION
    }
    override val swipeRightAction: Flow<SwipeActionType> = appContext.settingsDataStore.data.map {
        SwipeActionType.entries.find { e -> e.name == it[SWIPE_RIGHT_ACTION_KEY] }
            ?: AppDefaults.VAULT_SWIPE_RIGHT_ACTION
    }
    override val visibleVaultTabs: Flow<Set<String>?> = appContext.settingsDataStore.data.map {
        SettingsMapper.decodeVisibleTabs(it[VISIBLE_VAULT_TABS_KEY])
    }
    override val autofillUiMode: Flow<AutofillUiMode> = appContext.settingsDataStore.data.map {
        when (it[AUTOFILL_UI_MODE_KEY]) {
            "inline", "SYSTEM_INLINE" -> AutofillUiMode.SYSTEM_INLINE
            "bottom_sheet", "BOTTOM_SHEET" -> AutofillUiMode.BOTTOM_SHEET
            else -> AppDefaults.VAULT_AUTOFILL_UI_MODE
        }
    }
    override val tabBarMaxTabsWithoutScroll: Flow<Int> = appContext.settingsDataStore.data.map {
        (it[TAB_BAR_MAX_TABS_WITHOUT_SCROLL_KEY]
            ?: AppDefaults.VAULT_TAB_BAR_MAX_TABS_WITHOUT_SCROLL)
            .coerceIn(AppDefaults.TAB_THRESHOLD_MIN, AppDefaults.TAB_THRESHOLD_MAX)
    }
    override val isAutoDownloadIcons: Flow<Boolean> =
        appContext.settingsDataStore.data.map {
            it[AUTO_DOWNLOAD_ICONS_KEY] ?: AppDefaults.DISPLAY_AUTO_DOWNLOAD_ICONS
        }
    override val vaultSortOption: Flow<SortOption> =
        appContext.settingsDataStore.data.map { prefs ->
            prefs[VAULT_SORT_OPTION_KEY]?.let { SortOption.entries.find { s -> s.name == it } }
                ?: SortOption.DEFAULT
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
        appContext.settingsDataStore.edit { it[AUTOFILL_UI_MODE_KEY] = mode.name }
    }

    override suspend fun setVisibleVaultTabs(keys: Set<String>) {
        appContext.settingsDataStore.edit { it[VISIBLE_VAULT_TABS_KEY] = keys.joinToString(",") }
    }

    override suspend fun setTabBarMaxTabsWithoutScroll(maxTabs: Int) {
        appContext.settingsDataStore.edit {
            it[TAB_BAR_MAX_TABS_WITHOUT_SCROLL_KEY] =
                maxTabs.coerceIn(AppDefaults.TAB_THRESHOLD_MIN, AppDefaults.TAB_THRESHOLD_MAX)
        }
    }

    override suspend fun setAutoDownloadIcons(enabled: Boolean) {
        appContext.settingsDataStore.edit { it[AUTO_DOWNLOAD_ICONS_KEY] = enabled }
    }

    override suspend fun setVaultSortOption(sort: SortOption) {
        appContext.settingsDataStore.edit { it[VAULT_SORT_OPTION_KEY] = sort.name }
    }
}