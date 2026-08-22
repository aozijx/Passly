package com.aozijx.passly.domain.settings.model

sealed interface SettingsCommand {
    // Appearance
    data class SetThemeMode(val mode: ThemeMode) : SettingsCommand
    data class SetDynamicColor(val enabled: Boolean) : SettingsCommand
    data class SetThemeKey(val key: String) : SettingsCommand
    data class SetCanvasTintPercent(val percent: Int) : SettingsCommand

    data class SetLanguage(val language: AppLanguage) : SettingsCommand
    data class SetFontFamily(val mode: FontFamilyMode) : SettingsCommand

    // Interface (top bar, quick-filter bar, status bar)
    data class SetHideSystemBars(val enabled: Boolean) : SettingsCommand
    data class SetTopBarCollapsible(val enabled: Boolean) : SettingsCommand
    data class SetQuickFilterBarCollapsible(val enabled: Boolean) : SettingsCommand
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

    // Library
    data class SetVisibleLibraryQuickFilters(val keys: Set<String>) : SettingsCommand
    data object ClearVisibleLibraryQuickFilters : SettingsCommand
    data class SetVaultSortOption(val sort: LibrarySortSpec) : SettingsCommand
    data class SetEntryCardPresentation(val presentation: EntryCardPresentation) : SettingsCommand
    data class RemoveEntryCardPresentation(val entryTypeKey: String) : SettingsCommand
    data class SetEntryHierarchyDisplayMode(
        val mode: EntryHierarchyDisplayMode
    ) : SettingsCommand

    // Messages
    data class SetOptionalMessagesEnabled(val enabled: Boolean) : SettingsCommand
    data class SetSystemNotificationsEnabled(val enabled: Boolean) : SettingsCommand
    data class SetMessageTopicEnabled(val topic: MessageTopic, val enabled: Boolean) : SettingsCommand
    data class SetMessageTopicMinimumLevel(val topic: MessageTopic, val level: MessageLevel) : SettingsCommand

    // Backup
    data class SetBackupDirectoryUri(val uri: String) : SettingsCommand
    data object ClearBackupDirectoryUri : SettingsCommand
    data class SetDefaultExportFormat(val format: ExportFormat) : SettingsCommand
    data class SetIncludeIcons(val enabled: Boolean) : SettingsCommand
    data class SetIncludeAttachments(val enabled: Boolean) : SettingsCommand
    data class SetIncludeDeletedEntries(val enabled: Boolean) : SettingsCommand
    data class SetDefaultImportMode(val mode: ImportMode) : SettingsCommand
}
