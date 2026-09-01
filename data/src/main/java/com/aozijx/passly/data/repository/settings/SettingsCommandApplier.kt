package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.AppSettings
import com.aozijx.passly.data.local.datastore.settings.VisibleQuickFilters
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.ClipboardClearPolicy
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.model.ThemeCanvasTint
import com.aozijx.passly.domain.settings.model.TopicMessageSettings

/**
 * 把一条 [SettingsCommand] 应用到 Proto 构建器并返回新快照。
 * 每个分支读取当前子偏好、修改目标字段后写回；消息分支先解码再重编码。
 */
internal fun AppSettings.applyCommand(command: SettingsCommand): AppSettings {
    val b = toBuilder()
    when (command) {
        // ==================== Appearance ====================
        is SettingsCommand.SetThemeMode -> {
            val ab = appearance.toBuilder()
            ab.themeMode = command.mode.toProto()
            b.setAppearance(ab)
        }

        is SettingsCommand.SetDynamicColor -> {
            val ab = appearance.toBuilder()
            ab.dynamicColorEnabled = command.enabled
            b.setAppearance(ab)
        }

        is SettingsCommand.SetThemeKey -> {
            val ab = appearance.toBuilder()
            ab.themeKey = command.key
            b.setAppearance(ab)
        }

        is SettingsCommand.SetCanvasTintPercent -> {
            val ab = appearance.toBuilder()
            ab.canvasTintPercent = command.percent.coerceIn(
                ThemeCanvasTint.MIN_PERCENT,
                ThemeCanvasTint.MAX_PERCENT,
            )
            b.setAppearance(ab)
        }

        is SettingsCommand.SetLanguage -> {
            val ab = appearance.toBuilder()
            ab.language = command.language.storageTag
            b.setAppearance(ab)
        }

        is SettingsCommand.SetFontFamily -> {
            val ab = appearance.toBuilder()
            ab.fontFamily = command.mode.toFontFamilyString()
            b.setAppearance(ab)
        }

        // ==================== Interface ====================
        is SettingsCommand.SetHideSystemBars -> {
            val ib = interfacePrefs.toBuilder()
            ib.hideSystemBars = command.enabled
            b.setInterfacePrefs(ib)
        }

        is SettingsCommand.SetTopBarCollapsible -> {
            val ib = interfacePrefs.toBuilder()
            ib.collapseTopBarOnScroll = command.enabled
            b.setInterfacePrefs(ib)
        }

        is SettingsCommand.SetQuickFilterBarCollapsible -> {
            val ib = interfacePrefs.toBuilder()
            ib.collapseQuickFilterBarOnScroll = command.enabled
            b.setInterfacePrefs(ib)
        }

        is SettingsCommand.SetOuterCornerRadius -> {
            val ib = interfacePrefs.toBuilder()
            ib.outerCornerRadiusDp = command.radiusDp.coerceIn(
                InterfaceStyleConstraints.MIN_OUTER_RADIUS_DP,
                InterfaceStyleConstraints.MAX_OUTER_RADIUS_DP
            )
            b.setInterfacePrefs(ib)
        }

        is SettingsCommand.SetInnerCornerRadius -> {
            val ib = interfacePrefs.toBuilder()
            ib.innerCornerRadiusDp = command.radiusDp.coerceIn(
                InterfaceStyleConstraints.MIN_INNER_RADIUS_DP,
                InterfaceStyleConstraints.MAX_INNER_RADIUS_DP
            )
            b.setInterfacePrefs(ib)
        }

        is SettingsCommand.SetGroupItemSpacing -> {
            val ib = interfacePrefs.toBuilder()
            ib.groupItemSpacingDp = command.spacingDp.coerceIn(
                InterfaceStyleConstraints.MIN_ITEM_SPACING_DP,
                InterfaceStyleConstraints.MAX_ITEM_SPACING_DP
            )
            b.setInterfacePrefs(ib)
        }

        is SettingsCommand.SetGroupContentPadding -> {
            val ib = interfacePrefs.toBuilder()
            ib.groupContentPaddingDp = command.paddingDp.coerceIn(
                InterfaceStyleConstraints.MIN_CONTENT_PADDING_DP,
                InterfaceStyleConstraints.MAX_CONTENT_PADDING_DP
            )
            b.setInterfacePrefs(ib)
        }

        // ==================== Security ====================
        is SettingsCommand.SetSecureContentEnabled -> {
            val sb = security.toBuilder()
            sb.secureContentEnabled = command.enabled
            b.setSecurity(sb)
        }

        is SettingsCommand.SetFlipToLockEnabled -> {
            val sb = security.toBuilder()
            sb.flipToLockEnabled = command.enabled
            b.setSecurity(sb)
        }

        is SettingsCommand.SetFlipExitAndClearStackEnabled -> {
            val sb = security.toBuilder()
            sb.flipExitAndClearStack = command.enabled
            b.setSecurity(sb)
        }

        is SettingsCommand.SetLockOnBackground -> {
            val sb = security.toBuilder()
            sb.lockOnBackground = command.enabled
            b.setSecurity(sb)
        }

        is SettingsCommand.SetLockTimeout -> {
            val sb = security.toBuilder()
            sb.lockTimeoutMs = command.timeoutMs
            b.setSecurity(sb)
        }

        is SettingsCommand.SetInvalidateBiometricKeyOnChange -> {
            val sb = security.toBuilder()
            sb.invalidateBiometricKeyOnChange = command.enabled
            b.setSecurity(sb)
        }

        is SettingsCommand.SetReauthenticateSensitiveCopies -> {
            val sb = security.toBuilder()
            sb.reauthenticateSensitiveCopies = command.enabled
            b.setSecurity(sb)
        }

        is SettingsCommand.SetClipboardClearEnabled -> {
            val sb = security.toBuilder()
            sb.clipboardClearEnabled = command.enabled
            b.setSecurity(sb)
        }

        is SettingsCommand.SetClipboardClearDelaySeconds -> {
            val sb = security.toBuilder()
            sb.clipboardClearDelaySeconds = ClipboardClearPolicy.normalizeDelaySeconds(
                command.delaySeconds
            )
            b.setSecurity(sb)
        }

        // ==================== Interaction ====================
        is SettingsCommand.SetSwipeEnabled -> {
            val ib = interaction.toBuilder()
            ib.swipeActionsEnabled = command.enabled
            b.setInteraction(ib)
        }

        is SettingsCommand.SetSwipeLeftAction -> {
            val ib = interaction.toBuilder()
            ib.swipeLeftAction = command.action.toSwipeActionString()
            b.setInteraction(ib)
        }

        is SettingsCommand.SetSwipeRightAction -> {
            val ib = interaction.toBuilder()
            ib.swipeRightAction = command.action.toSwipeActionString()
            b.setInteraction(ib)
        }

        is SettingsCommand.SetAutofillEnabled -> {
            val ib = interaction.toBuilder()
            val ab = interaction.autofill.toBuilder()
            ab.enabled = command.enabled
            ib.setAutofill(ab)
            b.setInteraction(ib)
        }

        is SettingsCommand.SetAutofillPresentation -> {
            val ib = interaction.toBuilder()
            val ab = interaction.autofill.toBuilder()
            ab.presentation = command.presentation.toStorageKey()
            ib.setAutofill(ab)
            b.setInteraction(ib)
        }

        is SettingsCommand.SetCredentialManagerEnabled -> {
            val ib = interaction.toBuilder()
            val ab = interaction.autofill.toBuilder()
            ab.credentialManagerEnabled = command.enabled
            ib.setAutofill(ab)
            b.setInteraction(ib)
        }

        is SettingsCommand.SetAutofillAuthenticationRequired -> {
            val ib = interaction.toBuilder()
            val ab = interaction.autofill.toBuilder()
            ab.requireAuthentication = command.required
            ib.setAutofill(ab)
            b.setInteraction(ib)
        }

        is SettingsCommand.SetAutofillOtpEnabled -> {
            val ib = interaction.toBuilder()
            val ab = interaction.autofill.toBuilder()
            ab.includeOtp = command.enabled
            ib.setAutofill(ab)
            b.setInteraction(ib)
        }

        is SettingsCommand.SetAutofillSavePromptsEnabled -> {
            val ib = interaction.toBuilder()
            val ab = interaction.autofill.toBuilder()
            ab.savePromptsEnabled = command.enabled
            ib.setAutofill(ab)
            b.setInteraction(ib)
        }

        is SettingsCommand.SetUnmatchedAutofillSuggestionsEnabled -> {
            val ib = interaction.toBuilder()
            val ab = interaction.autofill.toBuilder()
            ab.allowUnmatchedSuggestions = command.enabled
            ib.setAutofill(ab)
            b.setInteraction(ib)
        }

        is SettingsCommand.SetAutofillMaxSuggestions -> {
            val ib = interaction.toBuilder()
            val ab = interaction.autofill.toBuilder()
            ab.maxSuggestions = command.count.coerceIn(
                AutofillSettings.MIN_SUGGESTIONS,
                AutofillSettings.MAX_SUGGESTIONS
            )
            ib.setAutofill(ab)
            b.setInteraction(ib)
        }

        // ==================== Vault ====================
        is SettingsCommand.SetVisibleLibraryQuickFilters -> {
            val vb = vaultView.toBuilder()
            vb.visibleQuickFilters = VisibleQuickFilters.newBuilder()
                .addAllFilterKeys(command.keys.sorted())
                .setConfigured(true)
                .build()
            b.setVaultView(vb)
        }

        is SettingsCommand.ClearVisibleLibraryQuickFilters -> {
            val vb = vaultView.toBuilder()
            vb.clearVisibleQuickFilters()
            b.setVaultView(vb)
        }

        is SettingsCommand.SetVaultSortOption -> {
            val vb = vaultView.toBuilder()
            vb.sort = command.sort.toProtoSort()
            b.setVaultView(vb)
        }

        is SettingsCommand.SetEntryCardPresentation -> {
            val vb = vaultView.toBuilder()
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
            val vb = vaultView.toBuilder()
            val existing = vb.entryCardPresentationsList.toMutableList()
            existing.removeAll { it.entryTypeKey == command.entryTypeKey }
            vb.clearEntryCardPresentations()
            vb.addAllEntryCardPresentations(existing)
            b.setVaultView(vb)
        }

        is SettingsCommand.SetEntryHierarchyDisplayMode -> {
            val vb = vaultView.toBuilder()
            vb.entryHierarchyDisplayMode = command.mode.key
            b.setVaultView(vb)
        }

        // ==================== Messages ====================
        is SettingsCommand.SetOptionalMessagesEnabled -> {
            val current = decodeMessageSettings(
                message.takeIf { hasMessage() }
            )
            b.setMessage(
                encodeMessageSettings(
                    current.copy(optionalMessagesEnabled = command.enabled)
                )
            )
        }

        is SettingsCommand.SetSystemNotificationsEnabled -> {
            val current = decodeMessageSettings(
                message.takeIf { hasMessage() }
            )
            b.setMessage(
                encodeMessageSettings(
                    current.copy(systemNotificationsEnabled = command.enabled)
                )
            )
        }

        is SettingsCommand.SetMessageTopicEnabled -> {
            val currentSettings = decodeMessageSettings(
                message.takeIf { hasMessage() }
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
                message.takeIf { hasMessage() }
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
            val bb = backup.toBuilder()
            bb.directoryTreeUri = command.uri
            b.setBackup(bb)
        }

        is SettingsCommand.ClearBackupDirectoryUri -> {
            val bb = backup.toBuilder()
            bb.directoryTreeUri = ""
            b.setBackup(bb)
        }

        is SettingsCommand.SetDefaultExportFormat -> {
            val bb = backup.toBuilder()
            bb.defaultExportFormat = command.format.toExportFormatString()
            b.setBackup(bb)
        }

        is SettingsCommand.SetIncludeIcons -> {
            val bb = backup.toBuilder()
            bb.includeIcons = command.enabled
            b.setBackup(bb)
        }

        is SettingsCommand.SetIncludeAttachments -> {
            val bb = backup.toBuilder()
            bb.includeAttachments = command.enabled
            b.setBackup(bb)
        }

        is SettingsCommand.SetIncludeDeletedEntries -> {
            val bb = backup.toBuilder()
            bb.includeDeletedEntries = command.enabled
            b.setBackup(bb)
        }

        is SettingsCommand.SetDefaultImportMode -> {
            val bb = backup.toBuilder()
            bb.defaultImportMode = command.mode.toImportModeString()
            b.setBackup(bb)
        }
    }
    return b.build()
}
