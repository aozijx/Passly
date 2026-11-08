package com.aozijx.passly.feature.vault.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.platform.media.FaviconCropRequest
import com.aozijx.passly.core.platform.media.FaviconImageProcessor
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.policy.EntryEdit
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.entry.policy.EntryTagNormalizationResult
import com.aozijx.passly.domain.entry.policy.EntryTagPolicy
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
import com.aozijx.passly.domain.entry.port.EntryTagQuery
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiState
import com.aozijx.passly.feature.vault.entry.EditEntryUseCase
import com.aozijx.passly.feature.vault.detail.EditableEntryLoadResult
import com.aozijx.passly.feature.vault.detail.ExportEntryQrUseCase
import com.aozijx.passly.feature.vault.detail.LoadEditableEntryUseCase
import com.aozijx.passly.feature.vault.detail.RevealEntryFieldsUseCase
import com.aozijx.passly.feature.vault.detail.RelatedPasswordLinks
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldResult
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldUseCase
import com.aozijx.passly.feature.vault.entry.CopyOtpCodeUseCase
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimeFactory
import com.aozijx.passly.feature.vault.presentation.detail.history.DetailRevisionWorkflowFactory
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailPresentationModel
import com.aozijx.passly.runtime.session.SessionLockedException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class DetailViewModel
    @Inject
    internal constructor(
        private val entryTagQuery: EntryTagQuery,
        private val activityQueryRepository: ActivityQueryRepository,
        exportEntryQr: ExportEntryQrUseCase,
        private val secureSessionAccessState: SecureSessionAccessState,
        private val revealEntryFields: RevealEntryFieldsUseCase,
        private val loadEditableEntry: LoadEditableEntryUseCase,
        private val editEntry: EditEntryUseCase,
        private val fieldReader: EntryFieldReader,
        private val copyEntryFieldUseCase: CopyEntryFieldUseCase,
        private val copyOtpCodeUseCase: CopyOtpCodeUseCase,
        private val faviconImageProcessor: FaviconImageProcessor,
        private val presentationLoader: DetailPresentationLoader,
        private val revisionWorkflowFactory: DetailRevisionWorkflowFactory,
        private val relatedPasswordLinks: RelatedPasswordLinks,
        private val logger: Logger,
        otpCodeRuntimeFactory: OtpCodeRuntimeFactory,
    ) : ViewModel() {
        private sealed interface SnapshotRead {
            data class Loaded(
                val snapshot: DetailSessionSnapshot,
            ) : SnapshotRead

            data object Missing : SnapshotRead

            data object SessionLocked : SnapshotRead

            data object Failed : SnapshotRead
        }

        private val revealStore = DetailRevealStore()
        private val faviconSession = DetailFaviconSession(faviconImageProcessor, viewModelScope)
        private val otpRuntime = otpCodeRuntimeFactory.create(viewModelScope)
        private val qrSession =
            DetailQrSession(viewModelScope, secureSessionAccessState) { entry, onAuthorized ->
                exportEntryQr(entry, onAuthorized)
            }
        val qrState = qrSession.state
        private var entryLoadJob: Job? = null
        private var historyJob: Job? = null
        private var packagePickerLoadJob: Job? = null
        private var tagEditorLoadJob: Job? = null
        private var snapshotRefreshJob: Job? = null
        private var historyObservationGeneration = 0L
        private var loadedEntryId: EntryId? = null
        private var loadedLaunchMode = DetailLaunchMode.VIEW

        private val _uiState = MutableStateFlow(DetailUiState())
        private val workSession =
            DetailWorkSession(
                hasAccess = secureSessionAccessState::hasFullSecureSessionAccess,
                editEntry = editEntry::edit,
                fieldReader = fieldReader,
                present = presentationLoader::present,
                onMutation = ::mutate,
                onReveal = ::setRevealedField,
            )
        private val revisionWorkflow =
            revisionWorkflowFactory.create(
                scope = viewModelScope,
                workSession = workSession,
                currentState = { _uiState.value.revisions },
                publishMutation = { mutate(DetailMutation.Revision(it)) },
                onRestoreSucceeded = ::onRevisionRestoreSucceeded,
            )
        private val passwordRelationsWorkflow =
            DetailPasswordRelationsWorkflow(
                scope = viewModelScope,
                links = relatedPasswordLinks,
                access = secureSessionAccessState,
                workSession = workSession,
                currentEntry = { _uiState.value.entry },
                currentBindingState = { _uiState.value.passwordBinding },
                currentRelatedPasswords = { _uiState.value.relatedPasswords },
                currentUnlinkState = { _uiState.value.passwordUnlink },
                logger = logger,
                presentationLoader = presentationLoader,
                publish = ::mutate,
            )
        val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
        private val _effects = Channel<DetailEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        suspend fun prepareEntryForEdit(): EntryEditorSeed.Editable? {
            val sourceEntry = _uiState.value.entry ?: return null
            if (!secureSessionAccessState.hasFullSecureSessionAccess()) return null
            val authentication = secureSessionAccessState.authenticationState.value
            val result =
                try {
                    loadEditableEntry(sourceEntry.id)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (isCurrentEditPreparation(sourceEntry.id, authentication)) {
                        _effects.send(DetailEffect.EditPreparationFailed(sourceEntry.id))
                    }
                    return null
                }
            if (result !is EditableEntryLoadResult.Loaded) return null
            if (result.entry.id != sourceEntry.id || result.entry.type != sourceEntry.type ||
                secureSessionAccessState.authenticationState.value !== result.authentication ||
                !secureSessionAccessState.hasFullSecureSessionAccess() ||
                _uiState.value.entry?.id != sourceEntry.id
            ) {
                return null
            }
            return EntryEditorSeed.Editable(result.entry, result.authentication)
        }

        private fun isCurrentEditPreparation(
            entryId: EntryId,
            authentication: AuthenticationState,
        ): Boolean =
            _uiState.value.entry?.id == entryId &&
                secureSessionAccessState.authenticationState.value === authentication &&
                secureSessionAccessState.hasFullSecureSessionAccess()

        private val _otpState = MutableStateFlow<OtpCodeState?>(null)
        val otpState: StateFlow<OtpCodeState?> = _otpState.asStateFlow()
        val presentation: StateFlow<DetailPresentationModel?> =
            combine(
                _uiState,
                _otpState,
                revisionWorkflow.revealedFields,
            ) { state, otp, revisionReveals ->
                toDetailPresentationModel(state, otp, revisionReveals)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

        init {
            viewModelScope.launch {
                secureSessionAccessState.authenticationState.collect {
                    if (!secureSessionAccessState.hasFullSecureSessionAccess()) clearSensitiveState()
                }
            }
            viewModelScope.launch {
                otpRuntime.states.collect { states ->
                    val token = loadedEntryId?.let(workSession::current)
                    _otpState.value = if (token != null && workSession.isCurrent(token)) states[token.entryId.value] else null
                }
            }
        }

        fun onAction(event: DetailUiAction) {
            if (event !is DetailUiAction.ClearSensitiveState && !secureSessionAccessState.hasFullSecureSessionAccess()) {
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
                    when (val plan = current.planDetailTitleEdit(state.editedTitle)) {
                        DetailTitleEditPlan.Cancel -> {
                            mutate(DetailMutation.TitleEditingCancelled)
                        }

                        is DetailTitleEditPlan.Save -> {
                            viewModelScope.launch {
                                persistEntryEdit(
                                    edit = plan.edit,
                                    completion = DetailEditCompletion.Title,
                                )
                            }
                        }
                    }
                }

                DetailUiAction.ToggleFavorite -> {
                    if (_uiState.value.entry == null) return
                    viewModelScope.launch {
                        persistEntryEdit(
                            edit = EntryEdit.ToggleFavorite,
                            completion = DetailEditCompletion.Favorite,
                        )
                    }
                }

                DetailUiAction.LoadPackagePickerApps -> {
                    loadPackagePickerApps()
                }

                is DetailUiAction.PackagePickerSearchChanged -> {
                    mutate(DetailMutation.PackagePickerSearchChanged(event.query))
                }

                DetailUiAction.RetryDetailLoad -> {
                    retryDetailLoad()
                }

                DetailUiAction.RetrySnapshotRefresh -> {
                    retrySnapshotRefresh()
                }

                is DetailUiAction.SelectAssociatedPackage -> {
                    if (_uiState.value.entry == null) return
                    viewModelScope.launch {
                        persistEntryEdit(
                            EntryEdit.SetApplicationIds(setOf(event.packageName)),
                            DetailEditCompletion.Applications,
                        )
                    }
                }

                DetailUiAction.StartNotesEdit -> {
                    val notes =
                        _uiState.value.entry
                            ?.secret
                            ?.notes
                            .orEmpty()
                    mutate(DetailMutation.FieldEditingStarted(DetailDraftKey.Notes, notes))
                }

                is DetailUiAction.UpdateNotesDraft -> {
                    mutate(DetailMutation.FieldDraftChanged(DetailDraftKey.Notes, event.value))
                }

                DetailUiAction.SaveNotes -> {
                    val notes = _uiState.value.fieldEdits.draft(DetailDraftKey.Notes)
                    viewModelScope.launch {
                        persistEntryEdit(
                            EntryEdit.SetNotes(notes.ifBlank { null }),
                            DetailEditCompletion.Notes,
                        )
                    }
                }

                DetailUiAction.StartDomainEdit -> {
                    val domain =
                        _uiState.value.entry
                            ?.associations
                            ?.primaryUrl
                            .orEmpty()
                    mutate(DetailMutation.FieldEditingStarted(DetailDraftKey.Domain, domain))
                }

                is DetailUiAction.UpdateDomainDraft -> {
                    mutate(DetailMutation.FieldDraftChanged(DetailDraftKey.Domain, event.value))
                }

                DetailUiAction.SaveDomain -> {
                    val state = _uiState.value
                    if (state.entry == null) return
                    val domain = state.fieldEdits.draft(DetailDraftKey.Domain)
                    viewModelScope.launch {
                        persistEntryEdit(
                            EntryEdit.SetPrimaryUrl(domain.trim().ifBlank { null }),
                            DetailEditCompletion.Domain,
                        )
                    }
                }

                is DetailUiAction.StartFieldEdit -> {
                    mutate(DetailMutation.FieldEditingStarted(event.field.draftKey, event.initialValue))
                }

                is DetailUiAction.UpdateFieldDraft -> {
                    mutate(DetailMutation.FieldDraftChanged(event.field.draftKey, event.value))
                }

                is DetailUiAction.CancelFieldEdit -> {
                    mutate(DetailMutation.FieldEditingCancelled(event.field.draftKey))
                }

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

                DetailUiAction.RevealBankCardFields -> {
                    revealFieldsFromState(_uiState.value.bankCardFieldsToReveal())
                }

                DetailUiAction.RevealSshFields -> {
                    revealFieldsFromState(_uiState.value.sshFieldsToReveal())
                }

                is DetailUiAction.SaveField -> {
                    val state = _uiState.value
                    if (state.entry == null) return
                    if (
                        state.fieldEdits.initialValue(event.field.draftKey)?.takeIf(String::isNotEmpty) == event.newValue
                    ) {
                        return
                    }
                    viewModelScope.launch {
                        val fieldKey = event.field.fieldKey
                        persistEntryEdit(
                            edit = EntryEdit.SetFieldValue(fieldKey, event.newValue),
                            completion = DetailEditCompletion.Field(fieldKey),
                        )
                    }
                }

                DetailUiAction.OpenTagEditor -> {
                    openTagEditor()
                }

                is DetailUiAction.UpdateTagInput -> {
                    if (event.value.any { it == ',' || it == '\n' || it == '\r' }) {
                        mutate(DetailMutation.TagSubmitted(event.value))
                    } else {
                        mutate(DetailMutation.TagInputChanged(event.value))
                    }
                }

                is DetailUiAction.SubmitTag -> {
                    submitTag(event.value)
                }

                is DetailUiAction.RemoveTag -> {
                    mutate(DetailMutation.TagRemoved(event.value))
                }

                DetailUiAction.SaveTags -> {
                    val editor = _uiState.value.tagEditor as? DetailTagEditorState.Open ?: return
                    when (
                        val normalized =
                            EntryTagPolicy.normalize(
                                editor.draftTags + editor.input,
                            )
                    ) {
                        is EntryTagNormalizationResult.Valid -> {
                            viewModelScope.launch {
                                persistEntryEdit(
                                    edit = EntryEdit.SetTags(normalized.tags),
                                    completion = DetailEditCompletion.Tags,
                                )
                            }
                        }

                        is EntryTagNormalizationResult.Invalid -> {
                            mutate(DetailMutation.TagSubmitted(editor.input))
                        }
                    }
                }

                DetailUiAction.DismissTagEditor -> {
                    tagEditorLoadJob?.cancel()
                    mutate(DetailMutation.TagEditorDismissRequested)
                }

                DetailUiAction.ConfirmDiscardTags -> {
                    tagEditorLoadJob?.cancel()
                    mutate(DetailMutation.TagEditorDiscardConfirmed)
                }

                DetailUiAction.KeepEditingTags -> {
                    mutate(DetailMutation.TagEditorDiscardCancelled)
                }

                DetailUiAction.OpenFaviconEditor -> {
                    if (faviconSession.isSaving) return
                    val icon = _uiState.value.entry?.icon ?: return
                    (_uiState.value.faviconEditor as? DetailFaviconEditorState.Open)?.let(faviconSession::close)
                    mutate(DetailMutation.FaviconEditorOpened(icon.toDetailFaviconSource()))
                }

                is DetailUiAction.SelectFaviconSource -> {
                    if (faviconSession.isSaving) return
                    val editor =
                        _uiState.value.faviconEditor as? DetailFaviconEditorState.Open
                            ?: return
                    val source = event.source
                    faviconSession.discardReplacedSource(editor, source)
                    mutate(DetailMutation.FaviconSourceChanged(source))
                }

                is DetailUiAction.SelectFaviconTab -> {
                    mutate(DetailMutation.FaviconTabChanged(event.tab))
                }

                is DetailUiAction.UpdateFaviconSearch -> {
                    mutate(DetailMutation.FaviconSearchChanged(event.value))
                }

                is DetailUiAction.UpdateFaviconImageUrl -> {
                    mutate(DetailMutation.FaviconImageUrlChanged(event.value))
                }

                is DetailUiAction.PickedFaviconImage -> {
                    faviconSession.launchPreparation { token ->
                        stageFaviconInput(token) { faviconImageProcessor.stageUpload(event.uri) }
                    }
                }

                DetailUiAction.DownloadFaviconImage -> {
                    val editor =
                        _uiState.value.faviconEditor as? DetailFaviconEditorState.Open
                            ?: return
                    val url = editor.imageUrl
                    faviconSession.launchPreparation { token ->
                        stageFaviconInput(token) { faviconImageProcessor.stageHttpsUrl(url) }
                    }
                }

                DetailUiAction.UseFaviconWithoutCrop -> {
                    faviconSession.launchPreparation { token -> processPendingFavicon(token, crop = null) }
                }

                is DetailUiAction.CropFaviconImage -> {
                    faviconSession.launchPreparation { token ->
                        processPendingFavicon(
                            token,
                            FaviconCropRequest(event.zoom, event.offsetX, event.offsetY),
                        )
                    }
                }

                DetailUiAction.CancelFaviconCrop -> {
                    if (faviconSession.isSaving) return
                    val editor =
                        _uiState.value.faviconEditor as? DetailFaviconEditorState.Open
                            ?: return
                    if (editor.work == DetailFaviconWorkState.Processing) return
                    faviconSession.cancelCrop(editor.pendingInputPath)
                    mutate(DetailMutation.FaviconCropCancelled)
                }

                DetailUiAction.SaveFavicon -> {
                    saveFavicon()
                }

                DetailUiAction.DismissFaviconEditor -> {
                    val editor =
                        _uiState.value.faviconEditor as? DetailFaviconEditorState.Open
                            ?: return
                    if (editor.work == DetailFaviconWorkState.Processing) return
                    mutate(DetailMutation.FaviconEditorDismissRequested)
                    if (_uiState.value.faviconEditor == DetailFaviconEditorState.Closed) {
                        faviconSession.close(editor)
                    }
                }

                DetailUiAction.ConfirmDiscardFavicon -> {
                    val editor =
                        _uiState.value.faviconEditor as? DetailFaviconEditorState.Open
                            ?: return
                    faviconSession.close(editor)
                    mutate(DetailMutation.FaviconEditorDiscardConfirmed)
                }

                DetailUiAction.KeepEditingFavicon -> {
                    mutate(DetailMutation.FaviconEditorDiscardCancelled)
                }

                is DetailUiAction.CopyField -> {
                    copyField(event.field.fieldKey)
                }

                DetailUiAction.CopyOtpCode -> {
                    copyOtpCode()
                }

                DetailUiAction.ShowEntryQr -> {
                    _uiState.value.entry?.let(qrSession::request)
                }

                DetailUiAction.DismissEntryQr -> {
                    qrSession.close()
                }

                is DetailUiAction.ToggleAccessHistoryRecording -> {
                    mutate(DetailMutation.AccessHistoryChanged(event.enabled))
                }

                DetailUiAction.ClearSensitiveState -> {
                    clearSensitiveState()
                }

                DetailUiAction.OpenRevisionHistory -> {
                    _uiState.value.entry?.id?.let(revisionWorkflow::open)
                }

                DetailUiAction.RetryRevisionHistory -> {
                    revisionWorkflow.retry()
                }

                DetailUiAction.DismissRevisionHistory -> {
                    revisionWorkflow.close()
                }

                is DetailUiAction.SelectRevision -> {
                    revisionWorkflow.select(event.revisionId)
                }

                DetailUiAction.RevisionBack -> {
                    revisionWorkflow.back()
                }

                is DetailUiAction.RevealRevisionField -> {
                    revisionWorkflow.reveal(event.key)
                }

                DetailUiAction.RequestRevisionRestore -> {
                    revisionWorkflow.requestRestore()
                }

                DetailUiAction.CancelRevisionRestore -> {
                    revisionWorkflow.cancelRestore()
                }

                DetailUiAction.ConfirmRevisionRestore -> {
                    revisionWorkflow.restore()
                }

                DetailUiAction.RetryActivityHistory -> {
                    retryActivityHistory()
                }

                is DetailUiAction.ActivityFilterChanged -> {
                    mutate(DetailMutation.ActivityFilterChanged(event.filter))
                }

                DetailUiAction.OpenPasswordBinding,
                DetailUiAction.RetryPasswordBinding,
                -> openPasswordBinding()

                is DetailUiAction.PasswordBindingSearchChanged -> {
                    mutate(DetailMutation.PasswordBindingSearchChanged(event.query))
                }

                DetailUiAction.DismissPasswordBinding -> {
                    passwordRelationsWorkflow.dismiss()
                }

                is DetailUiAction.RequestPasswordUnlink -> {
                    passwordRelationsWorkflow.requestUnlink(EntryId(event.targetEntryId), EntryLinkId(event.linkId))
                }

                DetailUiAction.DismissPasswordUnlink -> {
                    passwordRelationsWorkflow.dismissUnlink()
                }

                DetailUiAction.ConfirmPasswordUnlink -> {
                    passwordRelationsWorkflow.confirmUnlink()
                }

                is DetailUiAction.BindPassword -> passwordRelationsWorkflow.bind(EntryId(event.targetEntryId))
            }
        }

        private fun openPasswordBinding() = passwordRelationsWorkflow.open()

        private fun copyField(fieldKey: FieldKey) {
            val entry = _uiState.value.entry ?: return
            val token = workSession.current(entry.id) ?: return
            viewModelScope.launch {
                if (copyEntryFieldUseCase(
                        entry.id,
                        entry.type,
                        fieldKey,
                        canDeliver = { workSession.isCurrent(token) },
                    ) != CopyEntryFieldResult.Copied
                ) {
                    return@launch
                }
                if (!workSession.isCurrent(token)) return@launch
                clearRevealedFields()
                _effects.send(DetailEffect.ContentCopied(fieldKey))
            }
        }

        private fun revealFieldsFromState(fields: Set<DetailField>) {
            val current = _uiState.value.entry ?: return
            val hiddenKeys =
                fields
                    .mapTo(linkedSetOf()) { it.fieldKey }
                    .filterTo(linkedSetOf()) { _uiState.value.revealed(it) == null }
            if (hiddenKeys.isEmpty()) return
            viewModelScope.launch { revealFields(current, hiddenKeys) }
        }

        private fun copyOtpCode() {
            val entry = _uiState.value.entry ?: return
            val token = workSession.current(entry.id) ?: return
            viewModelScope.launch {
                if (
                    copyOtpCodeUseCase(
                        entry.id,
                        canDeliver = { workSession.isCurrent(token) },
                    ) {
                        if (workSession.isCurrent(token)) otpRuntime.codeForCopy(entry.id.value) else null
                    } != CopyEntryFieldResult.Copied
                ) {
                    return@launch
                }
                if (!workSession.isCurrent(token)) return@launch
                clearRevealedFields()
                _effects.send(DetailEffect.OtpCopied)
            }
        }

        private fun clearRevealedFields() {
            revealStore.clear()
            mutate(DetailMutation.RevealedFieldsCleared)
        }

        fun load(
            rawEntryId: String,
            launchMode: DetailLaunchMode = DetailLaunchMode.VIEW,
        ) {
            val entryId = EntryId(rawEntryId)
            if (loadedEntryId == entryId && _uiState.value.entry != null) return
            val token = workSession.open(entryId)
            tagEditorLoadJob?.cancel()
            (_uiState.value.faviconEditor as? DetailFaviconEditorState.Open)?.let(faviconSession::close)
            qrSession.close()
            loadedEntryId = entryId
            loadedLaunchMode = launchMode
            entryLoadJob?.cancel()
            snapshotRefreshJob?.cancel()
            historyJob?.cancel()
            packagePickerLoadJob?.cancel()
            passwordRelationsWorkflow.close()
            revisionWorkflow.reset()
            otpRuntime.clearAllSensitiveState()
            _otpState.value = null
            revealStore.clear()
            mutate(DetailMutation.StateCleared)
            entryLoadJob =
                viewModelScope.launch {
                    if (!secureSessionAccessState.hasFullSecureSessionAccess()) return@launch
                    val snapshot =
                        when (val result = readSnapshot(entryId)) {
                            is SnapshotRead.Loaded -> {
                                result.snapshot
                            }

                            SnapshotRead.Missing -> {
                                if (workSession.isCurrent(token)) mutate(DetailMutation.SnapshotNotFound)
                                return@launch
                            }

                            SnapshotRead.SessionLocked -> {
                                return@launch
                            }

                            SnapshotRead.Failed -> {
                                if (workSession.isCurrent(token)) mutate(DetailMutation.SnapshotLoadFailed)
                                return@launch
                            }
                        }
                    if (!workSession.isCurrent(token)) return@launch
                    val latest = snapshot.presentation.entry
                    mutate(DetailMutation.SessionOpened(snapshot))
                    observeActivityHistory(entryId, token)
                    if (launchMode != DetailLaunchMode.VIEW) {
                        when {
                            latest.username.isNotEmpty() -> {
                                mutate(
                                    DetailMutation.FieldEditingStarted(
                                        DetailDraftKey.EntryField(FieldKey.USERNAME),
                                        latest.username,
                                    ),
                                )
                            }

                            SensitiveFieldKey.PASSWORD in snapshot.presentation.sensitiveFieldKeys -> {
                                mutate(
                                    DetailMutation.FieldEditingStarted(
                                        DetailDraftKey.EntryField(FieldKey.PASSWORD),
                                        "",
                                    ),
                                )
                            }
                        }
                    }
                    if (latest.secret.otp != null) otpRuntime.autoUnlock(entryId.value)
                }
        }

        private fun retryDetailLoad() {
            if (_uiState.value.snapshotState != DetailSnapshotState.Failed) return
            val entryId = loadedEntryId ?: return
            load(entryId.value, loadedLaunchMode)
        }

        internal fun refreshOtpConfiguration() {
            val entry = _uiState.value.entry ?: return
            if (entry.type != EntryType.OTP) return
            val token = workSession.current(entry.id) ?: return
            if (!workSession.isCurrent(token)) return
            otpRuntime.entryChanged(entry.id.value)
        }

        private fun retrySnapshotRefresh() {
            if (_uiState.value.snapshotState != DetailSnapshotState.RefreshFailedAfterRestore) return
            val entryId = _uiState.value.entry?.id ?: return
            val token = workSession.current(entryId) ?: return
            refreshSnapshotAfterRestore(entryId, token)
        }

        private fun onRevisionRestoreSucceeded(
            entryId: EntryId,
            token: DetailWorkSession.Scope.Active,
        ) {
            if (!workSession.isCurrent(token) || _uiState.value.entry?.id != entryId) return
            if (_uiState.value.entry?.type == EntryType.OTP) otpRuntime.entryChanged(entryId.value)
            refreshSnapshotAfterRestore(entryId, token)
        }

        private fun refreshSnapshotAfterRestore(
            entryId: EntryId,
            token: DetailWorkSession.Scope.Active,
        ) {
            snapshotRefreshJob?.cancel()
            mutate(DetailMutation.SnapshotRefreshStarted)
            snapshotRefreshJob =
                viewModelScope.launch {
                    if (!secureSessionAccessState.hasFullSecureSessionAccess()) return@launch
                    val snapshot =
                        when (val result = readSnapshot(entryId)) {
                            is SnapshotRead.Loaded -> {
                                result.snapshot
                            }

                            SnapshotRead.Missing, SnapshotRead.Failed -> {
                                if (workSession.isCurrent(token)) mutate(DetailMutation.SnapshotRefreshFailed)
                                return@launch
                            }

                            SnapshotRead.SessionLocked -> {
                                return@launch
                            }
                        }
                    if (!workSession.isCurrent(token) || !currentCoroutineContext().isActive) return@launch
                    mutate(DetailMutation.SessionOpened(snapshot))
                }
        }

        private suspend fun readSnapshot(entryId: EntryId): SnapshotRead =
            try {
                presentationLoader.open(entryId)?.let(SnapshotRead::Loaded) ?: SnapshotRead.Missing
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SessionLockedException) {
                SnapshotRead.SessionLocked
            } catch (_: Exception) {
                SnapshotRead.Failed
            }

        private fun retryActivityHistory() {
            val state = _uiState.value
            val entryId = state.entry?.id ?: return
            if (state.activityHistory !is DetailActivityHistoryState.Failed) return
            val token = workSession.current(entryId) ?: return
            observeActivityHistory(entryId, token)
        }

        private fun observeActivityHistory(
            entryId: EntryId,
            token: DetailWorkSession.Scope.Active,
        ) {
            historyJob?.cancel()
            val generation = ++historyObservationGeneration
            mutate(DetailMutation.HistoryLoadStarted(entryId))
            historyJob =
                viewModelScope.launch {
                    if (!secureSessionAccessState.hasFullSecureSessionAccess()) return@launch
                    try {
                        activityQueryRepository.observeByEntryId(entryId.value).collect { history ->
                            if (workSession.isCurrent(token) && generation == historyObservationGeneration) {
                                mutate(DetailMutation.HistoryChanged(entryId, history))
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: SessionLockedException) {
                        // Session closure clears this screen through the access-state observer.
                    } catch (_: Exception) {
                        if (workSession.isCurrent(token) && generation == historyObservationGeneration) {
                            mutate(DetailMutation.HistoryLoadFailed(entryId))
                        }
                    }
                }
        }

        private fun setRevealedField(
            key: FieldKey,
            value: SensitiveValue?,
        ) {
            revealStore.replace(key, value)
            mutate(DetailMutation.RevealedFieldChanged(key, value))
        }

        private suspend fun revealFields(
            entry: Entry,
            requested: Set<FieldKey>,
        ) {
            if (!secureSessionAccessState.hasFullSecureSessionAccess()) return
            if (requested.isEmpty()) return
            val token = workSession.current(entry.id) ?: return
            val revealed =
                revealEntryFields.reveal(
                    entry = entry,
                    requestedFields = requested,
                    recordLowSensitivityAccess = _uiState.value.isAccessHistoryEnabled,
                )
            workSession.publishReveals(token, revealed.toList(), ::setRevealedField)
        }

        private fun clearSensitiveState() {
            tagEditorLoadJob?.cancel()
            (_uiState.value.faviconEditor as? DetailFaviconEditorState.Open)?.let(faviconSession::close)
            workSession.close()
            entryLoadJob?.cancel()
            snapshotRefreshJob?.cancel()
            historyJob?.cancel()
            packagePickerLoadJob?.cancel()
            passwordRelationsWorkflow.close()
            qrSession.close()
            revealStore.clear()
            revisionWorkflow.reset()
            otpRuntime.clearAllSensitiveState()
            _otpState.value = null
            mutate(DetailMutation.StateCleared)
        }

        private fun openTagEditor() {
            val state = _uiState.value
            if (state.tagEditor is DetailTagEditorState.Open) return
            val entryId = state.entry?.id ?: return
            val token = workSession.current(entryId) ?: return
            tagEditorLoadJob?.cancel()
            tagEditorLoadJob =
                viewModelScope.launch {
                    workSession.openTagEditor(token, entryTagQuery::findAllTags) { _uiState.value.entry }
                }
        }

        private fun submitTag(value: String) {
            if (_uiState.value.saveState != DetailSaveState.Idle || value.isBlank()) return
            mutate(DetailMutation.TagSubmitted(value))
        }

        private suspend fun persistEntryEdit(
            edit: EntryEdit,
            completion: DetailEditCompletion,
        ) {
            val entryId = _uiState.value.entry?.id ?: return
            if (_uiState.value.saveState != DetailSaveState.Idle) return
            val token = workSession.current(entryId) ?: return
            mutate(DetailMutation.SaveStarted(completion))
            workSession.persist(token, edit, completion)
        }

        private fun saveFavicon() {
            val state = _uiState.value
            val editor = state.faviconEditor as? DetailFaviconEditorState.Open ?: return
            val entryId = state.entry?.id ?: return
            val token = workSession.current(entryId) ?: return
            if (
                editor.work == DetailFaviconWorkState.Processing ||
                !editor.dirty ||
                state.saveState != DetailSaveState.Idle ||
                !secureSessionAccessState.hasFullSecureSessionAccess()
            ) {
                return
            }

            mutate(DetailMutation.SaveStarted(DetailEditCompletion.Icon))
            faviconSession.launchSave {
                val source =
                    (_uiState.value.faviconEditor as? DetailFaviconEditorState.Open)
                        ?.source ?: return@launchSave
                val persistedSource =
                    if (
                        source is DetailFaviconSource.PrivateImage &&
                        faviconImageProcessor.isStaged(source.localPath)
                    ) {
                        val promoted =
                            faviconImageProcessor
                                .promote(source.localPath)
                                .getOrElse { error ->
                                    if (!workSession.isCurrent(token)) return@launchSave
                                    val processingError = error.toFaviconProcessingError()
                                    mutate(DetailMutation.FaviconProcessingFailed(processingError))
                                    mutate(
                                        DetailMutation.SaveFailed(
                                            DetailEditCompletion.Icon,
                                        ),
                                    )
                                    return@launchSave
                                }
                        if (!workSession.isCurrent(token)) {
                            withContext(NonCancellable) { faviconImageProcessor.discardPromotedCandidate(promoted) }
                            return@launchSave
                        }
                        DetailFaviconSource.PrivateImage(promoted).also {
                            mutate(DetailMutation.FaviconSourcePromoted(promoted))
                        }
                    } else {
                        source
                    }
                workSession.persist(
                    token = token,
                    edit = EntryEdit.SetIcon(persistedSource.toEntryIcon()),
                    completion = DetailEditCompletion.Icon,
                )
            }
        }

        private fun loadPackagePickerApps() {
            val state = _uiState.value
            val entryId = state.entry?.id ?: return
            val token = workSession.current(entryId) ?: return
            if (state.packagePickerApps.catalogLoaded) {
                mutate(DetailMutation.PackagePickerSearchChanged(""))
                return
            }
            if (state.packagePickerApps == AppPackagePickerUiState.Loading) return
            if (packagePickerLoadJob?.isActive == true) return
            mutate(DetailMutation.PackagePickerAppsLoadStarted(entryId))
            packagePickerLoadJob =
                viewModelScope.launch {
                    try {
                        val apps = presentationLoader.launchableApps()
                        if (workSession.isCurrent(token)) {
                            mutate(DetailMutation.PackagePickerAppsChanged(entryId, apps))
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        if (workSession.isCurrent(token)) {
                            mutate(DetailMutation.PackagePickerAppsFailed(entryId))
                        }
                    }
                }
        }

        private fun mutate(mutation: DetailMutation) {
            _uiState.value = DetailReducer.reduce(_uiState.value, mutation)
        }

        override fun onCleared() {
            clearSensitiveState()
        }

        private suspend fun stageFaviconInput(
            token: DetailFaviconSession.Preparation.Active,
            stage: suspend () -> Result<String>,
        ) {
            val editor = _uiState.value.faviconEditor as? DetailFaviconEditorState.Open ?: return
            if (editor.work == DetailFaviconWorkState.Processing) return
            mutate(DetailMutation.FaviconProcessingStarted)
            faviconSession.publishPreparation(
                token = token,
                result = stage(),
                editor = editor,
                use = DetailFaviconSession.PreparedImageUse.CROP_INPUT,
                onSuccess = { mutate(DetailMutation.FaviconInputStaged(it)) },
                onFailure = { mutate(DetailMutation.FaviconProcessingFailed(it.toFaviconProcessingError())) },
            )
        }

        private suspend fun processPendingFavicon(
            token: DetailFaviconSession.Preparation.Active,
            crop: FaviconCropRequest?,
        ) {
            val editor = _uiState.value.faviconEditor as? DetailFaviconEditorState.Open ?: return
            val path = editor.pendingInputPath ?: return
            if (editor.work == DetailFaviconWorkState.Processing) return
            mutate(DetailMutation.FaviconProcessingStarted)
            faviconSession.publishPreparation(
                token = token,
                result = faviconImageProcessor.process(path, crop),
                editor = editor,
                use = DetailFaviconSession.PreparedImageUse.SOURCE,
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
