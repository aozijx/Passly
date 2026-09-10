package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.local.datastore.appSettingsDataStore
import com.aozijx.passly.data.local.datastore.settings.AppSettings
import com.aozijx.passly.data.local.datastore.settings.AppearancePreferences
import com.aozijx.passly.data.local.datastore.settings.AutofillPreferences
import com.aozijx.passly.data.local.datastore.settings.BackupPreferences
import com.aozijx.passly.data.local.datastore.settings.InteractionPreferences
import com.aozijx.passly.data.local.datastore.settings.InterfacePreferences
import com.aozijx.passly.data.local.datastore.settings.SecurityPreferences
import com.aozijx.passly.data.local.datastore.settings.VaultViewPreferences
import com.aozijx.passly.data.local.datastore.settings.VisibleQuickFilters
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.ClipboardClearPolicy
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.model.ThemeCanvasTint
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.domain.settings.port.MessageSettingsRepository
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ProtoSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) : AppearanceSettingsRepository,
    BackupDirectorySettingsRepository,
    IdleTimeoutSettings,
    InterfaceSettingsRepository,
    InteractionSettingsRepository,
    LibraryViewSettingsRepository,
    MessageSettingsRepository,
    SecuritySettingsRepository {

    private val dataStore = context.applicationContext.appSettingsDataStore

    override val appearance = dataStore.data.map { proto -> readAppearance(proto.appearance) }

    override val security = dataStore.data.map { proto -> readSecurity(proto.security) }

    override val messages = dataStore.data.map { proto ->
        decodeMessageSettings(proto.message.takeIf { proto.hasMessage() })
    }

    override val backupDirectoryUri = dataStore.data.map { proto ->
        readBackup(proto.backup).directoryTreeUri
    }

    override val interfaceSettings =
        dataStore.data.map { proto -> readInterface(proto.interfacePrefs) }

    override val libraryViewSettings =
        dataStore.data.map { proto -> readVault(proto.vaultView) }

    override val interaction =
        dataStore.data.map { proto -> readInteraction(proto.interaction) }

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

    override suspend fun setThemeMode(mode: ThemeMode) =
        updateAppearance { themeMode = mode.toProto() }

    override suspend fun setDynamicColor(enabled: Boolean) =
        updateAppearance { dynamicColorEnabled = enabled }

    override suspend fun setThemeKey(key: String) = updateAppearance { themeKey = key }

    override suspend fun setCanvasTintPercent(percent: Int) = updateAppearance {
        canvasTintPercent = percent.coerceIn(
            ThemeCanvasTint.MIN_PERCENT,
            ThemeCanvasTint.MAX_PERCENT,
        )
    }

    override suspend fun setLanguage(language: AppLanguage) = updateAppearance { this.language = language.storageTag }

    override suspend fun setFontFamily(mode: FontFamilyMode) =
        updateAppearance { fontFamily = mode.toFontFamilyString() }

    override suspend fun setHideSystemBars(enabled: Boolean) = updateInterface { hideSystemBars = enabled }

    override suspend fun setTopBarCollapsible(enabled: Boolean) =
        updateInterface { collapseTopBarOnScroll = enabled }

    override suspend fun setQuickFilterBarCollapsible(enabled: Boolean) = updateInterface {
        collapseQuickFilterBarOnScroll = enabled
    }

    override suspend fun setAppCornerRadius(radiusDp: Float) = updateInterface {
        appCornerRadiusDp = AppCornerRadiusConstraints.normalize(radiusDp)
    }

    override suspend fun setVisibleQuickFilters(keys: Set<String>) = updateVaultView {
        visibleQuickFilters = VisibleQuickFilters.newBuilder()
            .addAllFilterKeys(keys.sorted())
            .setConfigured(true)
            .build()
    }
    override suspend fun setEntryHierarchyDisplayMode(mode: EntryHierarchyDisplayMode) =
        updateVaultView { entryHierarchyDisplayMode = mode.key }

    override suspend fun setSort(sort: EntrySort) = updateVaultView { this.sort = sort.toProtoSort() }

    override suspend fun setSecureContentEnabled(enabled: Boolean) =
        updateSecurity { secureContentEnabled = enabled }

    override suspend fun setFlipToLockEnabled(enabled: Boolean) = updateSecurity { flipToLockEnabled = enabled }

    override suspend fun setFlipExitAndClearStackEnabled(enabled: Boolean) = updateSecurity {
        flipExitAndClearStack = enabled
    }
    override suspend fun setLockOnBackground(enabled: Boolean) = updateSecurity { lockOnBackground = enabled }

    override suspend fun setLockTimeout(timeoutMs: Long) = updateSecurity { lockTimeoutMs = timeoutMs }

    override suspend fun setInvalidateBiometricKeyOnChange(enabled: Boolean) = updateSecurity {
        invalidateBiometricKeyOnChange = enabled
    }
    override suspend fun setReauthenticateSensitiveCopies(enabled: Boolean) = updateSecurity {
        reauthenticateSensitiveCopies = enabled
    }
    override suspend fun setClipboardClearEnabled(enabled: Boolean) = updateSecurity {
        clipboardClearEnabled = enabled
    }
    override suspend fun setClipboardClearDelaySeconds(delaySeconds: Int) = updateSecurity {
        clipboardClearDelaySeconds = ClipboardClearPolicy.normalizeDelaySeconds(delaySeconds)
    }

    override suspend fun setOptionalMessagesEnabled(enabled: Boolean) = updateMessages {
        copy(optionalMessagesEnabled = enabled)
    }
    override suspend fun setSystemNotificationsEnabled(enabled: Boolean) = updateMessages {
        copy(systemNotificationsEnabled = enabled)
    }
    override suspend fun setTopicEnabled(topic: MessageTopic, enabled: Boolean) = updateMessages {
        val topics = topicSettings.toMutableMap()
        topics[topic] = (topics[topic] ?: TopicMessageSettings()).copy(enabled = enabled)
        copy(topicSettings = topics)
    }
    override suspend fun setTopicMinimumLevel(topic: MessageTopic, level: MessageLevel) = updateMessages {
        val topics = topicSettings.toMutableMap()
        topics[topic] = (topics[topic] ?: TopicMessageSettings()).copy(minimumLevel = level)
        copy(topicSettings = topics)
    }

    override suspend fun setBackupDirectoryUri(uri: String) = updateBackup { directoryTreeUri = uri }

    override suspend fun clearBackupDirectoryUri() = updateBackup { directoryTreeUri = "" }

    override suspend fun setSwipeEnabled(enabled: Boolean) = updateInteraction { swipeActionsEnabled = enabled }

    override suspend fun setSwipeLeftAction(action: SwipeActionType) = updateInteraction {
        swipeLeftAction = action.toSwipeActionString()
    }
    override suspend fun setSwipeRightAction(action: SwipeActionType) = updateInteraction {
        swipeRightAction = action.toSwipeActionString()
    }
    override suspend fun setAutofillEnabled(enabled: Boolean) = updateAutofill { this.enabled = enabled }

    override suspend fun setAutofillPresentation(presentation: AutofillPresentation) = updateAutofill {
        this.presentation = presentation.toStorageKey()
    }
    override suspend fun setCredentialManagerEnabled(enabled: Boolean) = updateAutofill {
        credentialManagerEnabled = enabled
    }
    override suspend fun setAutofillAuthenticationRequired(required: Boolean) = updateAutofill {
        requireAuthentication = required
    }
    override suspend fun setAutofillOtpEnabled(enabled: Boolean) = updateAutofill { includeOtp = enabled }

    override suspend fun setAutofillSavePromptsEnabled(enabled: Boolean) = updateAutofill {
        savePromptsEnabled = enabled
    }
    override suspend fun setUnmatchedAutofillSuggestionsEnabled(enabled: Boolean) = updateAutofill {
        allowUnmatchedSuggestions = enabled
    }
    override suspend fun setAutofillMaxSuggestions(count: Int) = updateAutofill {
        maxSuggestions = count.coerceIn(
            AutofillSettings.MIN_SUGGESTIONS,
            AutofillSettings.MAX_SUGGESTIONS,
        )
    }

    private suspend fun updateAppearance(block: AppearancePreferences.Builder.() -> Unit) =
        updateProto { current -> setAppearance(current.appearance.toBuilder().apply(block)) }

    private suspend fun updateInterface(block: InterfacePreferences.Builder.() -> Unit) =
        updateProto { current -> setInterfacePrefs(current.interfacePrefs.toBuilder().apply(block)) }

    private suspend fun updateSecurity(block: SecurityPreferences.Builder.() -> Unit) =
        updateProto { current -> setSecurity(current.security.toBuilder().apply(block)) }

    private suspend fun updateInteraction(block: InteractionPreferences.Builder.() -> Unit) =
        updateProto { current -> setInteraction(current.interaction.toBuilder().apply(block)) }

    private suspend fun updateAutofill(block: AutofillPreferences.Builder.() -> Unit) =
        updateInteraction {
            setAutofill(autofill.toBuilder().apply(block))
        }

    private suspend fun updateVaultView(block: VaultViewPreferences.Builder.() -> Unit) =
        updateProto { current -> setVaultView(current.vaultView.toBuilder().apply(block)) }

    private suspend fun updateMessages(transform: MessageSettings.() -> MessageSettings) =
        updateProto { current ->
            val settings = decodeMessageSettings(current.message.takeIf { current.hasMessage() })
            setMessage(encodeMessageSettings(settings.transform()))
        }

    private suspend fun updateBackup(block: BackupPreferences.Builder.() -> Unit) =
        updateProto { current -> setBackup(current.backup.toBuilder().apply(block)) }

    private suspend fun updateProto(block: AppSettings.Builder.(AppSettings) -> Unit) {
        dataStore.updateData { current -> current.toBuilder().apply { block(current) }.build() }
    }
}
