package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.codec.VaultSortSpecCodec
import com.aozijx.passly.data.local.datastore.appSettingsDataStore
import com.aozijx.passly.data.local.datastore.settings.MessagePreferences
import com.aozijx.passly.data.local.datastore.settings.NoticeLevelProto
import com.aozijx.passly.data.local.datastore.settings.NoticeTopicProto
import com.aozijx.passly.data.local.datastore.settings.TopicMessagePreference
import com.aozijx.passly.domain.notice.model.AppMessageSettings
import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.model.NoticeTopic
import com.aozijx.passly.domain.notice.model.TopicMessageSettings
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.AppSettingsSnapshot
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.AutofillUiMode
import com.aozijx.passly.domain.settings.model.BackupSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
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

        fun NoticeTopic.toProto(): NoticeTopicProto = when (this) {
            NoticeTopic.CLIPBOARD -> NoticeTopicProto.NOTICE_TOPIC_CLIPBOARD
            NoticeTopic.APP_LIFECYCLE -> NoticeTopicProto.NOTICE_TOPIC_APP_LIFECYCLE
            NoticeTopic.ICON_DOWNLOAD -> NoticeTopicProto.NOTICE_TOPIC_ICON_DOWNLOAD
            NoticeTopic.BACKUP -> NoticeTopicProto.NOTICE_TOPIC_BACKUP
            NoticeTopic.SECURITY -> NoticeTopicProto.NOTICE_TOPIC_SECURITY
            NoticeTopic.DATABASE -> NoticeTopicProto.NOTICE_TOPIC_DATABASE
        }

        fun NoticeTopicProto.toDomain(): NoticeTopic = when (this) {
            NoticeTopicProto.NOTICE_TOPIC_CLIPBOARD -> NoticeTopic.CLIPBOARD
            NoticeTopicProto.NOTICE_TOPIC_APP_LIFECYCLE -> NoticeTopic.APP_LIFECYCLE
            NoticeTopicProto.NOTICE_TOPIC_ICON_DOWNLOAD -> NoticeTopic.ICON_DOWNLOAD
            NoticeTopicProto.NOTICE_TOPIC_BACKUP -> NoticeTopic.BACKUP
            NoticeTopicProto.NOTICE_TOPIC_SECURITY -> NoticeTopic.SECURITY
            NoticeTopicProto.NOTICE_TOPIC_DATABASE -> NoticeTopic.DATABASE
        }

        fun NoticeLevel.toProto(): NoticeLevelProto = when (this) {
            NoticeLevel.INFO -> NoticeLevelProto.NOTICE_LEVEL_INFO
            NoticeLevel.SUCCESS -> NoticeLevelProto.NOTICE_LEVEL_SUCCESS
            NoticeLevel.WARNING -> NoticeLevelProto.NOTICE_LEVEL_WARNING
            NoticeLevel.ERROR -> NoticeLevelProto.NOTICE_LEVEL_ERROR
            NoticeLevel.CRITICAL -> NoticeLevelProto.NOTICE_LEVEL_CRITICAL
        }

        fun NoticeLevelProto.toDomain(): NoticeLevel = when (this) {
            NoticeLevelProto.NOTICE_LEVEL_INFO -> NoticeLevel.INFO
            NoticeLevelProto.NOTICE_LEVEL_SUCCESS -> NoticeLevel.SUCCESS
            NoticeLevelProto.NOTICE_LEVEL_WARNING -> NoticeLevel.WARNING
            NoticeLevelProto.NOTICE_LEVEL_ERROR -> NoticeLevel.ERROR
            NoticeLevelProto.NOTICE_LEVEL_CRITICAL -> NoticeLevel.CRITICAL
        }

        fun decodeMessageSettings(proto: MessagePreferences?): AppMessageSettings {
            if (proto == null) return AppMessageSettings()
            val configured = proto.topicsList.associate { item ->
                item.topic.toDomain() to TopicMessageSettings(
                    enabled = item.enabled,
                    minimumLevel = item.minimumLevel.toDomain()
                )
            }
            return AppMessageSettings(
                optionalMessagesEnabled = proto.optionalMessagesEnabled,
                systemNotificationsEnabled = proto.systemNotificationsEnabled,
                topicSettings = com.aozijx.passly.domain.notice.model.defaultTopicSettings() +
                    configured
            )
        }

        fun encodeMessageSettings(settings: AppMessageSettings): MessagePreferences =
            MessagePreferences.newBuilder()
                .setOptionalMessagesEnabled(settings.optionalMessagesEnabled)
                .setSystemNotificationsEnabled(settings.systemNotificationsEnabled)
                .addAllTopics(
                    NoticeTopic.entries.map { topic ->
                        val value = settings.topic(topic)
                        TopicMessagePreference.newBuilder()
                            .setTopic(topic.toProto())
                            .setEnabled(value.enabled)
                            .setMinimumLevel(value.minimumLevel.toProto())
                            .build()
                    }
                )
                .build()
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
                messages = decodeMessageSettings(
                    proto.messagePreferences.takeIf { proto.hasMessagePreferences() }
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

                is SettingsCommand.SetOptionalMessagesEnabled -> {
                    val current = decodeMessageSettings(
                        proto.messagePreferences.takeIf { proto.hasMessagePreferences() }
                    )
                    b.setMessagePreferences(
                        encodeMessageSettings(
                            current.copy(optionalMessagesEnabled = command.enabled)
                        )
                    )
                }

                is SettingsCommand.SetSystemNotificationsEnabled -> {
                    val current = decodeMessageSettings(
                        proto.messagePreferences.takeIf { proto.hasMessagePreferences() }
                    )
                    b.setMessagePreferences(
                        encodeMessageSettings(
                            current.copy(systemNotificationsEnabled = command.enabled)
                        )
                    )
                }

                is SettingsCommand.SetMessageTopicEnabled -> {
                    val currentSettings = decodeMessageSettings(
                        proto.messagePreferences.takeIf { proto.hasMessagePreferences() }
                    )
                    val current = currentSettings.topicSettings.toMutableMap()
                    val existing = current[command.topic] ?: TopicMessageSettings()
                    current[command.topic] = existing.copy(enabled = command.enabled)
                    b.setMessagePreferences(
                        encodeMessageSettings(currentSettings.copy(topicSettings = current))
                    )
                }

                is SettingsCommand.SetMessageTopicMinimumLevel -> {
                    val currentSettings = decodeMessageSettings(
                        proto.messagePreferences.takeIf { proto.hasMessagePreferences() }
                    )
                    val current = currentSettings.topicSettings.toMutableMap()
                    val existing = current[command.topic] ?: TopicMessageSettings()
                    current[command.topic] = existing.copy(minimumLevel = command.level)
                    b.setMessagePreferences(
                        encodeMessageSettings(currentSettings.copy(topicSettings = current))
                    )
                }

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
