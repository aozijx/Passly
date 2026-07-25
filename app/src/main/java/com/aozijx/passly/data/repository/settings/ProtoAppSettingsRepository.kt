package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.local.datastore.appSettingsDataStore
import com.aozijx.passly.data.local.datastore.settings.AppearancePreferences
import com.aozijx.passly.data.local.datastore.settings.AutofillUiModeProto
import com.aozijx.passly.data.local.datastore.settings.BackupPreferences
import com.aozijx.passly.data.local.datastore.settings.InterfacePreferences
import com.aozijx.passly.data.local.datastore.settings.InteractionPreferences
import com.aozijx.passly.data.local.datastore.settings.MessagePreferences
import com.aozijx.passly.data.local.datastore.settings.NoticeLevelProto
import com.aozijx.passly.data.local.datastore.settings.NoticeTopicProto
import com.aozijx.passly.data.local.datastore.settings.SecurityPreferences
import com.aozijx.passly.data.local.datastore.settings.SwipeActionProto
import com.aozijx.passly.data.local.datastore.settings.TopicMessagePreference
import com.aozijx.passly.data.local.datastore.settings.VaultViewPreferences
import com.aozijx.passly.data.local.datastore.settings.VisibleTabs
import com.aozijx.passly.data.local.datastore.settings.VaultSortPreference
import com.aozijx.passly.data.local.datastore.settings.ThemeMode as ProtoThemeMode
import com.aozijx.passly.data.local.datastore.settings.FallbackPalette as ProtoFallbackPalette
import com.aozijx.passly.data.local.datastore.settings.AppLanguage as ProtoAppLanguage
import com.aozijx.passly.data.local.datastore.settings.FontFamilyMode as ProtoFontFamilyMode
import com.aozijx.passly.data.local.datastore.settings.CardDensity as ProtoCardDensity
import com.aozijx.passly.data.local.datastore.settings.ExportFormat as ProtoExportFormat
import com.aozijx.passly.data.local.datastore.settings.ImportMode as ProtoImportMode
import com.aozijx.passly.data.local.datastore.settings.EntryCardPresentation as ProtoEntryCardPresentation
import com.aozijx.passly.domain.notice.model.AppMessageSettings
import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.model.NoticeTopic
import com.aozijx.passly.domain.notice.model.TopicMessageSettings
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppSettingsSnapshot
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.AutofillUiMode
import com.aozijx.passly.domain.settings.model.BackupSettings
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.ExportFormat
import com.aozijx.passly.domain.settings.model.FallbackPalette
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ImportMode
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.model.SortDirection
import com.aozijx.passly.domain.settings.model.VaultSortField
import com.aozijx.passly.domain.settings.model.VaultSortSpec
import com.aozijx.passly.domain.settings.model.VaultViewSettings
import com.aozijx.passly.domain.settings.model.VisibleTabsConfig
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

        // ============================================================
        // Proto ↔ Domain enum mappings
        // ============================================================

        // -- ThemeMode --
        fun ProtoThemeMode.toDomain(): ThemeMode = when (this) {
            ProtoThemeMode.THEME_MODE_SYSTEM -> ThemeMode.SYSTEM
            ProtoThemeMode.THEME_MODE_LIGHT -> ThemeMode.LIGHT
            ProtoThemeMode.THEME_MODE_DARK -> ThemeMode.DARK
        }

        fun ThemeMode.toProto(): ProtoThemeMode = when (this) {
            ThemeMode.SYSTEM -> ProtoThemeMode.THEME_MODE_SYSTEM
            ThemeMode.LIGHT -> ProtoThemeMode.THEME_MODE_LIGHT
            ThemeMode.DARK -> ProtoThemeMode.THEME_MODE_DARK
        }

        // -- FallbackPalette --
        fun ProtoFallbackPalette.toDomain(): FallbackPalette = when (this) {
            ProtoFallbackPalette.FALLBACK_PALETTE_BLUE -> FallbackPalette.BLUE
            ProtoFallbackPalette.FALLBACK_PALETTE_GREEN -> FallbackPalette.GREEN
            ProtoFallbackPalette.FALLBACK_PALETTE_RED -> FallbackPalette.RED
            ProtoFallbackPalette.FALLBACK_PALETTE_PURPLE -> FallbackPalette.PURPLE
            ProtoFallbackPalette.FALLBACK_PALETTE_ORANGE -> FallbackPalette.ORANGE
            ProtoFallbackPalette.FALLBACK_PALETTE_TEAL -> FallbackPalette.TEAL
            ProtoFallbackPalette.FALLBACK_PALETTE_PINK -> FallbackPalette.PINK
        }

        fun FallbackPalette.toProto(): ProtoFallbackPalette = when (this) {
            FallbackPalette.BLUE -> ProtoFallbackPalette.FALLBACK_PALETTE_BLUE
            FallbackPalette.GREEN -> ProtoFallbackPalette.FALLBACK_PALETTE_GREEN
            FallbackPalette.RED -> ProtoFallbackPalette.FALLBACK_PALETTE_RED
            FallbackPalette.PURPLE -> ProtoFallbackPalette.FALLBACK_PALETTE_PURPLE
            FallbackPalette.ORANGE -> ProtoFallbackPalette.FALLBACK_PALETTE_ORANGE
            FallbackPalette.TEAL -> ProtoFallbackPalette.FALLBACK_PALETTE_TEAL
            FallbackPalette.PINK -> ProtoFallbackPalette.FALLBACK_PALETTE_PINK
        }

        // -- AppLanguage --
        fun ProtoAppLanguage.toDomain(): AppLanguage = when (this) {
            ProtoAppLanguage.APP_LANGUAGE_SYSTEM -> AppLanguage.SYSTEM
            ProtoAppLanguage.APP_LANGUAGE_ZH -> AppLanguage.ZH
            ProtoAppLanguage.APP_LANGUAGE_EN -> AppLanguage.EN
        }

        fun AppLanguage.toProto(): ProtoAppLanguage = when (this) {
            AppLanguage.SYSTEM -> ProtoAppLanguage.APP_LANGUAGE_SYSTEM
            AppLanguage.ZH -> ProtoAppLanguage.APP_LANGUAGE_ZH
            AppLanguage.EN -> ProtoAppLanguage.APP_LANGUAGE_EN
        }

        // -- FontFamilyMode --
        fun ProtoFontFamilyMode.toDomain(): FontFamilyMode = when (this) {
            ProtoFontFamilyMode.FONT_FAMILY_SYSTEM -> FontFamilyMode.SYSTEM
            ProtoFontFamilyMode.FONT_FAMILY_APP_BUNDLED -> FontFamilyMode.APP_BUNDLED
        }

        fun FontFamilyMode.toProto(): ProtoFontFamilyMode = when (this) {
            FontFamilyMode.SYSTEM -> ProtoFontFamilyMode.FONT_FAMILY_SYSTEM
            FontFamilyMode.APP_BUNDLED -> ProtoFontFamilyMode.FONT_FAMILY_APP_BUNDLED
        }

        // -- SwipeActionProto → SwipeActionType --
        fun SwipeActionProto.toDomain(): SwipeActionType = when (this) {
            SwipeActionProto.SWIPE_ACTION_NONE -> SwipeActionType.COPY_PASSWORD
            SwipeActionProto.SWIPE_ACTION_COPY_USERNAME -> SwipeActionType.COPY_USERNAME
            SwipeActionProto.SWIPE_ACTION_COPY_PASSWORD -> SwipeActionType.COPY_PASSWORD
            SwipeActionProto.SWIPE_ACTION_COPY_TOTP -> SwipeActionType.COPY_PASSWORD
            SwipeActionProto.SWIPE_ACTION_OPEN_DETAILS -> SwipeActionType.DETAIL
            SwipeActionProto.SWIPE_ACTION_OPEN_IN_BROWSER -> SwipeActionType.DETAIL
            SwipeActionProto.SWIPE_ACTION_CALL -> SwipeActionType.DETAIL
            SwipeActionProto.SWIPE_ACTION_SEND_SMS -> SwipeActionType.DETAIL
            SwipeActionProto.SWIPE_ACTION_LAUNCH_APP -> SwipeActionType.DETAIL
        }

        fun SwipeActionType.toProto(): SwipeActionProto = when (this) {
            SwipeActionType.DELETE -> SwipeActionProto.SWIPE_ACTION_NONE
            SwipeActionType.DETAIL -> SwipeActionProto.SWIPE_ACTION_OPEN_DETAILS
            SwipeActionType.COPY_PASSWORD -> SwipeActionProto.SWIPE_ACTION_COPY_PASSWORD
            SwipeActionType.COPY_USERNAME -> SwipeActionProto.SWIPE_ACTION_COPY_USERNAME
        }

        // -- AutofillUiModeProto → AutofillUiMode --
        fun AutofillUiModeProto.toDomain(): AutofillUiMode = when (this) {
            AutofillUiModeProto.AUTOFILL_UI_SYSTEM_INLINE -> AutofillUiMode.SYSTEM_INLINE
            AutofillUiModeProto.AUTOFILL_UI_FULLSCREEN -> AutofillUiMode.BOTTOM_SHEET
            AutofillUiModeProto.AUTOFILL_UI_DIALOG -> AutofillUiMode.BOTTOM_SHEET
        }

        fun AutofillUiMode.toProto(): AutofillUiModeProto = when (this) {
            AutofillUiMode.SYSTEM_INLINE -> AutofillUiModeProto.AUTOFILL_UI_SYSTEM_INLINE
            AutofillUiMode.BOTTOM_SHEET -> AutofillUiModeProto.AUTOFILL_UI_DIALOG
        }

        // -- CardDensity --
        fun ProtoCardDensity.toDomain(): CardDensity = when (this) {
            ProtoCardDensity.CARD_DENSITY_COMPACT -> CardDensity.COMPACT
            ProtoCardDensity.CARD_DENSITY_STANDARD -> CardDensity.STANDARD
            ProtoCardDensity.CARD_DENSITY_COMFORTABLE -> CardDensity.COMFORTABLE
        }

        fun CardDensity.toProto(): ProtoCardDensity = when (this) {
            CardDensity.COMPACT -> ProtoCardDensity.CARD_DENSITY_COMPACT
            CardDensity.STANDARD -> ProtoCardDensity.CARD_DENSITY_STANDARD
            CardDensity.COMFORTABLE -> ProtoCardDensity.CARD_DENSITY_COMFORTABLE
        }

        // -- ExportFormat --
        fun ProtoExportFormat.toDomain(): ExportFormat = when (this) {
            ProtoExportFormat.EXPORT_FORMAT_ENCRYPTED -> ExportFormat.ENCRYPTED
            ProtoExportFormat.EXPORT_FORMAT_CSV -> ExportFormat.CSV
            ProtoExportFormat.EXPORT_FORMAT_JSON -> ExportFormat.JSON
        }

        fun ExportFormat.toProto(): ProtoExportFormat = when (this) {
            ExportFormat.ENCRYPTED -> ProtoExportFormat.EXPORT_FORMAT_ENCRYPTED
            ExportFormat.CSV -> ProtoExportFormat.EXPORT_FORMAT_CSV
            ExportFormat.JSON -> ProtoExportFormat.EXPORT_FORMAT_JSON
        }

        // -- ImportMode --
        fun ProtoImportMode.toDomain(): ImportMode = when (this) {
            ProtoImportMode.IMPORT_MODE_APPEND -> ImportMode.APPEND
            ProtoImportMode.IMPORT_MODE_REPLACE -> ImportMode.REPLACE
            ProtoImportMode.IMPORT_MODE_MERGE -> ImportMode.MERGE
        }

        fun ImportMode.toProto(): ProtoImportMode = when (this) {
            ImportMode.APPEND -> ProtoImportMode.IMPORT_MODE_APPEND
            ImportMode.REPLACE -> ProtoImportMode.IMPORT_MODE_REPLACE
            ImportMode.MERGE -> ProtoImportMode.IMPORT_MODE_MERGE
        }

        // ============================================================
        // VaultSortPreference ↔ VaultSortSpec
        // ============================================================

        fun VaultSortPreference.toDomain(): VaultSortSpec {
            val sortField = VaultSortField.entries.find { it.name == field }
                ?: VaultSortField.LAST_USED_AT
            val direction = if (descending) SortDirection.DESC else SortDirection.ASC
            val tieBreaker = VaultSortField.entries.find { it.name == tieBreakerField }
                ?: VaultSortField.ID
            return VaultSortSpec(sortField, direction, pinFavorites, tieBreaker)
        }

        fun VaultSortSpec.toProtoSort(): VaultSortPreference =
            VaultSortPreference.newBuilder()
                .setField(field.name)
                .setDescending(direction == SortDirection.DESC)
                .setPinFavorites(pinFavorites)
                .setTieBreakerField(tieBreaker.name)
                .build()

        // ============================================================
        // EntryCardPresentation proto ↔ domain
        // ============================================================

        fun ProtoEntryCardPresentation.toDomain(): EntryCardPresentation =
            EntryCardPresentation(
                entryTypeValue = entryTypeValue,
                variantKey = variantKey,
                density = density.toDomain(),
                showIcon = showIcon,
                showFavorite = showFavorite,
                showSecondaryText = showSecondaryText,
                showQuickAction = showQuickAction
            )

        fun EntryCardPresentation.toProto(): ProtoEntryCardPresentation =
            ProtoEntryCardPresentation.newBuilder()
                .setEntryTypeValue(entryTypeValue)
                .setVariantKey(variantKey)
                .setDensity(density.toProto())
                .setShowIcon(showIcon)
                .setShowFavorite(showFavorite)
                .setShowSecondaryText(showSecondaryText)
                .setShowQuickAction(showQuickAction)
                .build()

        // ============================================================
        // NoticeTopic / NoticeLevel (unchanged from old)
        // ============================================================

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

        // ============================================================
        // MessagePreferences encode / decode
        // ============================================================

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

    // ================================================================
    // settings flow
    // ================================================================

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

    private fun readAppearance(p: AppearancePreferences): AppearanceSettings =
        AppearanceSettings(
            themeMode = p.themeMode.toDomain(),
            isDynamicColor = p.dynamicColorEnabled,
            fallbackPalette = p.fallbackPalette.toDomain(),
            customSeedArgb = if (p.hasCustomSeedArgb()) p.customSeedArgb else null,
            language = p.language.toDomain(),
            fontFamily = p.fontFamily.toDomain()
        )

    private fun readInterface(p: InterfacePreferences): InterfaceSettings =
        InterfaceSettings(
            hideSystemBars = p.hideSystemBars,
            collapseTopBarOnScroll = p.collapseTopBarOnScroll,
            collapseTabBarOnScroll = p.collapseTabBarOnScroll
        )

    private fun readSecurity(p: SecurityPreferences): SecuritySettings =
        SecuritySettings(
            isSecureContentEnabled = p.secureContentEnabled,
            isFlipToLockEnabled = p.flipToLockEnabled,
            isFlipExitAndClearStackEnabled = p.flipExitAndClearStack,
            isLockOnBackground = p.lockOnBackground,
            lockTimeout = p.lockTimeoutMs,
            isInvalidateBiometricKeyOnChange = p.invalidateBiometricKeyOnChange
        )

    private fun readInteraction(p: InteractionPreferences): InteractionSettings =
        InteractionSettings(
            isSwipeEnabled = p.swipeActionsEnabled,
            swipeLeftAction = p.swipeLeftAction.toDomain(),
            swipeRightAction = p.swipeRightAction.toDomain(),
            autofillUiMode = p.autofillUiMode.toDomain(),
            isAutoDownloadIcons = p.autoDownloadIcons,
            faviconDownloadWhitelist = p.faviconAllowedDomainsList.toSet()
        )

    private fun readVault(p: VaultViewPreferences): VaultViewSettings {
        return VaultViewSettings(
            maxTabsWithoutScroll = p.maxTabsWithoutScroll,
            visibleTabs = if (p.hasVisibleTabs()) {
                VisibleTabsConfig(
                    tabKeys = p.visibleTabs.tabKeysList.toSet(),
                    configured = p.visibleTabs.configured
                )
            } else null,
            sort = if (p.hasSort()) p.sort.toDomain() else VaultSortSpec.DEFAULT,
            entryCardPresentations = p.entryCardPresentationsList.map { it.toDomain() }
        )
    }

    private fun readBackup(p: BackupPreferences): BackupSettings =
        BackupSettings(
            directoryTreeUri = p.directoryTreeUri.ifEmpty { null },
            defaultExportFormat = p.defaultExportFormat.toDomain(),
            includeIcons = p.includeIcons,
            includeAttachments = p.includeAttachments,
            includeDeletedEntries = p.includeDeletedEntries,
            includedEntryTypes = p.includedEntryTypesList.map { it.number }.toSet(),
            defaultImportMode = p.defaultImportMode.toDomain()
        )

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
        dataStore.updateData { proto ->
            val b = proto.toBuilder()
            when (command) {
                // ==================== Appearance ====================
                is SettingsCommand.SetThemeMode -> {
                    val ab = proto.appearance.toBuilder()
                    ab.themeMode = command.mode.toProto()
                    b.setAppearance(ab)
                }

                is SettingsCommand.SetDynamicColor -> {
                    val ab = proto.appearance.toBuilder()
                    ab.dynamicColorEnabled = command.enabled
                    b.setAppearance(ab)
                }

                is SettingsCommand.SetFallbackPalette -> {
                    val ab = proto.appearance.toBuilder()
                    ab.fallbackPalette = command.palette.toProto()
                    b.setAppearance(ab)
                }

                is SettingsCommand.SetCustomSeedArgb -> {
                    val ab = proto.appearance.toBuilder()
                    if (command.argb != null) ab.customSeedArgb = command.argb
                    else ab.clearCustomSeedArgb()
                    b.setAppearance(ab)
                }

                is SettingsCommand.SetLanguage -> {
                    val ab = proto.appearance.toBuilder()
                    ab.language = command.language.toProto()
                    b.setAppearance(ab)
                }

                is SettingsCommand.SetFontFamily -> {
                    val ab = proto.appearance.toBuilder()
                    ab.fontFamily = command.mode.toProto()
                    b.setAppearance(ab)
                }

                // ==================== Interface ====================
                is SettingsCommand.SetHideSystemBars -> {
                    val ib = proto.interfacePrefs.toBuilder()
                    ib.hideSystemBars = command.enabled
                    b.setInterfacePrefs(ib)
                }

                is SettingsCommand.SetTopBarCollapsible -> {
                    val ib = proto.interfacePrefs.toBuilder()
                    ib.collapseTopBarOnScroll = command.enabled
                    b.setInterfacePrefs(ib)
                }

                is SettingsCommand.SetTabBarCollapsible -> {
                    val ib = proto.interfacePrefs.toBuilder()
                    ib.collapseTabBarOnScroll = command.enabled
                    b.setInterfacePrefs(ib)
                }

                // ==================== Security ====================
                is SettingsCommand.SetSecureContentEnabled -> {
                    val sb = proto.security.toBuilder()
                    sb.secureContentEnabled = command.enabled
                    b.setSecurity(sb)
                }

                is SettingsCommand.SetFlipToLockEnabled -> {
                    val sb = proto.security.toBuilder()
                    sb.flipToLockEnabled = command.enabled
                    b.setSecurity(sb)
                }

                is SettingsCommand.SetFlipExitAndClearStackEnabled -> {
                    val sb = proto.security.toBuilder()
                    sb.flipExitAndClearStack = command.enabled
                    b.setSecurity(sb)
                }

                is SettingsCommand.SetLockOnBackground -> {
                    val sb = proto.security.toBuilder()
                    sb.lockOnBackground = command.enabled
                    b.setSecurity(sb)
                }

                is SettingsCommand.SetLockTimeout -> {
                    val sb = proto.security.toBuilder()
                    sb.lockTimeoutMs = command.timeoutMs
                    b.setSecurity(sb)
                }

                is SettingsCommand.SetInvalidateBiometricKeyOnChange -> {
                    val sb = proto.security.toBuilder()
                    sb.invalidateBiometricKeyOnChange = command.enabled
                    b.setSecurity(sb)
                }

                // ==================== Interaction ====================
                is SettingsCommand.SetSwipeEnabled -> {
                    val ib = proto.interaction.toBuilder()
                    ib.swipeActionsEnabled = command.enabled
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetSwipeLeftAction -> {
                    val ib = proto.interaction.toBuilder()
                    ib.swipeLeftAction = command.action.toProto()
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetSwipeRightAction -> {
                    val ib = proto.interaction.toBuilder()
                    ib.swipeRightAction = command.action.toProto()
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetAutofillUiMode -> {
                    val ib = proto.interaction.toBuilder()
                    ib.autofillUiMode = command.mode.toProto()
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetAutoDownloadIcons -> {
                    val ib = proto.interaction.toBuilder()
                    ib.autoDownloadIcons = command.enabled
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetFaviconDownloadWhitelist -> {
                    val ib = proto.interaction.toBuilder()
                    ib.clearFaviconAllowedDomains()
                    ib.addAllFaviconAllowedDomains(command.whitelist.sorted())
                    b.setInteraction(ib)
                }

                // ==================== Vault ====================
                is SettingsCommand.SetMaxTabsWithoutScroll -> {
                    val vb = proto.vaultView.toBuilder()
                    vb.maxTabsWithoutScroll = command.maxTabs.coerceIn(2, 8)
                    b.setVaultView(vb)
                }

                is SettingsCommand.SetVisibleVaultTabs -> {
                    val vb = proto.vaultView.toBuilder()
                    vb.visibleTabs = VisibleTabs.newBuilder()
                        .addAllTabKeys(command.keys.sorted())
                        .setConfigured(true)
                        .build()
                    b.setVaultView(vb)
                }

                is SettingsCommand.ClearVisibleVaultTabs -> {
                    val vb = proto.vaultView.toBuilder()
                    vb.clearVisibleTabs()
                    b.setVaultView(vb)
                }

                is SettingsCommand.SetVaultSortOption -> {
                    val vb = proto.vaultView.toBuilder()
                    vb.sort = command.sort.toProtoSort()
                    b.setVaultView(vb)
                }

                is SettingsCommand.SetEntryCardPresentation -> {
                    val vb = proto.vaultView.toBuilder()
                    val existing = vb.entryCardPresentationsList.toMutableList()
                    val idx = existing.indexOfFirst {
                        it.entryTypeValue == command.presentation.entryTypeValue
                    }
                    val protoPresentation = command.presentation.toProto()
                    if (idx >= 0) existing[idx] = protoPresentation
                    else existing.add(protoPresentation)
                    vb.clearEntryCardPresentations()
                    vb.addAllEntryCardPresentations(existing)
                    b.setVaultView(vb)
                }

                is SettingsCommand.RemoveEntryCardPresentation -> {
                    val vb = proto.vaultView.toBuilder()
                    val existing = vb.entryCardPresentationsList.toMutableList()
                    existing.removeAll { it.entryTypeValue == command.entryTypeValue }
                    vb.clearEntryCardPresentations()
                    vb.addAllEntryCardPresentations(existing)
                    b.setVaultView(vb)
                }

                // ==================== Messages ====================
                is SettingsCommand.SetOptionalMessagesEnabled -> {
                    val current = decodeMessageSettings(
                        proto.message.takeIf { proto.hasMessage() }
                    )
                    b.setMessage(
                        encodeMessageSettings(
                            current.copy(optionalMessagesEnabled = command.enabled)
                        )
                    )
                }

                is SettingsCommand.SetSystemNotificationsEnabled -> {
                    val current = decodeMessageSettings(
                        proto.message.takeIf { proto.hasMessage() }
                    )
                    b.setMessage(
                        encodeMessageSettings(
                            current.copy(systemNotificationsEnabled = command.enabled)
                        )
                    )
                }

                is SettingsCommand.SetMessageTopicEnabled -> {
                    val currentSettings = decodeMessageSettings(
                        proto.message.takeIf { proto.hasMessage() }
                    )
                    val current = currentSettings.topicSettings.toMutableMap()
                    val existing = current[command.topic] ?: TopicMessageSettings()
                    current[command.topic] = existing.copy(enabled = command.enabled)
                    b.setMessage(
                        encodeMessageSettings(currentSettings.copy(topicSettings = current))
                    )
                }

                is SettingsCommand.SetMessageTopicMinimumLevel -> {
                    val currentSettings = decodeMessageSettings(
                        proto.message.takeIf { proto.hasMessage() }
                    )
                    val current = currentSettings.topicSettings.toMutableMap()
                    val existing = current[command.topic] ?: TopicMessageSettings()
                    current[command.topic] = existing.copy(minimumLevel = command.level)
                    b.setMessage(
                        encodeMessageSettings(currentSettings.copy(topicSettings = current))
                    )
                }

                // ==================== Backup ====================
                is SettingsCommand.SetBackupDirectoryUri -> {
                    val bb = proto.backup.toBuilder()
                    bb.directoryTreeUri = command.uri
                    b.setBackup(bb)
                }

                is SettingsCommand.ClearBackupDirectoryUri -> {
                    val bb = proto.backup.toBuilder()
                    bb.directoryTreeUri = ""
                    b.setBackup(bb)
                }

                is SettingsCommand.SetDefaultExportFormat -> {
                    val bb = proto.backup.toBuilder()
                    bb.defaultExportFormat = command.format.toProto()
                    b.setBackup(bb)
                }

                is SettingsCommand.SetIncludeIcons -> {
                    val bb = proto.backup.toBuilder()
                    bb.includeIcons = command.enabled
                    b.setBackup(bb)
                }

                is SettingsCommand.SetIncludeAttachments -> {
                    val bb = proto.backup.toBuilder()
                    bb.includeAttachments = command.enabled
                    b.setBackup(bb)
                }

                is SettingsCommand.SetIncludeDeletedEntries -> {
                    val bb = proto.backup.toBuilder()
                    bb.includeDeletedEntries = command.enabled
                    b.setBackup(bb)
                }

                is SettingsCommand.SetDefaultImportMode -> {
                    val bb = proto.backup.toBuilder()
                    bb.defaultImportMode = command.mode.toProto()
                    b.setBackup(bb)
                }
            }
            b.build()
        }
    }
}
