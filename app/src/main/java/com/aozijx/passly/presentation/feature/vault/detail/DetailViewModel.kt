package com.aozijx.passly.presentation.feature.vault.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.entry.favicon.FaviconCropRequest
import com.aozijx.passly.app.entry.favicon.FaviconImageProcessor
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.policy.EntryTypePolicy
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.detail.DetailEntryPatch
import com.aozijx.passly.feature.vault.detail.ExportOtpQrUseCase
import com.aozijx.passly.feature.vault.detail.RevealEntryFieldsUseCase
import com.aozijx.passly.feature.vault.detail.UpdateDetailEntryUseCase
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldResult
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldUseCase
import com.aozijx.passly.feature.vault.entry.CopyOtpCodeUseCase
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimeFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject internal constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
    private val activityQueryRepository: ActivityQueryRepository,
    private val entryLinkRepository: EntryLinkRepository,
    private val activityRecorder: ActivityRecorder,
    private val entryTypePolicy: EntryTypePolicy,
    private val accessPolicy: DetailAccessPolicy,
    private val exportOtpQr: ExportOtpQrUseCase,
    private val revealEntryFields: RevealEntryFieldsUseCase,
    private val updateDetailEntry: UpdateDetailEntryUseCase,
    private val copyEntryFieldUseCase: CopyEntryFieldUseCase,
    private val copyOtpCodeUseCase: CopyOtpCodeUseCase,
    private val faviconImageProcessor: FaviconImageProcessor,
    private val installedAppDirectory: InstalledAppDirectory,
    otpCodeRuntimeFactory: OtpCodeRuntimeFactory,
) : ViewModel() {
    private val entryAnalyzer = DetailEntryAnalyzer(entryTypePolicy)
    private val revealStore = DetailRevealStore()
    private val faviconSession = DetailFaviconSession(faviconImageProcessor, viewModelScope)
    private val otpRuntime = otpCodeRuntimeFactory.create(viewModelScope)
    private var entryLoadJob: Job? = null
    private var historyJob: Job? = null
    private var packagePickerLoadJob: Job? = null
    private var loadedEntryId: EntryId? = null

    companion object {
        private const val ACCESS_HISTORY_TOGGLE_KEY = "detail.access_history_enabled"
    }

    private val userConfigExtras = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private val _effects = Channel<DetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private val _otpState = MutableStateFlow<OtpCodeState?>(null)
    val otpState: StateFlow<OtpCodeState?> = _otpState.asStateFlow()

    init {
        viewModelScope.launch {
            otpRuntime.states.collect { states ->
                _otpState.value = loadedEntryId?.value?.let(states::get)
            }
        }
        viewModelScope.launch {
            userConfigExtras.collect { extras ->
                val enabled = extras[ACCESS_HISTORY_TOGGLE_KEY]
                    ?.toBooleanStrictOrNull()
                    ?: false
                mutate(DetailMutation.AccessHistoryChanged(enabled))
            }
        }
    }

    fun onAction(event: DetailUiAction) {
        if (!accessPolicy.canHandle(event)) {
            clearSensitiveState()
            return
        }
        when (event) {

            DetailUiAction.StartTitleEdit -> {
                mutate(DetailMutation.TitleEditingStarted)
            }

            DetailUiAction.CancelTitleEdit -> {
                mutate(DetailMutation.TitleEditingCancelled)
            }

            is DetailUiAction.UpdateEditedTitle -> {
                mutate(DetailMutation.EditedTitleChanged(event.value))
            }

            DetailUiAction.SaveTitle -> {
                val state = _uiState.value
                val current = state.entry ?: return
                val newTitle = state.editedTitle.trim()
                if (newTitle.isBlank() || newTitle == current.title) {
                    mutate(DetailMutation.TitleEditingCancelled)
                } else {
                    viewModelScope.launch {
                        persistEntryPatch(
                            patch = DetailEntryPatch.Title(newTitle),
                            completion = DetailEditCompletion.Title,
                        )
                    }
                }
            }

            DetailUiAction.ToggleFavorite -> {
                val current = _uiState.value.entry ?: return
                viewModelScope.launch {
                    persistEntryPatch(
                        patch = DetailEntryPatch.Favorite(!current.favorite),
                        completion = DetailEditCompletion.Favorite,
                    )
                }
            }

            DetailUiAction.LoadPackagePickerApps -> loadPackagePickerApps()
            is DetailUiAction.SelectAssociatedPackage -> {
                val entry = _uiState.value.entry ?: return
                viewModelScope.launch {
                    persistEntryPatch(
                        DetailEntryPatch.Associations(
                            primaryUrl = entry.associations.primaryUrl,
                            applicationIds = setOf(event.packageName),
                        ),
                        DetailEditCompletion.Associations,
                    )
                }
            }
            DetailUiAction.StartNotesEdit -> {
                val notes = _uiState.value.entry?.secret?.notes.orEmpty()
                mutate(DetailMutation.FieldEditingStarted(DetailEditKey.NOTES, notes))
            }

            is DetailUiAction.UpdateNotesDraft ->
                mutate(DetailMutation.FieldDraftChanged(DetailEditKey.NOTES, event.value))

            DetailUiAction.SaveNotes -> {
                val notes = _uiState.value.fieldEdits.draft(DetailEditKey.NOTES)
                viewModelScope.launch {
                    persistEntryPatch(
                        DetailEntryPatch.Notes(notes.ifBlank { null }),
                        DetailEditCompletion.Notes,
                    )
                }
            }

            DetailUiAction.StartDomainEdit -> {
                val domain = _uiState.value.entry?.associations?.primaryUrl.orEmpty()
                mutate(DetailMutation.FieldEditingStarted(DetailEditKey.DOMAIN, domain))
            }

            is DetailUiAction.UpdateDomainDraft ->
                mutate(DetailMutation.FieldDraftChanged(DetailEditKey.DOMAIN, event.value))

            DetailUiAction.SaveDomain -> {
                val state = _uiState.value
                val entry = state.entry ?: return
                val domain = state.fieldEdits.draft(DetailEditKey.DOMAIN)
                viewModelScope.launch {
                    persistEntryPatch(
                        DetailEntryPatch.Associations(
                            primaryUrl = domain.trim().ifBlank { null },
                            applicationIds = entry.associations.applicationIds,
                        ),
                        DetailEditCompletion.Associations,
                    )
                }
            }
            is DetailUiAction.StartFieldEdit ->
                mutate(DetailMutation.FieldEditingStarted(event.key, event.initialValue))

            is DetailUiAction.UpdateFieldDraft ->
                mutate(DetailMutation.FieldDraftChanged(event.key, event.value))

            is DetailUiAction.CancelFieldEdit ->
                mutate(DetailMutation.FieldEditingCancelled(event.key))

            is DetailUiAction.ToggleFieldVisibility -> {
                val current = _uiState.value.entry ?: return
                if (_uiState.value.revealed(event.key) != null) {
                    setRevealedField(event.key, null)
                } else {
                    viewModelScope.launch {
                        revealFields(current, setOf(event.key))
                    }
                }
            }

            is DetailUiAction.RevealFields -> {
                val current = _uiState.value.entry ?: return
                val hiddenKeys = event.keys.filterTo(linkedSetOf()) {
                    _uiState.value.revealed(it) == null
                }
                if (hiddenKeys.isEmpty()) return
                viewModelScope.launch {
                    revealFields(current, hiddenKeys)
                }
            }

            is DetailUiAction.SaveField -> {
                val current = _uiState.value.entry ?: return
                viewModelScope.launch {
                    val patch = when (event.key) {
                        RevealedFieldKey.USERNAME,
                        RevealedFieldKey.CARDHOLDER -> DetailEntryPatch.Username(event.newValue)
                        RevealedFieldKey.PASSWORD -> when (current.type) {
                            EntryType.WIFI -> DetailEntryPatch.WifiPassword(event.newValue)
                            else -> DetailEntryPatch.LoginPassword(event.newValue)
                        }
                        RevealedFieldKey.CARD_NUMBER -> DetailEntryPatch.CardNumber(event.newValue)
                        RevealedFieldKey.CVV -> DetailEntryPatch.CardCvv(event.newValue)
                        RevealedFieldKey.SSH_PASSPHRASE ->
                            DetailEntryPatch.SshPassphrase(event.newValue)
                        else -> return@launch
                    }
                    persistEntryPatch(
                        patch = patch,
                        completion = DetailEditCompletion.SensitiveField(event.key),
                    )
                }
            }

            DetailUiAction.OpenTagEditor -> {
                val current = _uiState.value.entry ?: return
                viewModelScope.launch {
                    mutate(
                        DetailMutation.TagEditorOpened(
                            currentTags = current.tags,
                            availableTags = entryQueryRepository.findAllTags(),
                        ),
                    )
                }
            }

            is DetailUiAction.UpdateTagInput -> {
                if (event.value.any { it == ',' || it == '\n' || it == '\r' }) {
                    mutate(DetailMutation.TagSubmitted(event.value))
                } else {
                    mutate(DetailMutation.TagInputChanged(event.value))
                }
            }

            is DetailUiAction.SubmitTag ->
                mutate(DetailMutation.TagSubmitted(event.value))

            is DetailUiAction.RemoveTag ->
                mutate(DetailMutation.TagRemoved(event.value))

            DetailUiAction.SaveTags -> {
                val editor = _uiState.value.tagEditor
                when (
                    val normalized = DetailTagNormalizer.normalize(
                        editor.draftTags + editor.input,
                    )
                ) {
                    is TagNormalizationResult.Valid -> viewModelScope.launch {
                        persistEntryPatch(
                            patch = DetailEntryPatch.Tags(normalized.tags),
                            completion = DetailEditCompletion.Tags,
                        )
                    }

                    else -> mutate(DetailMutation.TagSubmitted(editor.input))
                }
            }

            DetailUiAction.DismissTagEditor ->
                mutate(DetailMutation.TagEditorDismissRequested)

            DetailUiAction.ConfirmDiscardTags ->
                mutate(DetailMutation.TagEditorDiscardConfirmed)

            DetailUiAction.KeepEditingTags ->
                mutate(DetailMutation.TagEditorDiscardCancelled)

            DetailUiAction.OpenFaviconEditor -> {
                val icon = _uiState.value.entry?.icon ?: return
                mutate(DetailMutation.FaviconEditorOpened(icon.toDetailFaviconSource()))
            }

            is DetailUiAction.SelectFaviconSource -> {
                faviconSession.discardReplacedSource(_uiState.value.faviconEditor, event.source)
                mutate(DetailMutation.FaviconSourceChanged(event.source))
            }

            is DetailUiAction.SelectFaviconTab ->
                mutate(DetailMutation.FaviconTabChanged(event.tab))

            is DetailUiAction.UpdateFaviconSearch ->
                mutate(DetailMutation.FaviconSearchChanged(event.value))

            is DetailUiAction.UpdateFaviconImageUrl ->
                mutate(DetailMutation.FaviconImageUrlChanged(event.value))

            is DetailUiAction.PickedFaviconImage -> {
                faviconSession.launchPreparation {
                    stageFaviconInput { faviconImageProcessor.stageUpload(event.uri) }
                }
            }

            DetailUiAction.DownloadFaviconImage -> {
                val url = _uiState.value.faviconEditor.imageUrl
                faviconSession.launchPreparation {
                    stageFaviconInput { faviconImageProcessor.stageHttpsUrl(url) }
                }
            }

            DetailUiAction.UseFaviconWithoutCrop -> {
                faviconSession.launchPreparation { processPendingFavicon(crop = null) }
            }

            is DetailUiAction.CropFaviconImage -> {
                faviconSession.launchPreparation {
                    processPendingFavicon(
                        FaviconCropRequest(event.zoom, event.offsetX, event.offsetY),
                    )
                }
            }

            DetailUiAction.CancelFaviconCrop -> {
                if (_uiState.value.faviconEditor.processing) return
                faviconSession.cancelCrop(_uiState.value.faviconEditor.pendingInputPath)
                mutate(DetailMutation.FaviconCropCancelled)
            }

            DetailUiAction.SaveFavicon -> saveFavicon()

            DetailUiAction.DismissFaviconEditor -> {
                if (_uiState.value.faviconEditor.processing) return
                val editor = _uiState.value.faviconEditor
                mutate(DetailMutation.FaviconEditorDismissRequested)
                if (!_uiState.value.faviconEditor.visible) {
                    faviconSession.close(editor)
                }
            }

            DetailUiAction.ConfirmDiscardFavicon -> {
                val editor = _uiState.value.faviconEditor
                faviconSession.close(editor)
                mutate(DetailMutation.FaviconEditorDiscardConfirmed)
            }

            DetailUiAction.KeepEditingFavicon ->
                mutate(DetailMutation.FaviconEditorDiscardCancelled)

            is DetailUiAction.CopyField -> copyField(event.fieldKey)
            DetailUiAction.CopyOtpCode -> copyOtpCode()
            DetailUiAction.ExportOtpQr -> {
                val entry = _uiState.value.entry ?: return
                viewModelScope.launch {
                    exportOtpQr(entry)?.let { uri ->
                        _effects.send(DetailEffect.ShowOtpQr(uri))
                        activityRecorder.recordUsage(entry.id.value, ActivityType.VIEW)
                    }
                }
            }


            is DetailUiAction.ToggleAccessHistoryRecording -> {
                mutate(DetailMutation.AccessHistoryChanged(event.enabled))
                userConfigExtras.value += (ACCESS_HISTORY_TOGGLE_KEY to event.enabled.toString())
            }

            DetailUiAction.ClearSensitiveState -> {
                clearSensitiveState()
            }
        }
    }

    private fun copyField(fieldKey: FieldKey) {
        val entry = _uiState.value.entry ?: return
        viewModelScope.launch {
            if (copyEntryFieldUseCase(entry.id, entry.type, fieldKey) != CopyEntryFieldResult.Copied) return@launch
            recordCopy(entry.id, fieldKey.copyActivityType())
            _effects.send(DetailEffect.ContentCopied(fieldKey))
        }
    }

    private fun copyOtpCode() {
        val entry = _uiState.value.entry ?: return
        viewModelScope.launch {
            if (copyOtpCodeUseCase { otpState.value?.code } != CopyEntryFieldResult.Copied) return@launch
            recordCopy(entry.id, ActivityType.COPY_PASSWORD)
            _effects.send(DetailEffect.ContentCopied(null))
        }
    }

    private suspend fun recordCopy(entryId: EntryId, type: ActivityType) {
        revealStore.clear()
        mutate(DetailMutation.RevealedFieldsCleared)
        activityRecorder.recordUsage(entryId.value, type)
    }

    private fun FieldKey.copyActivityType(): ActivityType = when (this) {
        FieldKey.USERNAME, FieldKey.CARD_HOLDER, FieldKey.WIFI_SSID -> ActivityType.COPY_USERNAME
        else -> ActivityType.COPY_PASSWORD
    }

    fun load(rawEntryId: String) {
        val entryId = EntryId(rawEntryId)
        if (loadedEntryId == entryId && _uiState.value.entry != null) return
        loadedEntryId = entryId
        entryLoadJob?.cancel()
        historyJob?.cancel()
        packagePickerLoadJob?.cancel()
        otpRuntime.clearAllSensitiveState()
        _otpState.value = null
        revealStore.clear()
        mutate(DetailMutation.StateCleared)
        entryLoadJob = viewModelScope.launch {
            if (!accessPolicy.hasFullAccess()) return@launch
            val latest = entryQueryRepository.getById(entryId) ?: return@launch
            refreshFromEntry(latest, isEditingTitle = false, editedTitle = latest.title)
            loadAssociatedApps(latest)
            val presence = sensitiveFieldRepository.getPresence(latest.id)
            mutate(DetailMutation.SensitiveFieldPresenceChanged(latest.id, presence.keys))
            loadRelatedEntries(latest)
            if (latest.secret.otp != null) otpRuntime.autoUnlock(entryId.value)
        }
        historyJob = viewModelScope.launch {
            if (!accessPolicy.hasFullAccess()) return@launch
            activityQueryRepository.observeByEntryId(entryId.value)
                .collect { history ->
                    mutate(DetailMutation.HistoryChanged(entryId, history))
                }
        }
    }

    private fun refreshKeepingTitleEdit(entry: Entry) {
        val isEditing = _uiState.value.isEditingTitle
        refreshFromEntry(
            entry,
            isEditingTitle = isEditing,
            editedTitle = if (isEditing) _uiState.value.editedTitle else entry.title
        )
    }

    private fun setRevealedField(key: String, value: SensitiveValue?) {
        revealStore.replace(key, value)
        mutate(DetailMutation.RevealedFieldChanged(key, value))
    }

    private suspend fun revealFields(entry: Entry, uiKeys: Set<String>) {
        if (!accessPolicy.hasFullAccess()) return
        val requested = uiKeys.mapNotNull(DetailSensitiveFieldKeyMapper::toFieldKey).toSet()
        if (requested.isEmpty()) return
        revealEntryFields.reveal(
            entry = entry,
            requestedFields = requested,
            recordLowSensitivityAccess = _uiState.value.isAccessHistoryEnabled,
        ).forEach { (fieldKey, value) ->
            DetailSensitiveFieldKeyMapper.toUiKey(fieldKey)?.let { uiKey ->
                setRevealedField(uiKey, value)
            }
        }
    }

    private fun clearSensitiveState() {
        revealStore.clear()
        otpRuntime.clearAllSensitiveState()
        _otpState.value = null
        mutate(DetailMutation.StateCleared)
    }

    private suspend fun persistEntryPatch(
        patch: DetailEntryPatch,
        completion: DetailEditCompletion,
    ) {
        if (patch !is DetailEntryPatch.Tags && !accessPolicy.hasFullAccess()) return
        val entryId = _uiState.value.entry?.id ?: return
        if (_uiState.value.savingEdit != null) return
        mutate(DetailMutation.SaveStarted(completion))
        persistStartedEntryPatch(entryId, patch, completion)
    }

    private suspend fun persistStartedEntryPatch(
        entryId: EntryId,
        patch: DetailEntryPatch,
        completion: DetailEditCompletion,
    ) {
        when (val result = updateDetailEntry.update(entryId, patch)) {
            is AppResult.Success -> {
                val latest = result.data
                val keepTitleEditing = completion != DetailEditCompletion.Title &&
                        _uiState.value.isEditingTitle
                refreshFromEntry(
                    latest,
                    isEditingTitle = keepTitleEditing,
                    editedTitle = if (keepTitleEditing) _uiState.value.editedTitle else latest.title,
                )
                if (completion == DetailEditCompletion.Associations) {
                    loadAssociatedApps(latest)
                }
                if (completion is DetailEditCompletion.SensitiveField) {
                    patch.revealedValueOrNull()?.let { value ->
                        setRevealedField(completion.key, OwnedChars.fromString(value))
                    }
                }
                mutate(DetailMutation.SaveSucceeded(completion))
            }

            is AppResult.Failure -> {
                mutate(DetailMutation.SaveFailed(completion, result.error.code))
            }
        }
    }

    private fun saveFavicon() {
        val state = _uiState.value
        val editor = state.faviconEditor
        val entryId = state.entry?.id ?: return
        if (
            editor.processing ||
            !editor.dirty ||
            state.savingEdit != null ||
            !accessPolicy.hasFullAccess()
        ) {
            return
        }

        mutate(DetailMutation.SaveStarted(DetailEditCompletion.Icon))
        faviconSession.launchSave {
            val source = _uiState.value.faviconEditor.source
            val persistedSource = if (
                source is DetailFaviconSource.PrivateImage &&
                faviconImageProcessor.isStaged(source.localPath)
            ) {
                val promoted = faviconImageProcessor.promote(source.localPath)
                    .getOrElse { error ->
                        val processingError = error.toFaviconProcessingError()
                        mutate(DetailMutation.FaviconProcessingFailed(processingError))
                        mutate(DetailMutation.SaveFailed(DetailEditCompletion.Icon, processingError.name))
                        return@launchSave
                    }
                DetailFaviconSource.PrivateImage(promoted).also {
                    mutate(DetailMutation.FaviconSourcePromoted(promoted))
                }
            } else {
                source
            }
            persistStartedEntryPatch(
                entryId = entryId,
                patch = DetailEntryPatch.Icon(persistedSource.toEntryIcon()),
                completion = DetailEditCompletion.Icon,
            )
        }
    }

    private suspend fun loadAssociatedApps(entry: Entry) {
        val apps = entry.associations.applicationIds
            .sorted()
            .map { packageName ->
                val metadata = installedAppDirectory.metadataFor(packageName)
                DetailInstalledApp(
                    label = metadata?.label?.takeIf(String::isNotBlank) ?: packageName,
                    packageName = packageName,
                )
            }
        mutate(DetailMutation.AssociatedAppsChanged(entry.id, apps))
    }

    private fun loadPackagePickerApps() {
        val state = _uiState.value
        val entryId = state.entry?.id ?: return
        if (state.packagePickerAppsLoaded || packagePickerLoadJob?.isActive == true) return
        packagePickerLoadJob = viewModelScope.launch {
            val apps = installedAppDirectory.launchableApps().map { metadata ->
                DetailInstalledApp(
                    label = metadata.label,
                    packageName = metadata.packageName,
                )
            }
            mutate(DetailMutation.PackagePickerAppsChanged(entryId, apps))
        }
    }
    private suspend fun loadRelatedEntries(entry: Entry) {
        val relatedIds = DetailRelatedEntryIds.resolve(
            entryId = entry.id,
            entryType = entry.type,
            links = entryLinkRepository.getAll(),
        )
        if (relatedIds.isEmpty()) {
            mutate(DetailMutation.RelatedEntriesChanged(entry.id, emptyList()))
            return
        }
        val related = relatedIds.mapNotNull { relatedId ->
            entryQueryRepository.getById(relatedId)
        }
        mutate(DetailMutation.RelatedEntriesChanged(entry.id, related))
    }

    private fun refreshFromEntry(entry: Entry, isEditingTitle: Boolean, editedTitle: String) {
        val analysis = entryAnalyzer.analyze(entry)

        mutate(
            DetailMutation.EntryPresented(
                entry = entry,
                entryType = analysis.entryType,
                strategySummary = analysis.strategySummary,
                validationError = analysis.validationError,
                sections = analysis.sections,
                isEditingTitle = isEditingTitle,
                editedTitle = editedTitle,
                strategyReady = analysis.strategyReady,
            )
        )
    }

    private fun mutate(mutation: DetailMutation) {
        _uiState.value = DetailReducer.reduce(_uiState.value, mutation)
    }

    override fun onCleared() {
        val editor = _uiState.value.faviconEditor
        faviconSession.close(editor)
        clearSensitiveState()
    }

    private suspend fun stageFaviconInput(
        stage: suspend () -> Result<String>,
    ) {
        if (_uiState.value.faviconEditor.processing) return
        mutate(DetailMutation.FaviconProcessingStarted)
        stage().fold(
            onSuccess = { mutate(DetailMutation.FaviconInputStaged(it)) },
            onFailure = { mutate(DetailMutation.FaviconProcessingFailed(it.toFaviconProcessingError())) },
        )
    }

    private suspend fun processPendingFavicon(crop: FaviconCropRequest?) {
        val path = _uiState.value.faviconEditor.pendingInputPath ?: return
        if (_uiState.value.faviconEditor.processing) return
        mutate(DetailMutation.FaviconProcessingStarted)
        faviconImageProcessor.process(path, crop).fold(
            onSuccess = {
                mutate(
                    DetailMutation.FaviconSourceChanged(
                        DetailFaviconSource.PrivateImage(it),
                    ),
                )
            },
            onFailure = { mutate(DetailMutation.FaviconProcessingFailed(it.toFaviconProcessingError())) },
        )
    }

}

private fun DetailEntryPatch.revealedValueOrNull(): String? = when (this) {
    is DetailEntryPatch.Username -> value
    is DetailEntryPatch.LoginPassword -> value
    is DetailEntryPatch.CardNumber -> value
    is DetailEntryPatch.CardCvv -> value
    is DetailEntryPatch.WifiPassword -> value
    is DetailEntryPatch.SshPassphrase -> value
    else -> null
}
