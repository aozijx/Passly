package com.aozijx.passly.presentation.feature.vault.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.clipboard.ClipboardCopyController
import com.aozijx.passly.app.entry.favicon.FaviconCropRequest
import com.aozijx.passly.app.entry.favicon.FaviconDownloadException
import com.aozijx.passly.app.entry.favicon.FaviconDownloadFailure
import com.aozijx.passly.app.entry.favicon.FaviconImageException
import com.aozijx.passly.app.entry.favicon.FaviconImageFailure
import com.aozijx.passly.app.entry.favicon.FaviconImageProcessor
import com.aozijx.passly.app.entry.favicon.FaviconUrlException
import com.aozijx.passly.app.entry.favicon.FaviconUrlFailure
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.policy.EntryTypePolicy
import com.aozijx.passly.domain.entry.policy.EntryAccountGraph
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.feature.vault.detail.DetailEffect
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailEntryAnalyzer
import com.aozijx.passly.presentation.feature.vault.detail.DetailMutation
import com.aozijx.passly.presentation.feature.vault.detail.DetailReducer
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconProcessingErrorUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
    private val activityQueryRepository: ActivityQueryRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val entryLinkRepository: EntryLinkRepository,
    private val activityRecorder: ActivityRecorder,
    private val entryTypePolicy: EntryTypePolicy,
    private val accessPolicy: DetailAccessPolicy,
    private val authorizationGate: AuthorizationGate,
    private val clipboardCopyController: ClipboardCopyController,
    private val faviconImageProcessor: FaviconImageProcessor,
) : ViewModel() {
    private val entryAnalyzer = DetailEntryAnalyzer(entryTypePolicy)
    private val revealStore = DetailRevealStore()
    private val updateCoordinator = DetailEntryUpdateCoordinator(
        entryQueryRepository = entryQueryRepository,
        entryCommandRepository = entryCommandRepository,
    )
    private val otpQrExporter = DetailOtpQrExporter(
        authorizationGate = authorizationGate,
        sensitiveFieldRepository = sensitiveFieldRepository,
    )
    private var pendingPromotedFaviconPath: String? = null
    private var faviconJob: Job? = null

    companion object {
        private const val ACCESS_HISTORY_TOGGLE_KEY = "detail.access_history_enabled"
    }

    private val userConfigExtras = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private val _effects = Channel<DetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun copySensitive(text: String) {
        viewModelScope.launch { clipboardCopyController.copySensitive(text) }
    }

    init {
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
            is DetailUiAction.Initialize -> {
                initialize(event.initialEntry)
            }

            is DetailUiAction.SyncEntry -> {
                refreshKeepingTitleEdit(event.entry)
            }

            is DetailUiAction.CommitPatch -> {
                viewModelScope.launch {
                    persistEntryPatch(event.patch, event.completion)
                }
            }

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

            is DetailUiAction.RevealField -> {
                setRevealedField(event.key, event.value)
            }

            is DetailUiAction.ToggleVisibility -> {
                val current = _uiState.value.revealed(event.key)
                if (current != null) {
                    setRevealedField(event.key, null)
                } else {
                    handleRevealLogic(event.key)
                }
            }

            is DetailUiAction.SaveField -> {
                _uiState.value.entry ?: return
                viewModelScope.launch {
                    val patch = when (event.key) {
                        RevealedFieldKey.USERNAME -> DetailEntryPatch.Username(event.newValue)
                        RevealedFieldKey.PASSWORD -> DetailEntryPatch.LoginPassword(event.newValue)
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
                mutate(DetailMutation.FaviconEditorOpened(icon.toFaviconDraftSource()))
            }

            is DetailUiAction.SelectFaviconSource -> {
                val previous = _uiState.value.faviconEditor.source
                if (previous is FaviconDraftSourceUiModel.PrivateImage && previous != event.source) {
                    viewModelScope.launch { faviconImageProcessor.discard(previous.localPath) }
                }
                pendingPromotedFaviconPath?.let { path ->
                    viewModelScope.launch { faviconImageProcessor.discardPromotedCandidate(path) }
                    pendingPromotedFaviconPath = null
                }
                mutate(DetailMutation.FaviconSourceChanged(event.source))
            }

            is DetailUiAction.SelectFaviconTab ->
                mutate(DetailMutation.FaviconTabChanged(event.tab))

            is DetailUiAction.UpdateFaviconSearch ->
                mutate(DetailMutation.FaviconSearchChanged(event.value))

            is DetailUiAction.UpdateFaviconImageUrl ->
                mutate(DetailMutation.FaviconImageUrlChanged(event.value))

            is DetailUiAction.PickedFaviconImage -> {
                launchFaviconJob {
                    stageFaviconInput { faviconImageProcessor.stageUpload(event.uri) }
                }
            }

            DetailUiAction.DownloadFaviconImage -> {
                val url = _uiState.value.faviconEditor.imageUrl
                launchFaviconJob {
                    stageFaviconInput { faviconImageProcessor.stageHttpsUrl(url) }
                }
            }

            DetailUiAction.UseFaviconWithoutCrop -> {
                launchFaviconJob { processPendingFavicon(crop = null) }
            }

            is DetailUiAction.CropFaviconImage -> {
                launchFaviconJob {
                    processPendingFavicon(
                        FaviconCropRequest(event.zoom, event.offsetX, event.offsetY),
                    )
                }
            }

            DetailUiAction.CancelFaviconCrop -> {
                if (_uiState.value.faviconEditor.processing) return
                val pending = _uiState.value.faviconEditor.pendingInputPath
                faviconJob?.cancel()
                faviconJob = null
                viewModelScope.launch { faviconImageProcessor.discard(pending) }
                mutate(DetailMutation.FaviconCropCancelled)
            }

            DetailUiAction.SaveFavicon -> {
                if (_uiState.value.faviconEditor.processing) return
                faviconJob = viewModelScope.launch {
                    val source = _uiState.value.faviconEditor.source
                    val persistedSource = if (
                        source is FaviconDraftSourceUiModel.PrivateImage &&
                        faviconImageProcessor.isStaged(source.localPath)
                    ) {
                        val promoted = faviconImageProcessor.promote(source.localPath)
                            .getOrElse {
                                mutate(DetailMutation.FaviconProcessingFailed(it.toFaviconUiError()))
                                return@launch
                            }
                        FaviconDraftSourceUiModel.PrivateImage(promoted).also {
                            pendingPromotedFaviconPath = promoted
                            mutate(DetailMutation.FaviconSourceChanged(it))
                        }
                    } else {
                        source
                    }
                    persistEntryPatch(
                        patch = DetailEntryPatch.Icon(persistedSource.toEntryIcon()),
                        completion = DetailEditCompletion.Icon,
                    )
                    if (!_uiState.value.faviconEditor.visible) {
                        pendingPromotedFaviconPath = null
                    }
                }
            }

            DetailUiAction.DismissFaviconEditor -> {
                if (_uiState.value.faviconEditor.processing) return
                val editor = _uiState.value.faviconEditor
                mutate(DetailMutation.FaviconEditorDismissRequested)
                if (!_uiState.value.faviconEditor.visible) {
                    cancelFaviconWorkAndDiscard(editor)
                }
            }

            DetailUiAction.ConfirmDiscardFavicon -> {
                val editor = _uiState.value.faviconEditor
                cancelFaviconWorkAndDiscard(editor)
                mutate(DetailMutation.FaviconEditorDiscardConfirmed)
            }

            DetailUiAction.KeepEditingFavicon ->
                mutate(DetailMutation.FaviconEditorDiscardCancelled)

            DetailUiAction.ExportOtpQr -> {
                val entry = _uiState.value.entry ?: return
                viewModelScope.launch {
                    otpQrExporter.export(entry)?.let { uri ->
                        _effects.send(DetailEffect.ShowOtpQr(uri))
                        activityRecorder.recordUsage(entry.id.value, ActivityType.VIEW)
                    }
                }
            }

            is DetailUiAction.RevealHighSensitivityField -> {
                val current = _uiState.value.entry ?: return
                val key = event.key
                if (_uiState.value.revealed(key) != null) {
                    mutate(DetailMutation.RevealedFieldChanged(key, null))
                    return
                }
                viewModelScope.launch {
                    revealHighSensitivityFields(current.id, setOf(key))
                }
            }

            is DetailUiAction.RevealHighSensitivityFields -> {
                val current = _uiState.value.entry ?: return
                val hiddenKeys = event.keys.filterTo(linkedSetOf()) {
                    _uiState.value.revealed(it) == null
                }
                if (hiddenKeys.isEmpty()) return
                viewModelScope.launch {
                    revealHighSensitivityFields(current.id, hiddenKeys)
                }
            }

            is DetailUiAction.RecordAction -> {
                val current = _uiState.value.entry ?: return
                if (event.type == ActivityType.VIEW && !_uiState.value.isAccessHistoryEnabled) return
                if (event.type.clearsRevealedFields()) {
                    mutate(DetailMutation.RevealedFieldsCleared)
                }

                viewModelScope.launch {
                    if (!accessPolicy.hasFullAccess()) return@launch
                    activityRecorder.recordUsage(current.id.value, event.type)
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

    private fun handleRevealLogic(key: String) {
        val entry = _uiState.value.entry ?: return
        when (key) {
            RevealedFieldKey.USERNAME -> {
                setRevealedField(key, OwnedChars.fromNullableString(entry.username))
                onAction(DetailUiAction.RecordAction("username", ActivityType.VIEW))
            }

            RevealedFieldKey.PASSWORD -> {
                onAction(DetailUiAction.RevealHighSensitivityField(key))
            }
        }
    }

    private fun initialize(initialEntry: Entry) {
        refreshFromEntry(initialEntry, isEditingTitle = false, editedTitle = initialEntry.title)
        viewModelScope.launch {
            if (!accessPolicy.hasFullAccess()) return@launch
            val latest = entryQueryRepository.getById(initialEntry.id)
                ?: initialEntry
            refreshFromEntry(latest, isEditingTitle = false, editedTitle = latest.title)
            val presence = sensitiveFieldRepository.getPresence(latest.id)
            mutate(DetailMutation.SensitiveFieldPresenceChanged(presence.keys))
            loadRelatedEntries(latest)
        }
        viewModelScope.launch {
            if (!accessPolicy.hasFullAccess()) return@launch
            activityQueryRepository.observeByEntryId(initialEntry.id.value)
                .collect { history -> mutate(DetailMutation.HistoryChanged(history)) }
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

    private suspend fun revealHighSensitivityFields(entryValue: EntryId, uiKeys: Set<String>) {
        if (!accessPolicy.hasFullAccess()) return
        val requested = uiKeys.mapNotNull { uiKey ->
            uiKey.toSensitiveFieldKey()?.let { fieldKey -> uiKey to fieldKey }
        }.toMap()
        if (requested.isEmpty()) return
        authorizationGate.authorize(
            AuthorizationScope.SensitiveFields(
                entryId = entryValue,
                fieldKeys = requested.values.toSet(),
                action = SensitiveAccessAction.REVEAL,
            ),
        ) authorize@{ permit ->
            val revealedFields = sensitiveFieldRepository.revealMany(
                entryId = entryValue,
                keys = requested.values.toSet(),
                permit = permit,
            )
            revealedFields.forEach { revealed ->
                val uiKey = requested.entries.firstOrNull { it.value == revealed.key }?.key
                    ?: return@forEach
                setRevealedField(uiKey, revealed.value)
            }
            if (revealedFields.isNotEmpty()) {
                activityRecorder.recordUsage(entryValue.value, ActivityType.VIEW)
            }
        }
    }

    private fun clearSensitiveState() {
        revealStore.clear()
        mutate(DetailMutation.StateCleared)
    }

    private fun String.toSensitiveFieldKey(): SensitiveFieldKey? = when (this) {
        RevealedFieldKey.PASSWORD -> SensitiveFieldKey.PASSWORD
        RevealedFieldKey.CARD_NUMBER -> SensitiveFieldKey.CARD_NUMBER
        RevealedFieldKey.CVV -> SensitiveFieldKey.CARD_CVV
        RevealedFieldKey.PAYMENT_PIN -> SensitiveFieldKey.CARD_PAYMENT_PIN
        RevealedFieldKey.SSH_PRIVATE_KEY -> SensitiveFieldKey.SSH_PRIVATE_KEY
        RevealedFieldKey.SSH_PASSPHRASE -> SensitiveFieldKey.SSH_PASSPHRASE
        RevealedFieldKey.SEED_PHRASE -> SensitiveFieldKey.SEED_PHRASE
        RevealedFieldKey.PASSKEY_DATA -> SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE
        RevealedFieldKey.ID_NUMBER -> SensitiveFieldKey.IDENTITY_NUMBER
        RevealedFieldKey.RECOVERY_CODES -> SensitiveFieldKey.RECOVERY_CODES
        else -> null
    }

    private fun ActivityType.clearsRevealedFields(): Boolean =
        this == ActivityType.COPY_PASSWORD || this == ActivityType.COPY_USERNAME

    private suspend fun persistEntryPatch(
        patch: DetailEntryPatch,
        completion: DetailEditCompletion,
    ) {
        if (!accessPolicy.hasFullAccess()) return
        val entryId = _uiState.value.entry?.id ?: return
        if (_uiState.value.savingEdit != null) return
        mutate(DetailMutation.SaveStarted(completion))
        when (val result = updateCoordinator.update(entryId, patch)) {
            is com.aozijx.passly.core.error.result.AppResult.Success -> {
                val latest = result.data
                val keepTitleEditing = completion != DetailEditCompletion.Title &&
                    _uiState.value.isEditingTitle
                refreshFromEntry(
                    latest,
                    isEditingTitle = keepTitleEditing,
                    editedTitle = if (keepTitleEditing) _uiState.value.editedTitle else latest.title,
                )
                if (completion is DetailEditCompletion.SensitiveField) {
                    patch.revealedValueOrNull()?.let { value ->
                        setRevealedField(completion.key, OwnedChars.fromString(value))
                    }
                }
                mutate(DetailMutation.SaveSucceeded(completion))
                emitEntryUpdated(latest)
            }

            is com.aozijx.passly.core.error.result.AppResult.Failure -> {
                mutate(DetailMutation.SaveFailed(completion, result.error.code))
            }
        }
    }

    private fun emitEntryUpdated(entry: Entry) {
        _effects.trySend(DetailEffect.EntryUpdated(entry))
    }

    private suspend fun loadRelatedEntries(entry: Entry) {
        val graph = EntryAccountGraph(entryLinkRepository.getAll())
        val accountId = if (entry.type == EntryType.ACCOUNT) {
            entry.id
        } else {
            graph.accountFor(entry.id)
        }
        if (accountId == null) {
            mutate(DetailMutation.RelatedEntriesChanged(emptyList()))
            return
        }
        val relatedIds = buildSet {
            add(accountId)
            addAll(graph.membersOf(accountId))
            remove(entry.id)
        }
        val related = relatedIds.mapNotNull { relatedId ->
            entryQueryRepository.getById(relatedId)
        }
        mutate(DetailMutation.RelatedEntriesChanged(related))
    }

    private fun refreshFromEntry(entry: Entry, isEditingTitle: Boolean, editedTitle: String) {
        val analysis = entryAnalyzer.analyze(entry)

        mutate(
            DetailMutation.EntryPresented(
                entry = entry,
                entryType = analysis.entryType,
                strategySummary = analysis.strategySummary,
                validationError = analysis.validationError,
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
        faviconJob?.cancel()
        faviconJob = null
        faviconImageProcessor.discardEditorResources(
            stagedPath = editor.privateImagePath(),
            pendingInputPath = editor.pendingInputPath,
            promotedCandidatePath = pendingPromotedFaviconPath,
        )
        pendingPromotedFaviconPath = null
        clearSensitiveState()
    }

    private fun launchFaviconJob(block: suspend () -> Unit) {
        if (faviconJob?.isActive == true) return
        faviconJob = viewModelScope.launch { block() }
    }

    private fun cancelFaviconWorkAndDiscard(editor: DetailFaviconEditorUiModel) {
        faviconJob?.cancel()
        faviconJob = null
        faviconImageProcessor.discardEditorResources(
            stagedPath = editor.privateImagePath(),
            pendingInputPath = editor.pendingInputPath,
            promotedCandidatePath = pendingPromotedFaviconPath,
        )
        pendingPromotedFaviconPath = null
    }

    private suspend fun stageFaviconInput(
        stage: suspend () -> Result<String>,
    ) {
        if (_uiState.value.faviconEditor.processing) return
        mutate(DetailMutation.FaviconProcessingStarted)
        stage().fold(
            onSuccess = { mutate(DetailMutation.FaviconInputStaged(it)) },
            onFailure = { mutate(DetailMutation.FaviconProcessingFailed(it.toFaviconUiError())) },
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
                        FaviconDraftSourceUiModel.PrivateImage(it),
                    ),
                )
            },
            onFailure = { mutate(DetailMutation.FaviconProcessingFailed(it.toFaviconUiError())) },
        )
    }

    private fun EntryIcon.toFaviconDraftSource(): FaviconDraftSourceUiModel {
        val privatePath = customReference
        val builtInName = name
        return when {
            !privatePath.isNullOrBlank() -> FaviconDraftSourceUiModel.PrivateImage(privatePath)
            !builtInName.isNullOrBlank() -> FaviconDraftSourceUiModel.BuiltIn(builtInName, color)
            else -> FaviconDraftSourceUiModel.InferredDefault
        }
    }

    private fun FaviconDraftSourceUiModel.toEntryIcon(): EntryIcon = when (this) {
        FaviconDraftSourceUiModel.InferredDefault -> EntryIcon()
        is FaviconDraftSourceUiModel.BuiltIn -> EntryIcon(name = key, color = colorToken)
        is FaviconDraftSourceUiModel.PrivateImage -> EntryIcon(customReference = localPath)
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

private fun DetailFaviconEditorUiModel.privateImagePath(): String? =
    (source as? FaviconDraftSourceUiModel.PrivateImage)?.localPath

private fun Throwable.toFaviconUiError(): FaviconProcessingErrorUiModel = when (this) {
    is FaviconUrlException -> when (reason) {
        FaviconUrlFailure.INVALID_URL,
        FaviconUrlFailure.HTTPS_REQUIRED,
        -> FaviconProcessingErrorUiModel.INVALID_URL

        FaviconUrlFailure.CREDENTIALS_NOT_ALLOWED,
        FaviconUrlFailure.HOST_NOT_ALLOWED,
        FaviconUrlFailure.PRIVATE_ADDRESS,
        -> FaviconProcessingErrorUiModel.URL_NOT_ALLOWED
    }

    is FaviconDownloadException -> when (reason) {
        FaviconDownloadFailure.NOT_IMAGE -> FaviconProcessingErrorUiModel.NOT_IMAGE
        FaviconDownloadFailure.TOO_LARGE -> FaviconProcessingErrorUiModel.IMAGE_TOO_LARGE
        else -> FaviconProcessingErrorUiModel.DOWNLOAD_FAILED
    }

    is FaviconImageException -> when (reason) {
        FaviconImageFailure.TOO_LARGE -> FaviconProcessingErrorUiModel.IMAGE_TOO_LARGE
        FaviconImageFailure.SAVE_FAILED -> FaviconProcessingErrorUiModel.SAVE_FAILED
        else -> FaviconProcessingErrorUiModel.INVALID_IMAGE
    }

    else -> FaviconProcessingErrorUiModel.INVALID_IMAGE
}
