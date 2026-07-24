package com.aozijx.passly.domain.settings.command

import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.model.NoticeTopic
import com.aozijx.passly.domain.settings.model.AutofillUiMode
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.model.VaultCardStyle
import com.aozijx.passly.domain.settings.model.VaultSortSpec

sealed interface SettingsCommand {
    // Security
    data class SetLockTimeout(val timeoutMs: Long) : SettingsCommand
    data class SetLockOnBackground(val enabled: Boolean) : SettingsCommand
    data class SetInvalidateKeyOnBioChange(val enabled: Boolean) : SettingsCommand
    data class SetSecureContentEnabled(val enabled: Boolean) : SettingsCommand
    data class SetFlipToLockEnabled(val enabled: Boolean) : SettingsCommand
    data class SetFlipExitAndClearStackEnabled(val enabled: Boolean) : SettingsCommand

    // Appearance
    data class SetDarkMode(val enabled: Boolean?) : SettingsCommand
    data class SetDynamicColor(val enabled: Boolean) : SettingsCommand
    data class SetThemeColor(val color: String) : SettingsCommand
    data class SetStatusBarAutoHide(val enabled: Boolean) : SettingsCommand
    data class SetTopBarCollapsible(val enabled: Boolean) : SettingsCommand
    data class SetTabBarCollapsible(val enabled: Boolean) : SettingsCommand

    // Interaction
    data class SetSwipeEnabled(val enabled: Boolean) : SettingsCommand
    data class SetSwipeLeftAction(val action: SwipeActionType) : SettingsCommand
    data class SetSwipeRightAction(val action: SwipeActionType) : SettingsCommand
    data class SetAutofillUiMode(val mode: AutofillUiMode) : SettingsCommand
    data class SetTabBarMaxTabsWithoutScroll(val maxTabs: Int) : SettingsCommand
    data class SetAutoDownloadIcons(val enabled: Boolean) : SettingsCommand
    data class SetFaviconDownloadWhitelist(val whitelist: Set<String>) : SettingsCommand

    // Vault
    data class SetCardStyle(val style: VaultCardStyle) : SettingsCommand
    data class SetCardStyleForEntryType(val entryTypeValue: Int, val style: VaultCardStyle) :
        SettingsCommand

    data class SetVisibleVaultTabs(val keys: Set<String>) : SettingsCommand
    data class SetVaultSortOption(val sort: VaultSortSpec) : SettingsCommand

    // Messages
    data class SetOptionalMessagesEnabled(val enabled: Boolean) : SettingsCommand
    data class SetSystemNotificationsEnabled(val enabled: Boolean) : SettingsCommand
    data class SetMessageTopicEnabled(val topic: NoticeTopic, val enabled: Boolean) :
        SettingsCommand

    data class SetMessageTopicMinimumLevel(val topic: NoticeTopic, val level: NoticeLevel) :
        SettingsCommand

    // Backup
    data class SetBackupDirectoryUri(val uri: String) : SettingsCommand
    class ClearBackupDirectoryUri : SettingsCommand
    data class SetLastBackupExportFileName(val fileName: String) : SettingsCommand
}
