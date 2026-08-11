package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.local.datastore.appSettingsDataStore
import com.aozijx.passly.data.local.datastore.settings.VisibleQuickFilters
import com.aozijx.passly.domain.notice.model.TopicMessageSettings
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.AppSettingsSnapshot
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints
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
                    val argb = command.argb
                    if (argb != null) ab.manualThemeColorArgb = argb
                    else ab.clearManualThemeColorArgb()
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

                is SettingsCommand.SetQuickFilterBarCollapsible -> {
                    val ib = proto.interfacePrefs.toBuilder()
                    ib.collapseQuickFilterBarOnScroll = command.enabled
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

                is SettingsCommand.SetReauthenticateSensitiveCopies -> {
                    val sb = proto.security.toBuilder()
                    sb.reauthenticateSensitiveCopies = command.enabled
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
                is SettingsCommand.SetVisibleVaultQuickFilters -> {
                    val vb = proto.vaultView.toBuilder()
                    vb.visibleQuickFilters = VisibleQuickFilters.newBuilder()
                        .addAllFilterKeys(command.keys.sorted())
                        .setConfigured(true)
                        .build()
                    b.setVaultView(vb)
                }

                is SettingsCommand.ClearVisibleVaultQuickFilters -> {
                    val vb = proto.vaultView.toBuilder()
                    vb.clearVisibleQuickFilters()
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

                is SettingsCommand.SetEntryHierarchyDisplayMode -> {
                    val vb = proto.vaultView.toBuilder()
                    vb.entryHierarchyDisplayMode = command.mode.key
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
