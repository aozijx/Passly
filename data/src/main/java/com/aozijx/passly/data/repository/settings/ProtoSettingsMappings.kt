package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.AppearancePreferences
import com.aozijx.passly.data.local.datastore.settings.AutofillPreferences
import com.aozijx.passly.data.local.datastore.settings.BackupPreferences
import com.aozijx.passly.data.local.datastore.settings.InteractionPreferences
import com.aozijx.passly.data.local.datastore.settings.InterfacePreferences
import com.aozijx.passly.data.local.datastore.settings.MessagePreferences
import com.aozijx.passly.data.local.datastore.settings.NoticeLevelProto
import com.aozijx.passly.data.local.datastore.settings.SecurityPreferences
import com.aozijx.passly.data.local.datastore.settings.TopicMessagePreference
import com.aozijx.passly.data.local.datastore.settings.VaultSortPreference
import com.aozijx.passly.data.local.datastore.settings.VaultViewPreferences
import com.aozijx.passly.data.message.model.AppMessageSettings
import com.aozijx.passly.data.message.model.NoticeLevel
import com.aozijx.passly.data.message.model.NoticeTopic
import com.aozijx.passly.data.message.model.TopicMessageSettings
import com.aozijx.passly.data.settings.model.AppLanguage
import com.aozijx.passly.data.settings.model.AppearanceSettings
import com.aozijx.passly.data.settings.model.AutofillPresentation
import com.aozijx.passly.data.settings.model.AutofillSettings
import com.aozijx.passly.data.settings.model.BackupSettings
import com.aozijx.passly.data.settings.model.CardDensity
import com.aozijx.passly.data.settings.model.EntryCardPresentation
import com.aozijx.passly.data.settings.model.EntryHierarchyDisplayMode
import com.aozijx.passly.data.settings.model.ExportFormat
import com.aozijx.passly.data.settings.model.FallbackPalette
import com.aozijx.passly.data.settings.model.FontFamilyMode
import com.aozijx.passly.data.settings.model.ImportMode
import com.aozijx.passly.data.settings.model.InteractionSettings
import com.aozijx.passly.data.settings.model.InterfaceSettings
import com.aozijx.passly.data.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.data.settings.model.SecuritySettings
import com.aozijx.passly.data.settings.model.SortDirection
import com.aozijx.passly.data.settings.model.SwipeActionType
import com.aozijx.passly.data.settings.model.ThemeMode
import com.aozijx.passly.data.settings.model.LibrarySortField
import com.aozijx.passly.data.settings.model.LibrarySortSpec
import com.aozijx.passly.data.settings.model.LibraryViewSettings
import com.aozijx.passly.data.settings.model.VisibleQuickFiltersConfig
import com.aozijx.passly.data.local.datastore.settings.CardDensity as ProtoCardDensity
import com.aozijx.passly.data.local.datastore.settings.EntryCardPresentation as ProtoEntryCardPresentation
import com.aozijx.passly.data.local.datastore.settings.FallbackPalette as ProtoFallbackPalette
import com.aozijx.passly.data.local.datastore.settings.ThemeMode as ProtoThemeMode

// ============================================================
// Proto ↔ Domain enum mappings (closed sets — Proto enum kept)
// ============================================================

// -- ThemeMode --
internal fun ProtoThemeMode.toDomain(): ThemeMode = when (this) {
    ProtoThemeMode.THEME_MODE_SYSTEM -> ThemeMode.SYSTEM
    ProtoThemeMode.THEME_MODE_LIGHT -> ThemeMode.LIGHT
    ProtoThemeMode.THEME_MODE_DARK -> ThemeMode.DARK
}

internal fun ThemeMode.toProto(): ProtoThemeMode = when (this) {
    ThemeMode.SYSTEM -> ProtoThemeMode.THEME_MODE_SYSTEM
    ThemeMode.LIGHT -> ProtoThemeMode.THEME_MODE_LIGHT
    ThemeMode.DARK -> ProtoThemeMode.THEME_MODE_DARK
}

// -- FallbackPalette --
internal fun ProtoFallbackPalette.toDomain(): FallbackPalette = when (this) {
    ProtoFallbackPalette.FALLBACK_PALETTE_BLUE -> FallbackPalette.BLUE
    ProtoFallbackPalette.FALLBACK_PALETTE_GREEN -> FallbackPalette.GREEN
    ProtoFallbackPalette.FALLBACK_PALETTE_RED -> FallbackPalette.RED
    ProtoFallbackPalette.FALLBACK_PALETTE_PURPLE -> FallbackPalette.PURPLE
    ProtoFallbackPalette.FALLBACK_PALETTE_ORANGE -> FallbackPalette.ORANGE
    ProtoFallbackPalette.FALLBACK_PALETTE_TEAL -> FallbackPalette.TEAL
    ProtoFallbackPalette.FALLBACK_PALETTE_PINK -> FallbackPalette.PINK
}

internal fun FallbackPalette.toProto(): ProtoFallbackPalette = when (this) {
    FallbackPalette.BLUE -> ProtoFallbackPalette.FALLBACK_PALETTE_BLUE
    FallbackPalette.GREEN -> ProtoFallbackPalette.FALLBACK_PALETTE_GREEN
    FallbackPalette.RED -> ProtoFallbackPalette.FALLBACK_PALETTE_RED
    FallbackPalette.PURPLE -> ProtoFallbackPalette.FALLBACK_PALETTE_PURPLE
    FallbackPalette.ORANGE -> ProtoFallbackPalette.FALLBACK_PALETTE_ORANGE
    FallbackPalette.TEAL -> ProtoFallbackPalette.FALLBACK_PALETTE_TEAL
    FallbackPalette.PINK -> ProtoFallbackPalette.FALLBACK_PALETTE_PINK
}

// -- CardDensity --
internal fun ProtoCardDensity.toDomain(): CardDensity = when (this) {
    ProtoCardDensity.CARD_DENSITY_COMPACT -> CardDensity.COMPACT
    ProtoCardDensity.CARD_DENSITY_STANDARD -> CardDensity.STANDARD
    ProtoCardDensity.CARD_DENSITY_COMFORTABLE -> CardDensity.COMFORTABLE
    ProtoCardDensity.CARD_DENSITY_UNSPECIFIED -> CardDensity.STANDARD
}

internal fun CardDensity.toProto(): ProtoCardDensity = when (this) {
    CardDensity.COMPACT -> ProtoCardDensity.CARD_DENSITY_COMPACT
    CardDensity.STANDARD -> ProtoCardDensity.CARD_DENSITY_STANDARD
    CardDensity.COMFORTABLE -> ProtoCardDensity.CARD_DENSITY_COMFORTABLE
}

// ============================================================
// String ↔ Domain mappings (open sets — Proto stores string ID)
// ============================================================

// -- AppLanguage --
internal fun String.toAppLanguageDomain(): AppLanguage = AppLanguage.fromLanguageTag(this)

// -- FontFamilyMode --
internal fun String.toFontFamilyDomain(): FontFamilyMode = when (this) {
    "system" -> FontFamilyMode.SYSTEM
    "app.default" -> FontFamilyMode.APP_BUNDLED
    else -> FontFamilyMode.APP_BUNDLED
}

internal fun FontFamilyMode.toFontFamilyString(): String = when (this) {
    FontFamilyMode.SYSTEM -> "system"
    FontFamilyMode.APP_BUNDLED -> "app.default"
}

// -- SwipeActionType --
internal fun String.toSwipeActionDomain(): SwipeActionType = when (this) {
    "delete" -> SwipeActionType.DELETE
    "detail" -> SwipeActionType.DETAIL
    "copy_username" -> SwipeActionType.COPY_USERNAME
    "copy_password" -> SwipeActionType.COPY_PASSWORD
    else -> SwipeActionType.COPY_PASSWORD
}

internal fun SwipeActionType.toSwipeActionString(): String = when (this) {
    SwipeActionType.DELETE -> "delete"
    SwipeActionType.DETAIL -> "detail"
    SwipeActionType.COPY_PASSWORD -> "copy_password"
    SwipeActionType.COPY_USERNAME -> "copy_username"
}

// -- Autofill presentation --
internal fun String.toAutofillPresentationDomain(): AutofillPresentation = when (this) {
    "bottom_sheet" -> AutofillPresentation.BOTTOM_SHEET
    else -> AutofillPresentation.SYSTEM_INLINE
}

internal fun AutofillPresentation.toStorageKey(): String = when (this) {
    AutofillPresentation.SYSTEM_INLINE -> "system_inline"
    AutofillPresentation.BOTTOM_SHEET -> "bottom_sheet"
}

// -- ExportFormat --
internal fun String.toExportFormatDomain(): ExportFormat = when (this) {
    "passly.encrypted" -> ExportFormat.ENCRYPTED
    "csv" -> ExportFormat.CSV
    "json" -> ExportFormat.JSON
    else -> ExportFormat.ENCRYPTED
}

internal fun ExportFormat.toExportFormatString(): String = when (this) {
    ExportFormat.ENCRYPTED -> "passly.encrypted"
    ExportFormat.CSV -> "csv"
    ExportFormat.JSON -> "json"
}

// -- ImportMode --
internal fun String.toImportModeDomain(): ImportMode = when (this) {
    "append" -> ImportMode.APPEND
    "replace" -> ImportMode.REPLACE
    "merge" -> ImportMode.MERGE
    else -> ImportMode.APPEND
}

internal fun ImportMode.toImportModeString(): String = when (this) {
    ImportMode.APPEND -> "append"
    ImportMode.REPLACE -> "replace"
    ImportMode.MERGE -> "merge"
}

// -- NoticeTopic --
internal fun String.toNoticeTopicDomain(): NoticeTopic = when (this) {
    "clipboard" -> NoticeTopic.CLIPBOARD
    "app_lifecycle" -> NoticeTopic.APP_LIFECYCLE
    "icon_download" -> NoticeTopic.ICON_DOWNLOAD
    "backup" -> NoticeTopic.BACKUP
    "security" -> NoticeTopic.SECURITY
    "database" -> NoticeTopic.DATABASE
    else -> NoticeTopic.CLIPBOARD
}

internal fun NoticeTopic.toNoticeTopicString(): String = when (this) {
    NoticeTopic.CLIPBOARD -> "clipboard"
    NoticeTopic.APP_LIFECYCLE -> "app_lifecycle"
    NoticeTopic.ICON_DOWNLOAD -> "icon_download"
    NoticeTopic.BACKUP -> "backup"
    NoticeTopic.SECURITY -> "security"
    NoticeTopic.DATABASE -> "database"
}

// -- NoticeLevel --
internal fun NoticeLevel.toProto(): NoticeLevelProto = when (this) {
    NoticeLevel.INFO -> NoticeLevelProto.NOTICE_LEVEL_INFO
    NoticeLevel.SUCCESS -> NoticeLevelProto.NOTICE_LEVEL_SUCCESS
    NoticeLevel.WARNING -> NoticeLevelProto.NOTICE_LEVEL_WARNING
    NoticeLevel.ERROR -> NoticeLevelProto.NOTICE_LEVEL_ERROR
    NoticeLevel.CRITICAL -> NoticeLevelProto.NOTICE_LEVEL_CRITICAL
}

internal fun NoticeLevelProto.toDomain(): NoticeLevel = when (this) {
    NoticeLevelProto.NOTICE_LEVEL_INFO -> NoticeLevel.INFO
    NoticeLevelProto.NOTICE_LEVEL_SUCCESS -> NoticeLevel.SUCCESS
    NoticeLevelProto.NOTICE_LEVEL_WARNING -> NoticeLevel.WARNING
    NoticeLevelProto.NOTICE_LEVEL_ERROR -> NoticeLevel.ERROR
    NoticeLevelProto.NOTICE_LEVEL_CRITICAL -> NoticeLevel.CRITICAL
}

// ============================================================
// VaultSortPreference ↔ LibrarySortSpec
// ============================================================

internal fun VaultSortPreference.toDomain(): LibrarySortSpec {
    val sortField = LibrarySortField.entries.find { it.name == field }
        ?: LibrarySortField.LAST_USED_AT
    val direction = if (descending) SortDirection.DESC else SortDirection.ASC
    val tieBreaker = LibrarySortField.entries.find { it.name == tieBreakerField }
        ?: LibrarySortField.ID
    return LibrarySortSpec(sortField, direction, pinFavorites, tieBreaker)
}

internal fun LibrarySortSpec.toProtoSort(): VaultSortPreference =
    VaultSortPreference.newBuilder()
        .setField(field.name)
        .setDescending(direction == SortDirection.DESC)
        .setPinFavorites(pinFavorites)
        .setTieBreakerField(tieBreaker.name)
        .build()

// ============================================================
// EntryCardPresentation proto ↔ domain
// ============================================================

internal fun ProtoEntryCardPresentation.toDomain(): EntryCardPresentation =
    EntryCardPresentation(
        entryTypeKey = entryTypeKey,
        variantKey = variantKey,
        density = density.toDomain(),
        showIcon = showIcon,
        showFavorite = showFavorite,
        showSecondaryText = showSecondaryText,
        showQuickAction = showQuickAction
    )

internal fun EntryCardPresentation.toProto(): ProtoEntryCardPresentation =
    ProtoEntryCardPresentation.newBuilder()
        .setEntryTypeKey(entryTypeKey)
        .setVariantKey(variantKey)
        .setDensity(density.toProto())
        .setShowIcon(showIcon)
        .setShowFavorite(showFavorite)
        .setShowSecondaryText(showSecondaryText)
        .setShowQuickAction(showQuickAction)
        .build()

// ============================================================
// MessagePreferences encode / decode
// ============================================================

internal fun decodeMessageSettings(proto: MessagePreferences?): AppMessageSettings {
    if (proto == null) return AppMessageSettings()
    val configured = proto.topicsList.associate { item ->
        item.topicKey.toNoticeTopicDomain() to TopicMessageSettings(
            enabled = item.enabled,
            minimumLevel = item.minimumLevel.toDomain()
        )
    }
    return AppMessageSettings(
        optionalMessagesEnabled = proto.optionalMessagesEnabled,
        systemNotificationsEnabled = proto.systemNotificationsEnabled,
        topicSettings = com.aozijx.passly.data.message.model.defaultTopicSettings() +
                configured
    )
}

internal fun encodeMessageSettings(settings: AppMessageSettings): MessagePreferences =
    MessagePreferences.newBuilder()
        .setOptionalMessagesEnabled(settings.optionalMessagesEnabled)
        .setSystemNotificationsEnabled(settings.systemNotificationsEnabled)
        .addAllTopics(
            NoticeTopic.entries.map { topic ->
                val value = settings.topic(topic)
                TopicMessagePreference.newBuilder()
                    .setTopicKey(topic.toNoticeTopicString())
                    .setEnabled(value.enabled)
                    .setMinimumLevel(value.minimumLevel.toProto())
                    .build()
            }
        )
        .build()

// ============================================================
// Proto Preferences → Domain Settings (read mapping)
// ============================================================

internal fun readAppearance(p: AppearancePreferences): AppearanceSettings =
    AppearanceSettings(
        themeMode = p.themeMode.toDomain(),
        isDynamicColor = p.dynamicColorEnabled,
        fallbackPalette = p.fallbackPalette.toDomain(),
        manualThemeColorArgb =
            if (p.hasManualThemeColorArgb()) p.manualThemeColorArgb else null,
        language = p.language.toAppLanguageDomain(),
        fontFamily = p.fontFamily.toFontFamilyDomain()
    )

internal fun readInterface(p: InterfacePreferences): InterfaceSettings =
    InterfaceSettings(
        hideSystemBars = p.hideSystemBars,
        collapseTopBarOnScroll = p.collapseTopBarOnScroll,
        collapseQuickFilterBarOnScroll = p.collapseQuickFilterBarOnScroll,
        outerCornerRadiusDp = p.outerCornerRadiusDp.coerceIn(
            InterfaceStyleConstraints.MIN_OUTER_RADIUS_DP,
            InterfaceStyleConstraints.MAX_OUTER_RADIUS_DP
        ),
        innerCornerRadiusDp = p.innerCornerRadiusDp.coerceIn(
            InterfaceStyleConstraints.MIN_INNER_RADIUS_DP,
            InterfaceStyleConstraints.MAX_INNER_RADIUS_DP
        ),
        groupItemSpacingDp = p.groupItemSpacingDp.coerceIn(
            InterfaceStyleConstraints.MIN_ITEM_SPACING_DP,
            InterfaceStyleConstraints.MAX_ITEM_SPACING_DP
        ),
        groupContentPaddingDp = p.groupContentPaddingDp.coerceIn(
            InterfaceStyleConstraints.MIN_CONTENT_PADDING_DP,
            InterfaceStyleConstraints.MAX_CONTENT_PADDING_DP
        )
    )

internal fun readSecurity(p: SecurityPreferences): SecuritySettings =
    SecuritySettings(
        isSecureContentEnabled = p.secureContentEnabled,
        isFlipToLockEnabled = p.flipToLockEnabled,
        isFlipExitAndClearStackEnabled = p.flipExitAndClearStack,
        isLockOnBackground = p.lockOnBackground,
        lockTimeout = p.lockTimeoutMs,
        isInvalidateBiometricKeyOnChange = p.invalidateBiometricKeyOnChange,
        reauthenticateSensitiveCopies = p.reauthenticateSensitiveCopies
    )

internal fun readInteraction(p: InteractionPreferences): InteractionSettings =
    InteractionSettings(
        isSwipeEnabled = p.swipeActionsEnabled,
        swipeLeftAction = p.swipeLeftAction.toSwipeActionDomain(),
        swipeRightAction = p.swipeRightAction.toSwipeActionDomain(),
        autofill = readAutofill(p.autofill),
        isAutoDownloadIcons = p.autoDownloadIcons,
        faviconDownloadWhitelist = p.faviconAllowedDomainsList.toSet()
    )

internal fun readAutofill(p: AutofillPreferences): AutofillSettings =
    AutofillSettings(
        enabled = p.enabled,
        presentation = p.presentation.toAutofillPresentationDomain(),
        credentialManagerEnabled = p.credentialManagerEnabled,
        requireAuthentication = p.requireAuthentication,
        includeOtp = p.includeOtp,
        savePromptsEnabled = p.savePromptsEnabled,
        allowUnmatchedSuggestions = p.allowUnmatchedSuggestions,
        maxSuggestions = p.maxSuggestions.coerceIn(
            AutofillSettings.MIN_SUGGESTIONS,
            AutofillSettings.MAX_SUGGESTIONS
        )
    )

internal fun readVault(p: VaultViewPreferences): LibraryViewSettings {
    return LibraryViewSettings(
        visibleQuickFilters = if (p.hasVisibleQuickFilters()) {
            VisibleQuickFiltersConfig(
                filterKeys = p.visibleQuickFilters.filterKeysList.toSet(),
                configured = p.visibleQuickFilters.configured
            )
        } else null,
        sort = if (p.hasSort()) p.sort.toDomain() else LibrarySortSpec.DEFAULT,
        entryCardPresentations = p.entryCardPresentationsList.map { it.toDomain() },
        entryHierarchyDisplayMode =
            EntryHierarchyDisplayMode.fromKey(p.entryHierarchyDisplayMode)
    )
}

internal fun readBackup(p: BackupPreferences): BackupSettings =
    BackupSettings(
        directoryTreeUri = p.directoryTreeUri.ifEmpty { null },
        defaultExportFormat = p.defaultExportFormat.toExportFormatDomain(),
        includeIcons = p.includeIcons,
        includeAttachments = p.includeAttachments,
        includeDeletedEntries = p.includeDeletedEntries,
        includedEntryTypes = p.includedEntryTypesList.toSet(),
        defaultImportMode = p.defaultImportMode.toImportModeDomain()
    )