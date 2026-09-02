package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.local.datastore.appSettingsDataStore
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppSettingsSnapshot
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.InterfaceSettingsSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ProtoAppSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : AppSettingsRepository, AppearanceSettingsRepository, InterfaceSettingsRepository {

    private val dataStore = context.applicationContext.appSettingsDataStore

    override val settings: Flow<AppSettingsSnapshot> =
        dataStore.data.map { proto ->
            AppSettingsSnapshot(
                appearance = readAppearance(proto.appearance),
                interfacePrefs = readInterface(proto.interfacePrefs),
                security = readSecurity(proto.security),
                interaction = readInteraction(proto.interaction),
                vault = readVault(proto.vaultView),
                messages = decodeMessageSettings(
                    proto.message.takeIf { proto.hasMessage() }
                ),
                backup = readBackup(proto.backup)
            )
        }

    override val appearance = dataStore.data.map { proto -> readAppearance(proto.appearance) }

    override val interfaceSettings: Flow<InterfaceSettingsSnapshot> =
        dataStore.data.map { proto ->
            val vault = readVault(proto.vaultView)
            InterfaceSettingsSnapshot(
                preferences = readInterface(proto.interfacePrefs),
                visibleLibraryQuickFilterKeys = vault.visibleQuickFilters?.filterKeys,
                entryHierarchyDisplayMode = vault.entryHierarchyDisplayMode,
            )
        }

    // ================================================================
    // Convenience flows
    // ================================================================

    override val lockTimeout: Flow<Long> =
        dataStore.data.map { proto ->
            if (proto.hasSecurity()) proto.security.lockTimeoutMs
            else 60000L
        }

    override val isLockOnBackground: Flow<Boolean> =
        dataStore.data.map { proto ->
            if (proto.hasSecurity()) proto.security.lockOnBackground
            else false
        }

    // ================================================================
    // update
    // ================================================================

    override suspend fun update(command: SettingsCommand) {
        dataStore.updateData { proto -> proto.applyCommand(command) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) =
        update(SettingsCommand.SetThemeMode(mode))
    override suspend fun setDynamicColor(enabled: Boolean) =
        update(SettingsCommand.SetDynamicColor(enabled))
    override suspend fun setThemeKey(key: String) =
        update(SettingsCommand.SetThemeKey(key))
    override suspend fun setCanvasTintPercent(percent: Int) =
        update(SettingsCommand.SetCanvasTintPercent(percent))
    override suspend fun setLanguage(language: AppLanguage) =
        update(SettingsCommand.SetLanguage(language))
    override suspend fun setFontFamily(mode: FontFamilyMode) =
        update(SettingsCommand.SetFontFamily(mode))
    override suspend fun setHideSystemBars(enabled: Boolean) =
        update(SettingsCommand.SetHideSystemBars(enabled))
    override suspend fun setTopBarCollapsible(enabled: Boolean) =
        update(SettingsCommand.SetTopBarCollapsible(enabled))
    override suspend fun setQuickFilterBarCollapsible(enabled: Boolean) =
        update(SettingsCommand.SetQuickFilterBarCollapsible(enabled))
    override suspend fun setOuterCornerRadius(radiusDp: Float) =
        update(SettingsCommand.SetOuterCornerRadius(radiusDp))
    override suspend fun setInnerCornerRadius(radiusDp: Float) =
        update(SettingsCommand.SetInnerCornerRadius(radiusDp))
    override suspend fun setGroupItemSpacing(spacingDp: Float) =
        update(SettingsCommand.SetGroupItemSpacing(spacingDp))
    override suspend fun setGroupContentPadding(paddingDp: Float) =
        update(SettingsCommand.SetGroupContentPadding(paddingDp))
    override suspend fun setVisibleLibraryQuickFilters(keys: Set<String>) =
        update(SettingsCommand.SetVisibleLibraryQuickFilters(keys))
    override suspend fun setEntryHierarchyDisplayMode(mode: EntryHierarchyDisplayMode) =
        update(SettingsCommand.SetEntryHierarchyDisplayMode(mode))
}
