package com.aozijx.passly.feature.vault.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.platform.media.FaviconCropRequest
import com.aozijx.passly.core.platform.media.FaviconImageProcessor
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
import com.aozijx.passly.domain.entry.port.EntryTagQuery
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.SecureSessionAccessPolicy
import com.aozijx.passly.feature.vault.detail.DetailEntryEdit
import com.aozijx.passly.feature.vault.detail.EditDetailEntryUseCase
import com.aozijx.passly.feature.vault.detail.ExportEntryQrUseCase
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.vault.detail.RevealEntryFieldsUseCase
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldResult
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldUseCase
import com.aozijx.passly.feature.vault.entry.CopyOtpCodeUseCase
import com.aozijx.passly.feature.vault.history.CompareEntryRevisionUseCase
import com.aozijx.passly.feature.vault.history.ObserveEntryRevisionsUseCase
import com.aozijx.passly.feature.vault.history.RestoreEntryRevisionUseCase
import com.aozijx.passly.feature.vault.history.RevealRevisionFieldsUseCase
import com.aozijx.passly.feature.vault.history.RevisionOperationResult
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimeFactory
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionFailure
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionMutation
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionPage
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionRevealStore
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailFieldUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailPresentationModel
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
    private val accessPolicy: SecureSessionAccessPolicy,
    exportEntryQr: ExportEntryQrUseCase,
    secureSessionAccessState: SecureSessionAccessState,
    private val revealEntryFields: RevealEntryFieldsUseCase,
    private val editDetailEntry: EditDetailEntryUseCase,
    private val copyEntryFieldUseCase: CopyEntryFieldUseCase,
    private val copyOtpCodeUseCase: CopyOtpCodeUseCase,
    private val faviconImageProcessor: FaviconImageProcessor,
    private val presentationLoader: DetailPresentationLoader,
    private val observeEntryRevisions: ObserveEntryRevisionsUseCase,
    private val compareEntryRevision: CompareEntryRevisionUseCase,
    private val revealRevisionFields: RevealRevisionFieldsUseCase,
    private val restoreEntryRevision: RestoreEntryRevisionUseCase,
    otpCodeRuntimeFactory: OtpCodeRuntimeFactory,
) : ViewModel() {
    private val revealStore = DetailRevealStore()
    private val revisionRevealStore = DetailRevisionRevealStore()
    private val faviconSession = DetailFaviconSession(faviconImageProcessor, viewModelScope)
    private val otpRuntime = otpCodeRuntimeFactory.create(viewModelScope)
    private val qrSession = DetailQrSession(viewModelScope, secureSessionAccessState, exportEntryQr::invoke)
    val qrState = qrSession.state
    private var entryLoadJob: Job? = null
    private var historyJob: Job? = null
    private var packagePickerLoadJob: Job? = null
    private var revisionObservationJob: Job? = null
    private var revisionOperationJob: Job? = null
    private var loadedEntryId: EntryId? = null

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private val _effects = Channel<DetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private val _otpState = MutableStateFlow<OtpCodeState?>(null)
    val otpState: StateFlow<OtpCodeState?> = _otpState.asStateFlow()
    private val _revisionReveals = MutableStateFlow(emptyMap<SensitiveFieldKey, SensitiveValue>())
    val presentation: StateFlow<DetailPresentationModel?> = combine(
        _uiState,
        _otpState,
        _revisionReveals,
    ) { state, otp, revisionReveals ->
        toDetailPresentationModel(state, otp, revisionReveals)
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
        if (event !is DetailUiAction.ClearSensitiveState && !accessPolicy.hasFullAccess()) {
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
                mutate(DetailMutation.FieldEditingStarted(DetailDraftKey.Notes, notes))
            }

            is DetailUiAction.UpdateNotesDraft ->
                mutate(DetailMutation.FieldDraftChanged(DetailDraftKey.Notes, event.value))

            DetailUiAction.SaveNotes -> {
                val notes = _uiState.value.fieldEdits.draft(DetailDraftKey.Notes)
                viewModelScope.launch {
                    persistEntryEdit(
                        DetailEntryEdit.SetNotes(notes.ifBlank { null }),
                        DetailEditCompletion.Notes,
                    )
                }
            }

            DetailUiAction.StartDomainEdit -> {
                val domain = _uiState.value.entry?.associations?.primaryUrl.orEmpty()
                mutate(DetailMutation.FieldEditingStarted(DetailDraftKey.Domain, domain))
            }

            is DetailUiAction.UpdateDomainDraft ->
                mutate(DetailMutation.FieldDraftChanged(DetailDraftKey.Domain, event.value))

            DetailUiAction.SaveDomain -> {
                val state = _uiState.value
                if (state.entry == null) return
                val domain = state.fieldEdits.draft(DetailDraftKey.Domain)
                viewModelScope.launch {
                    persistEntryEdit(
                        DetailEntryEdit.SetPrimaryUrl(domain.trim().ifBlank { null }),
                        DetailEditCompletion.Associations,
                    )
                }
            }

            is DetailUiAction.StartFieldEdit ->
                mutate(DetailMutation.FieldEditingStarted(event.field.draftKey, event.initialValue))

            is DetailUiAction.UpdateFieldDraft ->
                mutate(DetailMutation.FieldDraftChanged(event.field.draftKey, event.value))

            is DetailUiAction.CancelFieldEdit ->
                mutate(DetailMutation.FieldEditingCancelled(event.field.draftKey))

            is DetailUiAction.ToggleFieldVisibility -> {
                val current = _uiState.value.entry ?: return
                val key = event.field.fieldKey
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
                    val fieldKey = event.field.fieldKey
                    persistEntryEdit(
                        edit = DetailEntryEdit.SetFieldValue(fieldKey, event.newValue),
                        completion = DetailEditCompletion.Field(fieldKey),
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
                val editor = _uiState.value.tagEditor as? DetailTagEditorState.Open ?: return
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
                val editor = _uiState.value.faviconEditor as? DetailFaviconEditorState.Open
                    ?: return
                val source = event.source.toDetailFaviconSource()
                faviconSession.discardReplacedSource(editor, source)
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
                val editor = _uiState.value.faviconEditor as? DetailFaviconEditorState.Open
                    ?: return
                val url = editor.imageUrl
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
                val editor = _uiState.value.faviconEditor as? DetailFaviconEditorState.Open
                    ?: return
                if (editor.work == DetailFaviconWorkState.Processing) return
                faviconSession.cancelCrop(editor.pendingInputPath)
                mutate(DetailMutation.FaviconCropCancelled)
            }

            DetailUiAction.SaveFavicon -> saveFavicon()

            DetailUiAction.DismissFaviconEditor -> {
                val editor = _uiState.value.faviconEditor as? DetailFaviconEditorState.Open
                    ?: return
                if (editor.work == DetailFaviconWorkState.Processing) return
                mutate(DetailMutation.FaviconEditorDismissRequested)
                if (_uiState.value.faviconEditor == DetailFaviconEditorState.Closed) {
                    faviconSession.close(editor)
                }
            }

            DetailUiAction.ConfirmDiscardFavicon -> {
                val editor = _uiState.value.faviconEditor as? DetailFaviconEditorState.Open
                    ?: return
                faviconSession.close(editor)
                mutate(DetailMutation.FaviconEditorDiscardConfirmed)
            }

            DetailUiAction.KeepEditingFavicon ->
                mutate(DetailMutation.FaviconEditorDiscardCancelled)

            is DetailUiAction.CopyField -> copyField(event.field.fieldKey)
            DetailUiAction.CopyOtpCode -> copyOtpCode()
            DetailUiAction.ShowEntryQr -> _uiState.value.entry?.let(qrSession::request)
            DetailUiAction.DismissEntryQr -> qrSession.close()


            is DetailUiAction.ToggleAccessHistoryRecording -> {
                mutate(DetailMutation.AccessHistoryChanged(event.enabled))
            }

            DetailUiAction.ClearSensitiveState -> {
                clearSensitiveState()
            }

            DetailUiAction.OpenRevisionHistory -> openRevisionHistory()
            DetailUiAction.DismissRevisionHistory -> closeRevisionHistory()
            is DetailUiAction.SelectRevision -> selectRevision(event.revisionId)
            DetailUiAction.RevisionBack -> {
                revisionOperationJob?.cancel()
                clearRevisionReveals()
                mutate(DetailMutation.Revision(DetailRevisionMutation.BackToList))
            }

            is DetailUiAction.RevealRevisionField -> revealRevisionField(event.key)
            DetailUiAction.RequestRevisionRestore ->
                mutate(DetailMutation.Revision(DetailRevisionMutation.RestoreRequested))

            DetailUiAction.CancelRevisionRestore ->
                mutate(DetailMutation.Revision(DetailRevisionMutation.RestoreCancelled))

            DetailUiAction.ConfirmRevisionRestore -> restoreSelectedRevision()
        }
    }

    private fun copyField(fieldKey: FieldKey) {
        val entry = _uiState.value.entry ?: return
        viewModelScope.launch {
            if (copyEntryFieldUseCase(
                    entry.id,
                    entry.type,
                    fieldKey
                ) != CopyEntryFieldResult.Copied
            ) return@launch
            clearRevealedFields()
            _effects.send(DetailEffect.ContentCopied(fieldKey))
        }
    }

    private fun revealFieldsFromPresentation(fields: Set<DetailFieldUiModel>) {
        val current = _uiState.value.entry ?: return
        val hiddenKeys = fields.mapTo(linkedSetOf()) { it.fieldKey }
            .filterTo(linkedSetOf()) { _uiState.value.revealed(it) == null }
        if (hiddenKeys.isEmpty()) return
        viewModelScope.launch { revealFields(current, hiddenKeys) }
    }

    private fun copyOtpCode() {
        val entry = _uiState.value.entry ?: return
        viewModelScope.launch {
            if (
                copyOtpCodeUseCase(entry.id) {
                    (otpState.value as? OtpCodeState.Ready)?.code
                } != CopyEntryFieldResult.Copied
            ) return@launch
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
        qrSession.close()
        loadedEntryId = entryId
        entryLoadJob?.cancel()
        historyJob?.cancel()
        packagePickerLoadJob?.cancel()
        revisionObservationJob?.cancel()
        revisionOperationJob?.cancel()
        otpRuntime.clearAllSensitiveState()
        _otpState.value = null
        revealStore.clear()
        clearRevisionReveals()
        mutate(DetailMutation.StateCleared)
        entryLoadJob = viewModelScope.launch {
            if (!accessPolicy.hasFullAccess()) return@launch
            val snapshot = presentationLoader.open(entryId) ?: return@launch
            val latest = snapshot.presentation.entry
            mutate(DetailMutation.SessionOpened(snapshot))
            if (launchMode != DetailLaunchMode.VIEW) {
                when {
                    latest.username.isNotEmpty() -> mutate(
                        DetailMutation.FieldEditingStarted(
                            DetailDraftKey.EntryField(FieldKey.USERNAME),
                            latest.username
                        ),
                    )

                    SensitiveFieldKey.PASSWORD in snapshot.presentation.sensitiveFieldKeys -> mutate(
                        DetailMutation.FieldEditingStarted(
                            DetailDraftKey.EntryField(FieldKey.PASSWORD),
                            "",
                        ),
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

    private fun setRevealedField(key: FieldKey, value: SensitiveValue?) {
        revealStore.replace(key, value)
        mutate(DetailMutation.RevealedFieldChanged(key, value))
    }

    private suspend fun revealFields(entry: Entry, requested: Set<FieldKey>) {
        if (!accessPolicy.hasFullAccess()) return
        if (requested.isEmpty()) return
        revealEntryFields.reveal(
            entry = entry,
            requestedFields = requested,
            recordLowSensitivityAccess = _uiState.value.isAccessHistoryEnabled,
        ).forEach(::setRevealedField)
    }

    private fun clearSensitiveState() {
        qrSession.close()
        revealStore.clear()
        revisionObservationJob?.cancel()
        revisionOperationJob?.cancel()
        clearRevisionReveals()
        otpRuntime.clearAllSensitiveState()
        _otpState.value = null
        mutate(DetailMutation.StateCleared)
    }

    private fun openRevisionHistory() {
        val entryId = _uiState.value.entry?.id ?: return
        revisionObservationJob?.cancel()
        revisionOperationJob?.cancel()
        clearRevisionReveals()
        mutate(DetailMutation.Revision(DetailRevisionMutation.Opened(entryId)))
        revisionObservationJob = viewModelScope.launch {
            when (val initialization = observeEntryRevisions.initialize()) {
                is AppResult.Failure -> {
                    mutateRevisionFailure(
                        entryId = entryId,
                        revisionId = null,
                        result = RevisionOperationResult.Failed(initialization.error),
                    )
                    return@launch
                }

                is AppResult.Success -> Unit
            }
            observeEntryRevisions(entryId).collect { revisions ->
                mutate(
                    DetailMutation.Revision(
                        DetailRevisionMutation.MetadataChanged(entryId.value, revisions),
                    ),
                )
            }
        }
    }

    private fun closeRevisionHistory() {
        revisionObservationJob?.cancel()
        revisionOperationJob?.cancel()
        clearRevisionReveals()
        mutate(DetailMutation.Revision(DetailRevisionMutation.Closed))
    }

    private fun selectRevision(rawRevisionId: String) {
        val revisionState = _uiState.value.revisions as? DetailRevisionState.Open ?: return
        val entryId = revisionState.entryId
        val revisionId = EntryRevisionId(rawRevisionId)
        revisionOperationJob?.cancel()
        clearRevisionReveals()
        mutate(
            DetailMutation.Revision(
                DetailRevisionMutation.ComparisonStarted(entryId.value, rawRevisionId),
            ),
        )
        revisionOperationJob = viewModelScope.launch {
            when (val result = compareEntryRevision(entryId, revisionId)) {
                is RevisionOperationResult.Succeeded -> mutate(
                    DetailMutation.Revision(
                        DetailRevisionMutation.ComparisonLoaded(
                            entryId.value,
                            rawRevisionId,
                            result.value,
                        ),
                    ),
                )

                else -> mutateRevisionFailure(entryId, rawRevisionId, result)
            }
        }
    }

    private fun revealRevisionField(key: SensitiveFieldKey) {
        val state = _uiState.value.revisions as? DetailRevisionState.Open ?: return
        val comparison = state.page as? DetailRevisionPage.Comparison ?: return
        val entryId = state.entryId
        val revisionId = EntryRevisionId(comparison.revisionId)
        revisionOperationJob = viewModelScope.launch {
            when (val result = revealRevisionFields(entryId, revisionId, setOf(key))) {
                is RevisionOperationResult.Succeeded -> {
                    result.value.forEach { revisionRevealStore.replace(it.key, it.value) }
                    _revisionReveals.value = revisionRevealStore.snapshot()
                }

                RevisionOperationResult.Cancelled -> Unit
                else -> mutateRevisionFailure(entryId, revisionId.value, result)
            }
        }
    }

    private fun restoreSelectedRevision() {
        val state = _uiState.value.revisions as? DetailRevisionState.Open ?: return
        val comparison = state.page as? DetailRevisionPage.Comparison ?: return
        val entryId = state.entryId
        val revisionId = EntryRevisionId(comparison.revisionId)
        revisionOperationJob?.cancel()
        mutate(DetailMutation.Revision(DetailRevisionMutation.RestoreStarted))
        revisionOperationJob = viewModelScope.launch {
            when (val result = restoreEntryRevision(entryId, revisionId)) {
                is RevisionOperationResult.Succeeded -> {
                    clearRevisionReveals()
                    mutate(DetailMutation.Revision(DetailRevisionMutation.RestoreSucceeded))
                    presentationLoader.open(entryId)?.let {
                        mutate(DetailMutation.SessionOpened(it))
                    }
                }

                RevisionOperationResult.Cancelled ->
                    mutate(DetailMutation.Revision(DetailRevisionMutation.RestoreCancelled))

                else -> mutateRevisionFailure(entryId, revisionId.value, result)
            }
        }
    }

    private fun mutateRevisionFailure(
        entryId: EntryId,
        revisionId: String?,
        result: RevisionOperationResult<*>,
    ) {
        val failure = when (result) {
            RevisionOperationResult.Missing -> DetailRevisionFailure.MISSING
            RevisionOperationResult.Stale -> DetailRevisionFailure.STALE
            RevisionOperationResult.InvalidSnapshot -> DetailRevisionFailure.INVALID
            is RevisionOperationResult.AuthenticationDenied -> DetailRevisionFailure.AUTHENTICATION
            else -> DetailRevisionFailure.UNEXPECTED
        }
        mutate(
            DetailMutation.Revision(
                DetailRevisionMutation.Failed(entryId.value, revisionId, failure),
            ),
        )
    }

    private fun clearRevisionReveals() {
        revisionRevealStore.clear()
        _revisionReveals.value = emptyMap()
    }

    private suspend fun persistEntryEdit(
        edit: DetailEntryEdit,
        completion: DetailEditCompletion,
    ) {
        val entryId = _uiState.value.entry?.id ?: return
        if (_uiState.value.saveState is DetailSaveState.Saving) return
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
                    completion is DetailEditCompletion.Field &&
                    edit is DetailEntryEdit.SetFieldValue
                ) {
                    setRevealedField(edit.fieldKey, OwnedChars.fromString(edit.value))
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
        val editor = state.faviconEditor as? DetailFaviconEditorState.Open ?: return
        val entryId = state.entry?.id ?: return
        if (
            editor.work == DetailFaviconWorkState.Processing ||
            !editor.dirty ||
            state.saveState is DetailSaveState.Saving ||
            !accessPolicy.hasFullAccess()
        ) {
            return
        }

        mutate(DetailMutation.SaveStarted(DetailEditCompletion.Icon))
        faviconSession.launchSave {
            val source = (_uiState.value.faviconEditor as? DetailFaviconEditorState.Open)
                ?.source ?: return@launchSave
            val persistedSource = if (
                source is DetailFaviconSource.PrivateImage &&
                faviconImageProcessor.isStaged(source.localPath)
            ) {
                val promoted = faviconImageProcessor.promote(source.localPath)
                    .getOrElse { error ->
                        val processingError = error.toFaviconProcessingError()
                        mutate(DetailMutation.FaviconProcessingFailed(processingError))
                        mutate(
                            DetailMutation.SaveFailed(
                                DetailEditCompletion.Icon,
                                processingError.name
                            )
                        )
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
        (_uiState.value.faviconEditor as? DetailFaviconEditorState.Open)?.let(
            faviconSession::close,
        )
        clearSensitiveState()
    }

    private suspend fun stageFaviconInput(
        stage: suspend () -> Result<String>,
    ) {
        val editor = _uiState.value.faviconEditor as? DetailFaviconEditorState.Open ?: return
        if (editor.work == DetailFaviconWorkState.Processing) return
        mutate(DetailMutation.FaviconProcessingStarted)
        stage().fold(
            onSuccess = { mutate(DetailMutation.FaviconInputStaged(it)) },
            onFailure = { mutate(DetailMutation.FaviconProcessingFailed(it.toFaviconProcessingError())) },
        )
    }

    private suspend fun processPendingFavicon(crop: FaviconCropRequest?) {
        val editor = _uiState.value.faviconEditor as? DetailFaviconEditorState.Open ?: return
        val path = editor.pendingInputPath ?: return
        if (editor.work == DetailFaviconWorkState.Processing) return
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
