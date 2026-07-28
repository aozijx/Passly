package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.local.datastore.appSettingsDataStore
import com.aozijx.passly.data.local.datastore.settings.AppearancePreferences
import com.aozijx.passly.data.local.datastore.settings.AutofillPreferences
import com.aozijx.passly.data.local.datastore.settings.BackupPreferences
import com.aozijx.passly.data.local.datastore.settings.InteractionPreferences
import com.aozijx.passly.data.local.datastore.settings.InterfacePreferences
import com.aozijx.passly.data.local.datastore.settings.MessagePreferences
import com.aozijx.passly.data.local.datastore.settings.NoticeLevelProto
import com.aozijx.passly.data.local.datastore.settings.SecurityPreferences
import com.aozijx.passly.data.local.datastore.settings.VaultSortPreference
import com.aozijx.passly.data.local.datastore.settings.VaultViewPreferences
import com.aozijx.passly.data.local.datastore.settings.VisibleTabs
import com.aozijx.passly.domain.notice.model.AppMessageSettings
import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.model.NoticeTopic
import com.aozijx.passly.domain.notice.model.TopicMessageSettings
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppSettingsSnapshot
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.BackupSettings
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.ExportFormat
import com.aozijx.passly.domain.settings.model.FallbackPalette
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ImportMode
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.model.SortDirection
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.model.ThemeMode
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
import com.aozijx.passly.data.local.datastore.settings.CardDensity as ProtoCardDensity
import com.aozijx.passly.data.local.datastore.settings.EntryCardPresentation as ProtoEntryCardPresentation
import com.aozijx.passly.data.local.datastore.settings.FallbackPalette as ProtoFallbackPalette
import com.aozijx.passly.data.local.datastore.settings.ThemeMode as ProtoThemeMode

@Singleton
class ProtoAppSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) : AppSettingsRepository {

    private val dataStore = context.applicationContext.appSettingsDataStore

    private companion object {

        // ============================================================
        // Proto ↔ Domain enum mappings (closed sets — Proto enum kept)
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

        // -- CardDensity (Proto now has UNSPECIFIED=0, COMPACT=1, STANDARD=2, COMFORTABLE=3) --
        fun ProtoCardDensity.toDomain(): CardDensity = when (this) {
            ProtoCardDensity.CARD_DENSITY_COMPACT -> CardDensity.COMPACT
            ProtoCardDensity.CARD_DENSITY_STANDARD -> CardDensity.STANDARD
            ProtoCardDensity.CARD_DENSITY_COMFORTABLE -> CardDensity.COMFORTABLE
            ProtoCardDensity.CARD_DENSITY_UNSPECIFIED -> CardDensity.STANDARD
        }

        fun CardDensity.toProto(): ProtoCardDensity = when (this) {
            CardDensity.COMPACT -> ProtoCardDensity.CARD_DENSITY_COMPACT
            CardDensity.STANDARD -> ProtoCardDensity.CARD_DENSITY_STANDARD
            CardDensity.COMFORTABLE -> ProtoCardDensity.CARD_DENSITY_COMFORTABLE
        }

        // ============================================================
        // String ↔ Domain mappings (open sets — Proto stores string ID)
        // ============================================================

        // -- AppLanguage --
        private fun String.toAppLanguageDomain(): AppLanguage = AppLanguage.fromLanguageTag(this)

        // -- FontFamilyMode --
        private fun String.toFontFamilyDomain(): FontFamilyMode = when (this) {
            "system" -> FontFamilyMode.SYSTEM
            "app.default" -> FontFamilyMode.APP_BUNDLED
            else -> FontFamilyMode.APP_BUNDLED
        }

        private fun FontFamilyMode.toFontFamilyString(): String = when (this) {
            FontFamilyMode.SYSTEM -> "system"
            FontFamilyMode.APP_BUNDLED -> "app.default"
        }

        // -- SwipeActionType --
        private fun String.toSwipeActionDomain(): SwipeActionType = when (this) {
            "delete" -> SwipeActionType.DELETE
            "detail" -> SwipeActionType.DETAIL
            "copy_username" -> SwipeActionType.COPY_USERNAME
            "copy_password" -> SwipeActionType.COPY_PASSWORD
            else -> SwipeActionType.COPY_PASSWORD
        }

        private fun SwipeActionType.toSwipeActionString(): String = when (this) {
            SwipeActionType.DELETE -> "delete"
            SwipeActionType.DETAIL -> "detail"
            SwipeActionType.COPY_PASSWORD -> "copy_password"
            SwipeActionType.COPY_USERNAME -> "copy_username"
        }

        // -- Autofill presentation --
        private fun String.toAutofillPresentationDomain(): AutofillPresentation = when (this) {
            "bottom_sheet" -> AutofillPresentation.BOTTOM_SHEET
            else -> AutofillPresentation.SYSTEM_INLINE
        }

        private fun AutofillPresentation.toStorageKey(): String = when (this) {
            AutofillPresentation.SYSTEM_INLINE -> "system_inline"
            AutofillPresentation.BOTTOM_SHEET -> "bottom_sheet"
        }

        // -- ExportFormat --
        private fun String.toExportFormatDomain(): ExportFormat = when (this) {
            "passly.encrypted" -> ExportFormat.ENCRYPTED
            "csv" -> ExportFormat.CSV
            "json" -> ExportFormat.JSON
            else -> ExportFormat.ENCRYPTED
        }

        private fun ExportFormat.toExportFormatString(): String = when (this) {
            ExportFormat.ENCRYPTED -> "passly.encrypted"
            ExportFormat.CSV -> "csv"
            ExportFormat.JSON -> "json"
        }

        // -- ImportMode --
        private fun String.toImportModeDomain(): ImportMode = when (this) {
            "append" -> ImportMode.APPEND
            "replace" -> ImportMode.REPLACE
            "merge" -> ImportMode.MERGE
            else -> ImportMode.APPEND
        }

        private fun ImportMode.toImportModeString(): String = when (this) {
            ImportMode.APPEND -> "append"
            ImportMode.REPLACE -> "replace"
            ImportMode.MERGE -> "merge"
        }

        // -- NoticeTopic --
        private fun String.toNoticeTopicDomain(): NoticeTopic = when (this) {
            "clipboard" -> NoticeTopic.CLIPBOARD
            "app_lifecycle" -> NoticeTopic.APP_LIFECYCLE
            "icon_download" -> NoticeTopic.ICON_DOWNLOAD
            "backup" -> NoticeTopic.BACKUP
            "security" -> NoticeTopic.SECURITY
            "database" -> NoticeTopic.DATABASE
            else -> NoticeTopic.CLIPBOARD
        }

        private fun NoticeTopic.toNoticeTopicString(): String = when (this) {
            NoticeTopic.CLIPBOARD -> "clipboard"
            NoticeTopic.APP_LIFECYCLE -> "app_lifecycle"
            NoticeTopic.ICON_DOWNLOAD -> "icon_download"
            NoticeTopic.BACKUP -> "backup"
            NoticeTopic.SECURITY -> "security"
            NoticeTopic.DATABASE -> "database"
        }

        // -- NoticeLevel --
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
                entryTypeKey = entryTypeKey,
                variantKey = variantKey,
                density = density.toDomain(),
                showIcon = showIcon,
                showFavorite = showFavorite,
                showSecondaryText = showSecondaryText,
                showQuickAction = showQuickAction
            )

        fun EntryCardPresentation.toProto(): ProtoEntryCardPresentation =
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

        fun decodeMessageSettings(proto: MessagePreferences?): AppMessageSettings {
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
                        com.aozijx.passly.data.local.datastore.settings.TopicMessagePreference.newBuilder()
                            .setTopicKey(topic.toNoticeTopicString())
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
            language = p.language.toAppLanguageDomain(),
            fontFamily = p.fontFamily.toFontFamilyDomain(),
            isExpressive = p.expressiveEnabled
        )

    private fun readInterface(p: InterfacePreferences): InterfaceSettings =
        InterfaceSettings(
            hideSystemBars = p.hideSystemBars,
            collapseTopBarOnScroll = p.collapseTopBarOnScroll,
            collapseTabBarOnScroll = p.collapseTabBarOnScroll,
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
            swipeLeftAction = p.swipeLeftAction.toSwipeActionDomain(),
            swipeRightAction = p.swipeRightAction.toSwipeActionDomain(),
            autofill = readAutofill(p.autofill),
            isAutoDownloadIcons = p.autoDownloadIcons,
            faviconDownloadWhitelist = p.faviconAllowedDomainsList.toSet()
        )

    private fun readAutofill(p: AutofillPreferences): AutofillSettings =
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
            defaultExportFormat = p.defaultExportFormat.toExportFormatDomain(),
            includeIcons = p.includeIcons,
            includeAttachments = p.includeAttachments,
            includeDeletedEntries = p.includeDeletedEntries,
            includedEntryTypes = p.includedEntryTypesList.toSet(),
            defaultImportMode = p.defaultImportMode.toImportModeDomain()
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

                is SettingsCommand.SelectManualThemeColor -> {
                    val ab = proto.appearance.toBuilder()
                    if (command.argb != null) ab.customSeedArgb = command.argb
                    else ab.clearCustomSeedArgb()
                    ab.dynamicColorEnabled = false
                    b.setAppearance(ab)
                }

                is SettingsCommand.SetLanguage -> {
                    val ab = proto.appearance.toBuilder()
                    ab.language = command.language.storageTag
                    b.setAppearance(ab)
                }

                is SettingsCommand.SetFontFamily -> {
                    val ab = proto.appearance.toBuilder()
                    ab.fontFamily = command.mode.toFontFamilyString()
                    b.setAppearance(ab)
                }

                is SettingsCommand.SetExpressiveEnabled -> {
                    val ab = proto.appearance.toBuilder()
                    ab.expressiveEnabled = command.enabled
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

                is SettingsCommand.SetOuterCornerRadius -> {
                    val ib = proto.interfacePrefs.toBuilder()
                    ib.outerCornerRadiusDp = command.radiusDp.coerceIn(
                        InterfaceStyleConstraints.MIN_OUTER_RADIUS_DP,
                        InterfaceStyleConstraints.MAX_OUTER_RADIUS_DP
                    )
                    b.setInterfacePrefs(ib)
                }

                is SettingsCommand.SetInnerCornerRadius -> {
                    val ib = proto.interfacePrefs.toBuilder()
                    ib.innerCornerRadiusDp = command.radiusDp.coerceIn(
                        InterfaceStyleConstraints.MIN_INNER_RADIUS_DP,
                        InterfaceStyleConstraints.MAX_INNER_RADIUS_DP
                    )
                    b.setInterfacePrefs(ib)
                }

                is SettingsCommand.SetGroupItemSpacing -> {
                    val ib = proto.interfacePrefs.toBuilder()
                    ib.groupItemSpacingDp = command.spacingDp.coerceIn(
                        InterfaceStyleConstraints.MIN_ITEM_SPACING_DP,
                        InterfaceStyleConstraints.MAX_ITEM_SPACING_DP
                    )
                    b.setInterfacePrefs(ib)
                }

                is SettingsCommand.SetGroupContentPadding -> {
                    val ib = proto.interfacePrefs.toBuilder()
                    ib.groupContentPaddingDp = command.paddingDp.coerceIn(
                        InterfaceStyleConstraints.MIN_CONTENT_PADDING_DP,
                        InterfaceStyleConstraints.MAX_CONTENT_PADDING_DP
                    )
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
                    ib.swipeLeftAction = command.action.toSwipeActionString()
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetSwipeRightAction -> {
                    val ib = proto.interaction.toBuilder()
                    ib.swipeRightAction = command.action.toSwipeActionString()
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetAutofillEnabled -> {
                    val ib = proto.interaction.toBuilder()
                    val ab = proto.interaction.autofill.toBuilder()
                    ab.enabled = command.enabled
                    ib.setAutofill(ab)
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetAutofillPresentation -> {
                    val ib = proto.interaction.toBuilder()
                    val ab = proto.interaction.autofill.toBuilder()
                    ab.presentation = command.presentation.toStorageKey()
                    ib.setAutofill(ab)
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetCredentialManagerEnabled -> {
                    val ib = proto.interaction.toBuilder()
                    val ab = proto.interaction.autofill.toBuilder()
                    ab.credentialManagerEnabled = command.enabled
                    ib.setAutofill(ab)
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetAutofillAuthenticationRequired -> {
                    val ib = proto.interaction.toBuilder()
                    val ab = proto.interaction.autofill.toBuilder()
                    ab.requireAuthentication = command.required
                    ib.setAutofill(ab)
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetAutofillOtpEnabled -> {
                    val ib = proto.interaction.toBuilder()
                    val ab = proto.interaction.autofill.toBuilder()
                    ab.includeOtp = command.enabled
                    ib.setAutofill(ab)
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetAutofillSavePromptsEnabled -> {
                    val ib = proto.interaction.toBuilder()
                    val ab = proto.interaction.autofill.toBuilder()
                    ab.savePromptsEnabled = command.enabled
                    ib.setAutofill(ab)
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetUnmatchedAutofillSuggestionsEnabled -> {
                    val ib = proto.interaction.toBuilder()
                    val ab = proto.interaction.autofill.toBuilder()
                    ab.allowUnmatchedSuggestions = command.enabled
                    ib.setAutofill(ab)
                    b.setInteraction(ib)
                }

                is SettingsCommand.SetAutofillMaxSuggestions -> {
                    val ib = proto.interaction.toBuilder()
                    val ab = proto.interaction.autofill.toBuilder()
                    ab.maxSuggestions = command.count.coerceIn(
                        AutofillSettings.MIN_SUGGESTIONS,
                        AutofillSettings.MAX_SUGGESTIONS
                    )
                    ib.setAutofill(ab)
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
                        it.entryTypeKey == command.presentation.entryTypeKey
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
                    existing.removeAll { it.entryTypeKey == command.entryTypeKey }
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
                    bb.defaultExportFormat = command.format.toExportFormatString()
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
                    bb.defaultImportMode = command.mode.toImportModeString()
                    b.setBackup(bb)
                }
            }
            b.build()
        }
    }
}
