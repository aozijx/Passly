package com.aozijx.passly.presentation.feature.vault.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.platform.media.FaviconCropRequest
import com.aozijx.passly.core.platform.media.FaviconImageProcessor
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
import com.aozijx.passly.domain.entry.port.EntryTagQuery
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.detail.DetailEntryEdit
import com.aozijx.passly.feature.vault.detail.EditDetailEntryUseCase
import com.aozijx.passly.feature.vault.detail.ExportOtpQrUseCase
import com.aozijx.passly.feature.vault.detail.RevealEntryFieldsUseCase
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldResult
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldUseCase
import com.aozijx.passly.feature.vault.entry.CopyOtpCodeUseCase
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimeFactory
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFieldUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailPresentationModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject internal constructor(
    private val entryTagQuery: EntryTagQuery,
    private val activityQueryRepository: ActivityQueryRepository,
    private val accessPolicy: DetailAccessPolicy,
    private val exportOtpQr: ExportOtpQrUseCase,
    private val revealEntryFields: RevealEntryFieldsUseCase,
    private val editDetailEntry: EditDetailEntryUseCase,
    private val copyEntryFieldUseCase: CopyEntryFieldUseCase,
    private val copyOtpCodeUseCase: CopyOtpCodeUseCase,
    private val faviconImageProcessor: FaviconImageProcessor,
    private val presentationLoader: DetailPresentationLoader,
    otpCodeRuntimeFactory: OtpCodeRuntimeFactory,
) : ViewModel() {
    private val revealStore = DetailRevealStore()
    private val faviconSession = DetailFaviconSession(faviconImageProcessor, viewModelScope)
    private val otpRuntime = otpCodeRuntimeFactory.create(viewModelScope)
    private var entryLoadJob: Job? = null
    private var historyJob: Job? = null
    private var packagePickerLoadJob: Job? = null
    private var loadedEntryId: EntryId? = null

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private val _effects = Channel<DetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private val _otpState = MutableStateFlow<OtpCodeState?>(null)
    val otpState: StateFlow<OtpCodeState?> = _otpState.asStateFlow()
    val presentation: StateFlow<DetailPresentationModel?> = combine(_uiState, _otpState) { state, otp ->
        toDetailPresentationModel(state, otp)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    init {
        viewModelScope.launch {
            otpRuntime.states.collect { states ->
                _otpState.value = loadedEntryId?.value?.let(states::get)
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
                        persistEntryEdit(
                            edit = DetailEntryEdit.SetTitle(newTitle),
                            completion = DetailEditCompletion.Title,
                        )
                    }
                }
            }

            DetailUiAction.ToggleFavorite -> {
                if (_uiState.value.entry == null) return
                viewModelScope.launch {
                    persistEntryEdit(
                        edit = DetailEntryEdit.ToggleFavorite,
                        completion = DetailEditCompletion.Favorite,
                    )
                }
            }

            DetailUiAction.LoadPackagePickerApps -> loadPackagePickerApps()
            is DetailUiAction.SelectAssociatedPackage -> {
                if (_uiState.value.entry == null) return
                viewModelScope.launch {
                    persistEntryEdit(
                        DetailEntryEdit.SetApplicationIds(setOf(event.packageName)),
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
                    persistEntryEdit(
                        DetailEntryEdit.SetNotes(notes.ifBlank { null }),
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
                if (state.entry == null) return
                val domain = state.fieldEdits.draft(DetailEditKey.DOMAIN)
                viewModelScope.launch {
                    persistEntryEdit(
                        DetailEntryEdit.SetPrimaryUrl(domain.trim().ifBlank { null }),
                        DetailEditCompletion.Associations,
                    )
                }
            }
            is DetailUiAction.StartFieldEdit ->
                event.field.revealedKey?.let {
                    mutate(DetailMutation.FieldEditingStarted(it, event.initialValue))
                }

            is DetailUiAction.UpdateFieldDraft ->
                event.field.revealedKey?.let {
                    mutate(DetailMutation.FieldDraftChanged(it, event.value))
                }

            is DetailUiAction.CancelFieldEdit ->
                event.field.revealedKey?.let {
                    mutate(DetailMutation.FieldEditingCancelled(it))
                }

            is DetailUiAction.ToggleFieldVisibility -> {
                val current = _uiState.value.entry ?: return
                val key = event.field.revealedKey ?: return
                if (_uiState.value.revealed(key) != null) {
                    setRevealedField(key, null)
                } else {
                    viewModelScope.launch {
                        revealFields(current, setOf(key))
                    }
                }
            }

            DetailUiAction.RevealBankCardFields -> revealFieldsFromPresentation(
                presentation.value?.content?.bankCard?.fieldsToReveal().orEmpty(),
            )

            DetailUiAction.RevealSshFields -> revealFieldsFromPresentation(
                presentation.value?.content?.ssh?.fieldsToReveal().orEmpty(),
            )

            is DetailUiAction.SaveField -> {
                if (_uiState.value.entry == null) return
                viewModelScope.launch {
                    val revealedKey = event.field.revealedKey ?: return@launch
                    val fieldKey = DetailSensitiveFieldKeyMapper.toFieldKey(revealedKey)
                        ?: return@launch
                    persistEntryEdit(
                        edit = DetailEntryEdit.SetSensitiveField(fieldKey, event.newValue),
                        completion = DetailEditCompletion.SensitiveField(revealedKey),
                    )
                }
            }

            DetailUiAction.OpenTagEditor -> {
                val current = _uiState.value.entry ?: return
                viewModelScope.launch {
                    mutate(
                        DetailMutation.TagEditorOpened(
                            currentTags = current.tags,
                            availableTags = entryTagQuery.findAllTags(),
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
                        persistEntryEdit(
                            edit = DetailEntryEdit.SetTags(normalized.tags),
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
                val source = event.source.toDetailFaviconSource()
                faviconSession.discardReplacedSource(_uiState.value.faviconEditor, source)
                mutate(DetailMutation.FaviconSourceChanged(source))
            }

            is DetailUiAction.SelectFaviconTab ->
                mutate(DetailMutation.FaviconTabChanged(event.tab.toDetailFaviconTab()))

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

            is DetailUiAction.CopyField -> event.field.copyKey?.let(::copyField)
            DetailUiAction.CopyOtpCode -> copyOtpCode()
            DetailUiAction.ExportOtpQr -> {
                val entry = _uiState.value.entry ?: return
                viewModelScope.launch {
                    exportOtpQr(entry)?.let { uri ->
                        _effects.send(DetailEffect.ShowOtpQr(uri))
                    }
                }
            }


            is DetailUiAction.ToggleAccessHistoryRecording -> {
                mutate(DetailMutation.AccessHistoryChanged(event.enabled))
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
            clearRevealedFields()
            _effects.send(DetailEffect.ContentCopied(fieldKey))
        }
    }

    private fun revealFieldsFromPresentation(fields: Set<DetailFieldUiModel>) {
        val current = _uiState.value.entry ?: return
        val hiddenKeys = fields.mapNotNullTo(linkedSetOf()) { it.revealedKey }
            .filterTo(linkedSetOf()) { _uiState.value.revealed(it) == null }
        if (hiddenKeys.isEmpty()) return
        viewModelScope.launch { revealFields(current, hiddenKeys) }
    }

    private fun copyOtpCode() {
        val entry = _uiState.value.entry ?: return
        viewModelScope.launch {
            if (copyOtpCodeUseCase(entry.id) { otpState.value?.code } != CopyEntryFieldResult.Copied) return@launch
            clearRevealedFields()
            _effects.send(DetailEffect.ContentCopied(null))
        }
    }

    private fun clearRevealedFields() {
        revealStore.clear()
        mutate(DetailMutation.RevealedFieldsCleared)
    }

    fun load(rawEntryId: String, launchMode: DetailLaunchMode = DetailLaunchMode.VIEW) {
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
            val snapshot = presentationLoader.open(entryId) ?: return@launch
            val latest = snapshot.presentation.entry
            mutate(DetailMutation.SessionOpened(snapshot))
            if (launchMode != DetailLaunchMode.VIEW) {
                when {
                    latest.username.isNotEmpty() -> mutate(
                        DetailMutation.FieldEditingStarted(RevealedFieldKey.USERNAME, latest.username),
                    )
                    SensitiveFieldKey.PASSWORD in snapshot.presentation.sensitiveFieldKeys -> mutate(
                        DetailMutation.FieldEditingStarted(RevealedFieldKey.PASSWORD, ""),
                    )
                }
            }
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

    private suspend fun persistEntryEdit(
        edit: DetailEntryEdit,
        completion: DetailEditCompletion,
    ) {
        if (edit !is DetailEntryEdit.SetTags && !accessPolicy.hasFullAccess()) return
        val entryId = _uiState.value.entry?.id ?: return
        if (_uiState.value.savingEdit != null) return
        mutate(DetailMutation.SaveStarted(completion))
        persistStartedEntryEdit(entryId, edit, completion)
    }

    private suspend fun persistStartedEntryEdit(
        entryId: EntryId,
        edit: DetailEntryEdit,
        completion: DetailEditCompletion,
    ) {
        when (val result = editDetailEntry.edit(entryId, edit)) {
            is AppResult.Success -> {
                val latest = result.data
                val keepTitleEditing = completion != DetailEditCompletion.Title &&
                        _uiState.value.isEditingTitle
                mutate(
                    DetailMutation.EntryPresented(
                        presentation = presentationLoader.present(latest),
                        isEditingTitle = keepTitleEditing,
                        editedTitle = if (keepTitleEditing) _uiState.value.editedTitle else latest.title,
                    ),
                )
                if (
                    completion is DetailEditCompletion.SensitiveField &&
                    edit is DetailEntryEdit.SetSensitiveField
                ) {
                    setRevealedField(completion.key, OwnedChars.fromString(edit.value))
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
            persistStartedEntryEdit(
                entryId = entryId,
                edit = DetailEntryEdit.SetIcon(persistedSource.toEntryIcon()),
                completion = DetailEditCompletion.Icon,
            )
        }
    }

    private fun loadPackagePickerApps() {
        val state = _uiState.value
        val entryId = state.entry?.id ?: return
        if (state.packagePickerAppsLoaded || packagePickerLoadJob?.isActive == true) return
        packagePickerLoadJob = viewModelScope.launch {
            val apps = presentationLoader.launchableApps()
            mutate(DetailMutation.PackagePickerAppsChanged(entryId, apps))
        }
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
