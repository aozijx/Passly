package com.aozijx.passly.domain.settings.command

import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.model.NoticeTopic
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.ExportFormat
import com.aozijx.passly.domain.settings.model.FallbackPalette
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ImportMode
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.model.VaultSortSpec

sealed interface SettingsCommand {
    // Appearance
    data class SetThemeMode(val mode: ThemeMode) : SettingsCommand
    data class SetDynamicColor(val enabled: Boolean) : SettingsCommand
    data class SetFallbackPalette(val palette: FallbackPalette) : SettingsCommand

    /**
     * Selects a manual palette and disables dynamic color in the same settings transaction.
     * A null value selects the app's default static color scheme.
     */
    data class SelectManualThemeColor(val argb: Long?) : SettingsCommand
    data class SetLanguage(val language: AppLanguage) : SettingsCommand
    data class SetFontFamily(val mode: FontFamilyMode) : SettingsCommand
    data class SetExpressiveEnabled(val enabled: Boolean) : SettingsCommand

    // Interface (top bar, tab bar, status bar)
    data class SetHideSystemBars(val enabled: Boolean) : SettingsCommand
    data class SetTopBarCollapsible(val enabled: Boolean) : SettingsCommand
    data class SetTabBarCollapsible(val enabled: Boolean) : SettingsCommand
    data class SetOuterCornerRadius(val radiusDp: Float) : SettingsCommand
    data class SetInnerCornerRadius(val radiusDp: Float) : SettingsCommand
    data class SetGroupItemSpacing(val spacingDp: Float) : SettingsCommand
    data class SetGroupContentPadding(val paddingDp: Float) : SettingsCommand

    // Security
    data class SetSecureContentEnabled(val enabled: Boolean) : SettingsCommand
    data class SetFlipToLockEnabled(val enabled: Boolean) : SettingsCommand
    data class SetFlipExitAndClearStackEnabled(val enabled: Boolean) : SettingsCommand
    data class SetLockOnBackground(val enabled: Boolean) : SettingsCommand
    data class SetLockTimeout(val timeoutMs: Long) : SettingsCommand
    data class SetInvalidateBiometricKeyOnChange(val enabled: Boolean) : SettingsCommand
    data class SetReauthenticateSensitiveCopies(val enabled: Boolean) : SettingsCommand

    // Interaction
    data class SetSwipeEnabled(val enabled: Boolean) : SettingsCommand
    data class SetSwipeLeftAction(val action: SwipeActionType) : SettingsCommand
    data class SetSwipeRightAction(val action: SwipeActionType) : SettingsCommand
    data class SetAutofillEnabled(val enabled: Boolean) : SettingsCommand
    data class SetAutofillPresentation(val presentation: AutofillPresentation) : SettingsCommand
    data class SetCredentialManagerEnabled(val enabled: Boolean) : SettingsCommand
    data class SetAutofillAuthenticationRequired(val required: Boolean) : SettingsCommand
    data class SetAutofillOtpEnabled(val enabled: Boolean) : SettingsCommand
    data class SetAutofillSavePromptsEnabled(val enabled: Boolean) : SettingsCommand
    data class SetUnmatchedAutofillSuggestionsEnabled(val enabled: Boolean) : SettingsCommand
    data class SetAutofillMaxSuggestions(val count: Int) : SettingsCommand
    data class SetAutoDownloadIcons(val enabled: Boolean) : SettingsCommand
    data class SetFaviconDownloadWhitelist(val whitelist: Set<String>) : SettingsCommand

    // Vault
    data class SetMaxTabsWithoutScroll(val maxTabs: Int) : SettingsCommand
    data class SetVisibleVaultTabs(val keys: Set<String>) : SettingsCommand
    data object ClearVisibleVaultTabs : SettingsCommand
    data class SetVaultSortOption(val sort: VaultSortSpec) : SettingsCommand
    data class SetEntryCardPresentation(val presentation: EntryCardPresentation) : SettingsCommand
    data class RemoveEntryCardPresentation(val entryTypeKey: String) : SettingsCommand
    data class SetEntryHierarchyDisplayMode(
        val mode: EntryHierarchyDisplayMode
    ) : SettingsCommand

    // Messages
    data class SetOptionalMessagesEnabled(val enabled: Boolean) : SettingsCommand
    data class SetSystemNotificationsEnabled(val enabled: Boolean) : SettingsCommand
    data class SetMessageTopicEnabled(val topic: NoticeTopic, val enabled: Boolean) : SettingsCommand
    data class SetMessageTopicMinimumLevel(val topic: NoticeTopic, val level: NoticeLevel) : SettingsCommand

    // Backup
    data class SetBackupDirectoryUri(val uri: String) : SettingsCommand
    data object ClearBackupDirectoryUri : SettingsCommand
    data class SetDefaultExportFormat(val format: ExportFormat) : SettingsCommand
    data class SetIncludeIcons(val enabled: Boolean) : SettingsCommand
    data class SetIncludeAttachments(val enabled: Boolean) : SettingsCommand
    data class SetIncludeDeletedEntries(val enabled: Boolean) : SettingsCommand
    data class SetDefaultImportMode(val mode: ImportMode) : SettingsCommand
}
