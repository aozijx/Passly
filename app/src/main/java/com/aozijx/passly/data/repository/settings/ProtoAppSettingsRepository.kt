package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.codec.VaultSortSpecCodec
import com.aozijx.passly.data.local.datastore.appSettingsDataStore
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.AppSettingsSnapshot
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.AutofillUiMode
import com.aozijx.passly.domain.settings.model.BackupSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.NotificationSettings
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.model.VaultCardStyle
import com.aozijx.passly.domain.settings.model.VaultViewSettings
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtoAppSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) : AppSettingsRepository {

    private val dataStore = context.applicationContext.appSettingsDataStore

    private companion object {
        const val DEFAULT_STYLE_KEY = -1

        fun decodeCardStyles(encoded: Map<Int, String>): Map<Int, VaultCardStyle> =
            encoded.mapValues { (_, value) -> VaultCardStyle.fromKey(value) }

        fun encodeCardStyles(styles: Map<Int, VaultCardStyle>): Map<Int, String> =
            styles.mapValues { (_, value) -> value.key }
    }

    override val settings: Flow<AppSettingsSnapshot> =
        dataStore.data.map { proto ->
            AppSettingsSnapshot(
                appearance = AppearanceSettings(
                    isDarkMode = if (proto.hasDarkMode()) proto.darkMode else null,
                    isDynamicColor = proto.dynamicColor,
                    themeColor = proto.themeColor,
                    isStatusBarAutoHide = proto.autoHideStatusBar,
                    isTopBarCollapsible = proto.collapseTopBar,
                    isTabBarCollapsible = proto.collapseTabBar
                ),
                interaction = InteractionSettings(
                    isSwipeEnabled = proto.swipeEnabled,
                    swipeLeftAction = SwipeActionType.entries.find { it.name == proto.swipeLeftAction }
                        ?: SwipeActionType.COPY_PASSWORD,
                    swipeRightAction = SwipeActionType.entries.find { it.name == proto.swipeRightAction }
                        ?: SwipeActionType.DETAIL,
                    autofillUiMode = when (proto.autofillUiMode) {
                        "inline", "SYSTEM_INLINE" -> AutofillUiMode.SYSTEM_INLINE
                        "bottom_sheet", "BOTTOM_SHEET" -> AutofillUiMode.BOTTOM_SHEET
                        else -> AutofillUiMode.SYSTEM_INLINE
                    },
                    tabBarMaxTabsWithoutScroll = proto.tabBarMaxTabsWithoutScroll.coerceIn(2, 8),
                    isAutoDownloadIcons = proto.autoDownloadIcons,
                    faviconDownloadWhitelist = proto.faviconDownloadDomainList.toSet()
                ),
                security = SecuritySettings(
                    lockTimeout = proto.lockTimeoutMs,
                    isLockOnBackground = proto.lockOnBackground,
                    isInvalidateKeyOnBioChange = proto.invalidateKeyOnBioChange,
                    isSecureContentEnabled = proto.secureContent,
                    isFlipToLockEnabled = proto.flipToLock,
                    isFlipExitAndClearStackEnabled = proto.flipExitAndClearStack
                ),
                notifications = NotificationSettings(
                    statusBarNotificationsEnabled = proto.statusBarNotificationsEnabled,
                    iconDownloadNotificationsEnabled = proto.iconDownloadNotificationsEnabled,
                    clipboardClearToastsEnabled = proto.clipboardClearToastsEnabled,
                    appCloseToastsEnabled = proto.appCloseToastsEnabled
                ),
                vault = VaultViewSettings(
                    cardStyle = decodeCardStyles(proto.cardStyleByEntryTypeMap)[DEFAULT_STYLE_KEY]
                        ?: VaultCardStyle.fromKey(proto.cardStyle),
                    cardStyleByEntryType = {
                        val parsed = decodeCardStyles(proto.cardStyleByEntryTypeMap).toMutableMap()
                        if (parsed[DEFAULT_STYLE_KEY] == null) {
                            parsed[DEFAULT_STYLE_KEY] = VaultCardStyle.fromKey(proto.cardStyle)
                        }
                        parsed.toMap()
                    }(),
                    visibleVaultTabs = if (proto.visibleVaultTabsConfigured) proto.visibleVaultTabList.toSet() else null,
                    vaultSortOption = VaultSortSpecCodec.parse(proto.vaultSortOption)
                ),
                backup = BackupSettings(
                    backupDirectoryUri = proto.backupDirectoryUri.ifEmpty { null },
                    lastBackupExportFileName = proto.lastBackupExportFileName.ifEmpty { null }
                )
            )
        }

    override val lockTimeout: Flow<Long> =
        dataStore.data.map { it.lockTimeoutMs }

    override val isLockOnBackground: Flow<Boolean> =
        dataStore.data.map { it.lockOnBackground }

    override suspend fun update(command: SettingsCommand) {
        dataStore.updateData { proto ->
            val b = proto.toBuilder()
            when (command) {
                // Security
                is SettingsCommand.SetLockTimeout -> b.setLockTimeoutMs(command.timeoutMs)
                is SettingsCommand.SetLockOnBackground -> b.setLockOnBackground(command.enabled)
                is SettingsCommand.SetInvalidateKeyOnBioChange -> b.setInvalidateKeyOnBioChange(
                    command.enabled
                )

                is SettingsCommand.SetSecureContentEnabled -> b.setSecureContent(command.enabled)
                is SettingsCommand.SetFlipToLockEnabled -> b.setFlipToLock(command.enabled)
                is SettingsCommand.SetFlipExitAndClearStackEnabled -> b.setFlipExitAndClearStack(
                    command.enabled
                )

                // Appearance
                is SettingsCommand.SetDarkMode -> {
                    if (command.enabled == null) b.clearDarkMode()
                    else b.setDarkMode(command.enabled)
                }

                is SettingsCommand.SetDynamicColor -> b.setDynamicColor(command.enabled)
                is SettingsCommand.SetThemeColor -> b.setThemeColor(command.color)
                is SettingsCommand.SetStatusBarAutoHide -> b.setAutoHideStatusBar(command.enabled)
                is SettingsCommand.SetTopBarCollapsible -> b.setCollapseTopBar(command.enabled)
                is SettingsCommand.SetTabBarCollapsible -> b.setCollapseTabBar(command.enabled)

                // Interaction
                is SettingsCommand.SetSwipeEnabled -> b.setSwipeEnabled(command.enabled)
                is SettingsCommand.SetSwipeLeftAction -> b.setSwipeLeftAction(command.action.name)
                is SettingsCommand.SetSwipeRightAction -> b.setSwipeRightAction(command.action.name)
                is SettingsCommand.SetAutofillUiMode -> b.setAutofillUiMode(command.mode.name)
                is SettingsCommand.SetTabBarMaxTabsWithoutScroll -> {
                    val clamped = command.maxTabs.coerceIn(2, 8)
                    b.setTabBarMaxTabsWithoutScroll(clamped)
                }

                is SettingsCommand.SetAutoDownloadIcons -> b.setAutoDownloadIcons(command.enabled)
                is SettingsCommand.SetFaviconDownloadWhitelist -> {
                    b.clearFaviconDownloadDomain()
                    b.addAllFaviconDownloadDomain(command.whitelist.sorted())
                }

                // Vault
                is SettingsCommand.SetCardStyle -> {
                    val map = decodeCardStyles(proto.cardStyleByEntryTypeMap).toMutableMap()
                    map[DEFAULT_STYLE_KEY] = command.style
                    b.setCardStyle(command.style.key)
                        .clearCardStyleByEntryType()
                        .putAllCardStyleByEntryType(encodeCardStyles(map))
                }

                is SettingsCommand.SetCardStyleForEntryType -> {
                    val map = decodeCardStyles(proto.cardStyleByEntryTypeMap).toMutableMap()
                    if (command.style == VaultCardStyle.DEFAULT) map.remove(command.entryTypeValue)
                    else map[command.entryTypeValue] = command.style
                    if (map[DEFAULT_STYLE_KEY] == null) map[DEFAULT_STYLE_KEY] =
                        VaultCardStyle.fromKey(proto.cardStyle)
                    b.clearCardStyleByEntryType()
                        .putAllCardStyleByEntryType(encodeCardStyles(map))
                }

                is SettingsCommand.SetVisibleVaultTabs -> {
                    b.clearVisibleVaultTab()
                        .addAllVisibleVaultTab(command.keys.sorted())
                        .setVisibleVaultTabsConfigured(true)
                }

                is SettingsCommand.SetVaultSortOption -> {
                    b.setVaultSortOption(VaultSortSpecCodec.serialize(command.sort))
                }

                // Notifications
                is SettingsCommand.SetStatusBarNotificationsEnabled -> b.setStatusBarNotificationsEnabled(
                    command.enabled
                )

                is SettingsCommand.SetIconDownloadNotificationsEnabled -> b.setIconDownloadNotificationsEnabled(
                    command.enabled
                )

                is SettingsCommand.SetClipboardClearToastsEnabled -> b.setClipboardClearToastsEnabled(
                    command.enabled
                )

                is SettingsCommand.SetAppCloseToastsEnabled -> b.setAppCloseToastsEnabled(command.enabled)

                // Backup
                is SettingsCommand.SetBackupDirectoryUri -> b.setBackupDirectoryUri(command.uri)
                is SettingsCommand.ClearBackupDirectoryUri -> b.setBackupDirectoryUri("")
                is SettingsCommand.SetLastBackupExportFileName -> b.setLastBackupExportFileName(
                    command.fileName
                )
            }
            b.build()
        }
    }
}
