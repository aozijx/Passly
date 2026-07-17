package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.local.datastore.appSettingsDataStore
import com.aozijx.passly.domain.model.settings.AppDefaults
import com.aozijx.passly.domain.model.settings.AutofillUiMode
import com.aozijx.passly.domain.model.settings.SortOption
import com.aozijx.passly.domain.model.settings.SwipeActionType
import com.aozijx.passly.domain.model.settings.VaultCardStyle
import com.aozijx.passly.domain.repository.settings.PortableRepository
import com.aozijx.passly.domain.repository.settings.PortableSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortableRepositoryImpl @Inject constructor(@ApplicationContext context: Context) :
    PortableRepository {
    private val dataStore = context.applicationContext.appSettingsDataStore

    private companion object {
        const val DEFAULT_STYLE_KEY = -1

        fun decodeCardStyles(encoded: Map<Int, String>): Map<Int, VaultCardStyle> =
            encoded.mapValues { (_, value) -> VaultCardStyle.fromKey(value) }

        fun encodeCardStyles(styles: Map<Int, VaultCardStyle>): Map<Int, String> =
            styles.mapValues { (_, value) -> value.key }
    }

    override val isDarkMode: Flow<Boolean?> =
        dataStore.data.map { s -> if (s.hasDarkMode()) s.darkMode else null }
    override val isDynamicColor: Flow<Boolean> =
        dataStore.data.map { s ->
            if (s.hasDynamicColor()) s.dynamicColor
            else AppDefaults.Display.DYNAMIC_COLOR
        }
    override val cardStyle: Flow<VaultCardStyle> = dataStore.data.map { s ->
        decodeCardStyles(s.cardStyleByEntryTypeMap)[DEFAULT_STYLE_KEY]
            ?: VaultCardStyle.fromKey(s.cardStyle)
    }
    override val cardStyleByEntryType: Flow<Map<Int, VaultCardStyle>> =
        dataStore.data.map { s ->
            val parsed = decodeCardStyles(s.cardStyleByEntryTypeMap).toMutableMap()
            if (parsed[DEFAULT_STYLE_KEY] == null) {
                parsed[DEFAULT_STYLE_KEY] = VaultCardStyle.fromKey(s.cardStyle)
            }
            parsed.toMap()
        }
    override val isStatusBarAutoHide: Flow<Boolean> =
        dataStore.data.map { s ->
            if (s.hasAutoHideStatusBar()) s.autoHideStatusBar
            else AppDefaults.Display.STATUS_BAR_AUTO_HIDE
        }
    override val isTopBarCollapsible: Flow<Boolean> =
        dataStore.data.map { s ->
            if (s.hasCollapseTopBar()) s.collapseTopBar
            else AppDefaults.Display.TOP_BAR_COLLAPSIBLE
        }
    override val isTabBarCollapsible: Flow<Boolean> =
        dataStore.data.map { s ->
            if (s.hasCollapseTabBar()) s.collapseTabBar
            else AppDefaults.Display.TAB_BAR_COLLAPSIBLE
        }
    override val isSwipeEnabled: Flow<Boolean> =
        dataStore.data.map { s ->
            if (s.hasSwipeEnabled()) s.swipeEnabled
            else AppDefaults.Vault.SWIPE_ENABLED
        }
    override val swipeLeftAction: Flow<SwipeActionType> = dataStore.data.map { s ->
        SwipeActionType.entries.find { e -> e.name == s.swipeLeftAction }
            ?: AppDefaults.Vault.SWIPE_LEFT_ACTION
    }
    override val swipeRightAction: Flow<SwipeActionType> = dataStore.data.map { s ->
        SwipeActionType.entries.find { e -> e.name == s.swipeRightAction }
            ?: AppDefaults.Vault.SWIPE_RIGHT_ACTION
    }
    override val visibleVaultTabs: Flow<Set<String>?> = dataStore.data.map { s ->
        if (s.visibleVaultTabsConfigured) s.visibleVaultTabList.toSet() else null
    }
    override val autofillUiMode: Flow<AutofillUiMode> = dataStore.data.map { s ->
        when (s.autofillUiMode) {
            "inline", "SYSTEM_INLINE" -> AutofillUiMode.SYSTEM_INLINE
            "bottom_sheet", "BOTTOM_SHEET" -> AutofillUiMode.BOTTOM_SHEET
            else -> AppDefaults.Vault.AUTOFILL_UI_MODE
        }
    }
    override val tabBarMaxTabsWithoutScroll: Flow<Int> = dataStore.data.map { s ->
        val value = if (s.hasTabBarMaxTabsWithoutScroll()) {
            s.tabBarMaxTabsWithoutScroll
        } else {
            AppDefaults.Vault.TAB_BAR_MAX_TABS_WITHOUT_SCROLL
        }
        value.coerceIn(
            AppDefaults.Display.TAB_THRESHOLD_MIN,
            AppDefaults.Display.TAB_THRESHOLD_MAX
        )
    }
    override val isAutoDownloadIcons: Flow<Boolean> =
        dataStore.data.map { s ->
            if (s.hasAutoDownloadIcons()) s.autoDownloadIcons
            else AppDefaults.Display.AUTO_DOWNLOAD_ICONS
        }
    override val faviconDownloadWhitelist: Flow<Set<String>> =
        dataStore.data.map { s -> s.faviconDownloadDomainList.toSet() }
    override val vaultSortOption: Flow<SortOption> =
        dataStore.data.map { s ->
            SortOption.entries.find { it.name == s.vaultSortOption } ?: SortOption.DEFAULT
        }
    override val themeColor: Flow<String> =
        dataStore.data.map { s -> s.themeColor }

    override fun getSettingsFlow(): Flow<PortableSettings> = combine(
        combine(
            isDarkMode, isDynamicColor, themeColor, isStatusBarAutoHide
        ) { dm, dc, tc, sbah -> Group1(dm, dc, tc, sbah) },
        combine(
            isTopBarCollapsible, isTabBarCollapsible, visibleVaultTabs,
            cardStyle, cardStyleByEntryType
        ) { tbc, tabbc, vvt, cs, csbet -> Group2(tbc, tabbc, vvt, cs, csbet) },
        combine(
            isSwipeEnabled, swipeLeftAction, swipeRightAction, autofillUiMode,
            tabBarMaxTabsWithoutScroll
        ) { se, sla, sra, aum, tbmtws -> Group3(se, sla, sra, aum, tbmtws) },
        combine(
            isAutoDownloadIcons, faviconDownloadWhitelist, vaultSortOption
        ) { adi, fwl, vso -> Group4(adi, fwl, vso) }
    ) { g1, g2, g3, g4 ->
        PortableSettings(
            isDarkMode = g1.dm,
            isDynamicColor = g1.dc,
            themeColor = g1.tc,
            isStatusBarAutoHide = g1.sbah,
            isTopBarCollapsible = g2.tbc,
            isTabBarCollapsible = g2.tabbc,
            visibleVaultTabs = g2.vvt,
            cardStyle = g2.cs,
            cardStyleByEntryType = g2.csbet,
            isSwipeEnabled = g3.se,
            swipeLeftAction = g3.sla,
            swipeRightAction = g3.sra,
            autofillUiMode = g3.aum,
            tabBarMaxTabsWithoutScroll = g3.tbmtws,
            isAutoDownloadIcons = g4.adi,
            faviconDownloadWhitelist = g4.fwl,
            vaultSortOption = g4.vso
        )
    }

    private data class Group1(
        val dm: Boolean?, val dc: Boolean, val tc: String, val sbah: Boolean
    )

    private data class Group2(
        val tbc: Boolean, val tabbc: Boolean, val vvt: Set<String>?,
        val cs: VaultCardStyle, val csbet: Map<Int, VaultCardStyle>
    )

    private data class Group3(
        val se: Boolean, val sla: SwipeActionType, val sra: SwipeActionType,
        val aum: AutofillUiMode, val tbmtws: Int
    )

    private data class Group4(
        val adi: Boolean, val fwl: Set<String>, val vso: SortOption
    )

    override suspend fun setDarkMode(enabled: Boolean?) {
        dataStore.updateData { b ->
            if (enabled == null) b.toBuilder().clearDarkMode().build()
            else b.toBuilder().setDarkMode(enabled).build()
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setDynamicColor(enabled).build() }
    }

    override suspend fun setCardStyle(style: VaultCardStyle) {
        dataStore.updateData { s ->
            val map = decodeCardStyles(s.cardStyleByEntryTypeMap).toMutableMap()
            map[DEFAULT_STYLE_KEY] = style
            s.toBuilder()
                .setCardStyle(style.key)
                .clearCardStyleByEntryType()
                .putAllCardStyleByEntryType(encodeCardStyles(map))
                .build()
        }
    }

    override suspend fun setCardStyleForEntryType(entryTypeValue: Int, style: VaultCardStyle) {
        dataStore.updateData { s ->
            val map = decodeCardStyles(s.cardStyleByEntryTypeMap).toMutableMap()
            if (style == VaultCardStyle.DEFAULT) map.remove(entryTypeValue) else map[entryTypeValue] =
                style
            if (map[DEFAULT_STYLE_KEY] == null) map[DEFAULT_STYLE_KEY] =
                VaultCardStyle.fromKey(s.cardStyle)
            s.toBuilder()
                .clearCardStyleByEntryType()
                .putAllCardStyleByEntryType(encodeCardStyles(map))
                .build()
        }
    }

    override suspend fun setStatusBarAutoHide(autoHide: Boolean) {
        dataStore.updateData { it.toBuilder().setAutoHideStatusBar(autoHide).build() }
    }

    override suspend fun setTopBarCollapsible(collapsible: Boolean) {
        dataStore.updateData { it.toBuilder().setCollapseTopBar(collapsible).build() }
    }

    override suspend fun setTabBarCollapsible(collapsible: Boolean) {
        dataStore.updateData { it.toBuilder().setCollapseTabBar(collapsible).build() }
    }

    override suspend fun setSwipeEnabled(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setSwipeEnabled(enabled).build() }
    }

    override suspend fun setSwipeLeftAction(action: SwipeActionType) {
        dataStore.updateData { it.toBuilder().setSwipeLeftAction(action.name).build() }
    }

    override suspend fun setSwipeRightAction(action: SwipeActionType) {
        dataStore.updateData { it.toBuilder().setSwipeRightAction(action.name).build() }
    }

    override suspend fun setAutofillUiMode(mode: AutofillUiMode) {
        dataStore.updateData { it.toBuilder().setAutofillUiMode(mode.name).build() }
    }

    override suspend fun setVisibleVaultTabs(keys: Set<String>) {
        dataStore.updateData { settings ->
            settings.toBuilder()
                .clearVisibleVaultTab()
                .addAllVisibleVaultTab(keys.sorted())
                .setVisibleVaultTabsConfigured(true)
                .build()
        }
    }

    override suspend fun setTabBarMaxTabsWithoutScroll(maxTabs: Int) {
        dataStore.updateData {
            it.toBuilder().setTabBarMaxTabsWithoutScroll(
                maxTabs.coerceIn(
                    AppDefaults.Display.TAB_THRESHOLD_MIN,
                    AppDefaults.Display.TAB_THRESHOLD_MAX
                )
            ).build()
        }
    }

    override suspend fun setAutoDownloadIcons(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setAutoDownloadIcons(enabled).build() }
    }

    override suspend fun setFaviconDownloadWhitelist(whitelist: Set<String>) {
        dataStore.updateData { settings ->
            settings.toBuilder()
                .clearFaviconDownloadDomain()
                .addAllFaviconDownloadDomain(whitelist.sorted())
                .build()
        }
    }

    override suspend fun setVaultSortOption(sort: SortOption) {
        dataStore.updateData { it.toBuilder().setVaultSortOption(sort.name).build() }
    }

    override suspend fun setThemeColor(color: String) {
        dataStore.updateData { it.toBuilder().setThemeColor(color).build() }
    }
}
