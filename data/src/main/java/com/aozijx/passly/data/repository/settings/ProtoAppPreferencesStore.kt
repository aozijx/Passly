package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.local.datastore.appPreferencesDataStore
import com.aozijx.passly.data.preferences.proto.AppPreferences
import com.aozijx.passly.data.preferences.proto.AppearancePreferences
import com.aozijx.passly.data.preferences.proto.AutofillPreferences
import com.aozijx.passly.data.preferences.proto.BackupPreferences
import com.aozijx.passly.data.preferences.proto.InteractionPreferences
import com.aozijx.passly.data.preferences.proto.InterfacePreferences
import com.aozijx.passly.data.preferences.proto.DiagnosticsPreferences
import com.aozijx.passly.data.preferences.proto.VaultViewPreferences
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.domain.settings.port.MessageSettingsRepository
import com.aozijx.passly.domain.settings.port.DiagnosticsLogSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ProtoAppPreferencesStore @Inject constructor(
    @ApplicationContext context: Context,
) : AppearanceSettingsRepository,
    BackupDirectorySettingsRepository,
    InterfaceSettingsRepository,
    InteractionSettingsRepository,
    LibraryViewSettingsRepository,
    MessageSettingsRepository,
    DiagnosticsLogSettingsRepository {

    private val dataStore = context.applicationContext.appPreferencesDataStore

    override val appearance = dataStore.data.map { readAppearance(it.appearance) }
    override val backupDirectoryUri = dataStore.data.map { it.backup.directoryTreeUri.ifEmpty { null } }
    override val interfaceSettings = dataStore.data.map { readInterface(it.interfacePrefs) }
    override val interaction = dataStore.data.map { readInteraction(it.interaction) }
    override val libraryViewSettings = dataStore.data.map { readVault(it.vaultView) }
    override val messages = dataStore.data.map { decodeMessageSettings(it.message) }
    override val diagnosticsLogRecordingMode = dataStore.data.map {
        readDiagnosticsLogRecordingMode(it.diagnostics)
    }

    override suspend fun setThemeMode(mode: ThemeMode) = updateAppearance { themeMode = mode.toProto() }
    override suspend fun setDynamicColor(enabled: Boolean) = updateAppearance { dynamicColorEnabled = enabled }
    override suspend fun setThemeKey(key: String) = updateAppearance { themeKey = key }
    override suspend fun setLanguage(language: AppLanguage) = updateAppearance {
        this.language = language.storageTag
    }
    override suspend fun setFontFamily(mode: FontFamilyMode) = updateAppearance {
        fontFamily = mode.toProto()
    }

    override suspend fun setHideSystemBars(enabled: Boolean) = updateInterface { hideSystemBars = enabled }
    override suspend fun setTopBarCollapsible(enabled: Boolean) = updateInterface {
        collapseTopBarOnScroll = enabled
    }
    override suspend fun setQuickFilterBarCollapsible(enabled: Boolean) = updateInterface {
        collapseQuickFilterBarOnScroll = enabled
    }
    override suspend fun setAppCornerRadius(radiusDp: Float) = updateInterface {
        appCornerRadiusDp = AppCornerRadiusConstraints.normalize(radiusDp)
    }

    override suspend fun setEntryHierarchyDisplayMode(mode: EntryHierarchyDisplayMode) =
        updateVaultView { hierarchyDisplayMode = mode.toProto() }
    override suspend fun setSort(sort: EntrySort) = updateVaultView { this.sort = sort.toProtoSort() }

    override suspend fun setOptionalMessagesEnabled(enabled: Boolean) = updateMessages {
        copy(optionalMessagesEnabled = enabled)
    }
    override suspend fun setSystemNotificationsEnabled(enabled: Boolean) = updateMessages {
        copy(systemNotificationsEnabled = enabled)
    }
    override suspend fun setTopicEnabled(topic: MessageTopic, enabled: Boolean) = updateMessages {
        copy(topicSettings = topicSettings + (topic to topic(topic).copy(enabled = enabled)))
    }
    override suspend fun setTopicMinimumLevel(topic: MessageTopic, level: MessageLevel) = updateMessages {
        copy(topicSettings = topicSettings + (topic to topic(topic).copy(minimumLevel = level)))
    }

    override suspend fun setDiagnosticsLogRecordingMode(mode: DiagnosticsLogRecordingMode) = update {
        current -> setDiagnostics(current.diagnostics.toBuilder().setLogRecordingMode(mode.toProto()))
    }

    override suspend fun setBackupDirectoryUri(uri: String) = updateBackup { directoryTreeUri = uri }
    override suspend fun clearBackupDirectoryUri() = updateBackup { directoryTreeUri = "" }

    override suspend fun setSwipeEnabled(enabled: Boolean) = updateInteraction { swipeActionsEnabled = enabled }
    override suspend fun setPullToSearchEnabled(enabled: Boolean) = updateInteraction {
        pullToSearchEnabled = enabled
    }
    override suspend fun setSwipeLeftAction(action: SwipeActionType) = updateInteraction {
        swipeLeftAction = action.toProto()
    }
    override suspend fun setSwipeRightAction(action: SwipeActionType) = updateInteraction {
        swipeRightAction = action.toProto()
    }
    override suspend fun setAutofillEnabled(enabled: Boolean) = updateAutofill { this.enabled = enabled }
    override suspend fun setAutofillPresentation(presentation: AutofillPresentation) = updateAutofill {
        this.presentation = presentation.toProto()
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
        maxSuggestions = count.coerceIn(AutofillSettings.MIN_SUGGESTIONS, AutofillSettings.MAX_SUGGESTIONS)
    }

    private suspend fun updateAppearance(block: AppearancePreferences.Builder.() -> Unit) =
        update { current -> setAppearance(current.appearance.toBuilder().apply(block)) }
    private suspend fun updateInterface(block: InterfacePreferences.Builder.() -> Unit) =
        update { current -> setInterfacePrefs(current.interfacePrefs.toBuilder().apply(block)) }
    private suspend fun updateInteraction(block: InteractionPreferences.Builder.() -> Unit) =
        update { current -> setInteraction(current.interaction.toBuilder().apply(block)) }
    private suspend fun updateAutofill(block: AutofillPreferences.Builder.() -> Unit) =
        updateInteraction { setAutofill(autofill.toBuilder().apply(block)) }
    private suspend fun updateVaultView(block: VaultViewPreferences.Builder.() -> Unit) =
        update { current -> setVaultView(current.vaultView.toBuilder().apply(block)) }
    private suspend fun updateMessages(transform: MessageSettings.() -> MessageSettings) =
        update { current -> setMessage(encodeMessageSettings(decodeMessageSettings(current.message).transform())) }
    private suspend fun updateBackup(block: BackupPreferences.Builder.() -> Unit) =
        update { current -> setBackup(current.backup.toBuilder().apply(block)) }

    private suspend fun update(block: AppPreferences.Builder.(AppPreferences) -> Unit) {
        dataStore.updateData { current -> current.toBuilder().apply { block(current) }.build() }
    }
}
