package com.aozijx.passly.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.entry.model.EntryChanges
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryHighSensitivitySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.entry.model.favicon.FaviconResult
import com.aozijx.passly.domain.entry.repository.ActivityQueryRepository
import com.aozijx.passly.domain.entry.repository.ActivityRecorder
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import com.aozijx.passly.domain.entry.repository.EntryLinkRepository
import com.aozijx.passly.domain.entry.repository.EntryHighSensitivityRepository
import com.aozijx.passly.domain.entry.repository.EntryQueryRepository
import com.aozijx.passly.domain.entry.repository.FaviconRepository
import com.aozijx.passly.domain.entry.service.EntryTypePolicy
import com.aozijx.passly.domain.entry.service.EntryAccountGraph
import com.aozijx.passly.domain.entry.service.EntryValidatorProvider
import com.aozijx.passly.feature.detail.contract.DetailEffect
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.DetailUiState
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey
import com.aozijx.passly.feature.detail.page.internal.DetailEntryAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val entryHighSensitivityRepository: EntryHighSensitivityRepository,
    private val activityQueryRepository: ActivityQueryRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val entryLinkRepository: EntryLinkRepository,
    private val activityRecorder: ActivityRecorder,
    private val faviconRepository: FaviconRepository,
    private val entryTypePolicy: EntryTypePolicy,
    private val entryValidatorProvider: EntryValidatorProvider,
    private val accessPolicy: DetailAccessPolicy
) : ViewModel() {
    private val entryAnalyzer = DetailEntryAnalyzer(entryTypePolicy, entryValidatorProvider)

    companion object {
        private const val ACCESS_HISTORY_TOGGLE_KEY = "detail.access_history_enabled"
    }

    private val userConfigExtras = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private val _effects = Channel<DetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            userConfigExtras.collect { extras ->
                val enabled = extras[ACCESS_HISTORY_TOGGLE_KEY]
                    ?.toBooleanStrictOrNull()
                    ?: false
                _uiState.update { it.copy(isAccessHistoryEnabled = enabled) }
            }
        }
    }

    fun handleIntent(event: DetailIntent) {
        if (!accessPolicy.canHandle(event)) {
            _uiState.update { DetailUiState() }
            return
        }
        when (event) {
            is DetailIntent.Initialize -> {
                initialize(event.initialEntry)
            }

            is DetailIntent.SyncEntry -> {
                refreshKeepingTitleEdit(event.entry)
            }

            is DetailIntent.CommitEntryUpdate -> {
                refreshKeepingTitleEdit(event.entry)
                emitEntryUpdated(event.entry)
            }

            DetailIntent.StartTitleEdit -> {
                _uiState.update {
                    val currentTitle = it.entry?.title.orEmpty()
                    it.copy(isEditingTitle = true, editedTitle = currentTitle)
                }
            }

            DetailIntent.CancelTitleEdit -> {
                _uiState.update {
                    it.copy(
                        isEditingTitle = false,
                        editedTitle = it.entry?.title.orEmpty()
                    )
                }
            }

            is DetailIntent.UpdateEditedTitle -> {
                _uiState.update { it.copy(editedTitle = event.value) }
            }

            DetailIntent.SaveTitle -> {
                val state = _uiState.value
                val current = state.entry ?: return
                val newTitle = state.editedTitle.trim()
                if (newTitle.isBlank() || newTitle == current.title) {
                    _uiState.update {
                        it.copy(
                            isEditingTitle = false,
                            editedTitle = current.title
                        )
                    }
                } else {
                    commitEntryUpdate(
                        current.copy(summary = current.summary.copy(title = newTitle)),
                        isEditingTitle = false
                    )
                }
            }

            DetailIntent.ToggleFavorite -> {
                val current = _uiState.value.entry ?: return
                commitEntryUpdate(
                    current.copy(summary = current.summary.copy(favorite = !current.favorite))
                )
            }

            is DetailIntent.RevealField -> {
                setRevealedField(event.key, event.value)
            }

            is DetailIntent.DownloadFavicon -> {
                val current = _uiState.value.entry ?: return
                viewModelScope.launch {
                    downloadAndApplyFavicon(current, event.domain, updateDomain = true)
                }
            }

            is DetailIntent.RevealHighSensitivityField -> {
                val current = _uiState.value.entry ?: return
                val key = event.key
                if (_uiState.value.revealed(key) != null) {
                    _uiState.update { state ->
                        state.copy(revealedFields = state.revealedFields - key)
                    }
                    return
                }
                viewModelScope.launch {
                    if (!accessPolicy.hasFullAccess()) return@launch
                    val high = entryHighSensitivityRepository
                        .getHighSensitivitySecretForReveal(current.id)
                    val value = high?.valueFor(key)?.takeIf { it.isNotBlank() } ?: return@launch
                    setRevealedField(key, value)
                    activityRecorder.recordUsage(current.id, ActivityType.VIEW)
                }
            }

            is DetailIntent.RecordAction -> {
                val current = _uiState.value.entry ?: return
                if (event.type == ActivityType.VIEW && !_uiState.value.isAccessHistoryEnabled) return
                if (event.type.clearsRevealedFields()) {
                    _uiState.update { it.copy(revealedFields = emptyMap()) }
                }

                viewModelScope.launch {
                    if (!accessPolicy.hasFullAccess()) return@launch
                    activityRecorder.recordUsage(current.id, event.type)
                }
            }

            is DetailIntent.ToggleAccessHistoryRecording -> {
                _uiState.update { it.copy(isAccessHistoryEnabled = event.enabled) }
                userConfigExtras.value =
                    userConfigExtras.value + (ACCESS_HISTORY_TOGGLE_KEY to event.enabled.toString())
            }

            DetailIntent.ClearSensitiveState -> {
                _uiState.update { DetailUiState() }
            }
        }
    }

    private fun initialize(initialEntry: EntryAggregate) {
        refreshFromEntry(initialEntry, isEditingTitle = false, editedTitle = initialEntry.title)
        viewModelScope.launch {
            if (!accessPolicy.hasFullAccess()) return@launch
            val latest = entryQueryRepository.getByIdWithoutHighSensitivity(initialEntry.id)
                ?: initialEntry
            refreshFromEntry(latest, isEditingTitle = false, editedTitle = latest.title)
            loadRelatedEntries(latest)
            autoDownloadFavicon(latest)
        }
        viewModelScope.launch {
            if (!accessPolicy.hasFullAccess()) return@launch
            activityQueryRepository.observeByEntryId(initialEntry.id)
                .collect { history -> _uiState.update { it.copy(history = history) } }
        }
    }

    private fun refreshKeepingTitleEdit(entry: EntryAggregate) {
        val isEditing = _uiState.value.isEditingTitle
        refreshFromEntry(
            entry,
            isEditingTitle = isEditing,
            editedTitle = if (isEditing) _uiState.value.editedTitle else entry.title
        )
    }

    private fun setRevealedField(key: String, value: String?) {
        _uiState.update { state ->
            state.copy(
                revealedFields = if (value == null) {
                    state.revealedFields - key
                } else {
                    state.revealedFields + (key to value)
                }
            )
        }
    }

    private fun EntryHighSensitivitySecret.valueFor(key: String): String? = when (key) {
        RevealedFieldKey.CARD_NUMBER -> card?.cardNumber
        RevealedFieldKey.CVV -> card?.cardCvv
        RevealedFieldKey.PAYMENT_PIN -> card?.paymentPin
        RevealedFieldKey.SSH_PRIVATE_KEY -> ssh?.privateKey
        RevealedFieldKey.SEED_PHRASE -> identity?.seedPhrase
        RevealedFieldKey.PASSKEY_DATA -> passkey?.privateKeyReference
        RevealedFieldKey.ID_NUMBER -> identity?.idNumber
        else -> null
    }

    private fun ActivityType.clearsRevealedFields(): Boolean =
        this == ActivityType.COPY_PASSWORD || this == ActivityType.COPY_USERNAME

    private fun commitEntryUpdate(entry: EntryAggregate, isEditingTitle: Boolean = _uiState.value.isEditingTitle) {
        val editedTitle = if (isEditingTitle) _uiState.value.editedTitle else entry.title
        refreshFromEntry(entry, isEditingTitle = isEditingTitle, editedTitle = editedTitle)
        emitEntryUpdated(entry)
    }

    private fun emitEntryUpdated(entry: EntryAggregate) {
        _effects.trySend(DetailEffect.EntryUpdated(entry))
    }

    private fun autoDownloadFavicon(entry: EntryAggregate) {
        if (entry.associatedDomain.isNullOrBlank() || !entry.iconCustomPath.isNullOrBlank()) return
        viewModelScope.launch {
            downloadAndApplyFavicon(entry, entry.associatedDomain!!, updateDomain = false)
        }
    }

    private suspend fun loadRelatedEntries(entry: EntryAggregate) {
        val graph = EntryAccountGraph(entryLinkRepository.getAll())
        val accountId = if (entry.entryType == EntryType.ACCOUNT) {
            EntryId(entry.id)
        } else {
            graph.accountFor(EntryId(entry.id))
        }
        if (accountId == null) {
            _uiState.update { it.copy(relatedEntries = emptyList()) }
            return
        }
        val relatedIds = buildSet {
            add(accountId)
            addAll(graph.membersOf(accountId))
            remove(EntryId(entry.id))
        }
        val related = relatedIds.mapNotNull { relatedId ->
            entryQueryRepository.getByIdWithoutHighSensitivity(relatedId.value)
        }
        _uiState.update { it.copy(relatedEntries = related) }
    }

    private suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.download(input)
    }

    private suspend fun downloadAndApplyFavicon(
        entry: EntryAggregate,
        domain: String,
        updateDomain: Boolean
    ) {
        if (domain.isBlank()) return
        if (!accessPolicy.hasFullAccess()) return
        _uiState.update { it.copy(isFaviconDownloading = true) }
        try {
            val outcome = downloadFavicon(domain)
            if (!accessPolicy.hasFullAccess()) return
            if (outcome.result != FaviconResult.SUCCESS || outcome.filePath == null) return
            val website = if (updateDomain) {
                (entry.summary.website ?: com.aozijx.passly.domain.entry.model.WebsiteInfo())
                    .copy(primaryUrl = domain.trim())
            } else {
                entry.summary.website
            }
            val updatedSummary = entry.summary.copy(
                website = website,
                icon = null,
                iconCustomPath = outcome.filePath
            )
            val updateResult = entryCommandRepository.updateEntry(
                entry.id,
                entry.entryVersion,
                EntryChanges(summary = updatedSummary)
            )
            if (updateResult.isSuccess) {
                val latest = entryQueryRepository.getByIdWithoutHighSensitivity(entry.id)
                if (latest != null) {
                    refreshFromEntry(
                        latest,
                        _uiState.value.isEditingTitle,
                        _uiState.value.editedTitle
                    )
                }
            }
        } finally {
            _uiState.update { it.copy(isFaviconDownloading = false) }
        }
    }

    private fun refreshFromEntry(entry: EntryAggregate, isEditingTitle: Boolean, editedTitle: String) {
        val analysis = entryAnalyzer.analyze(entry)

        _uiState.update {
            it.copy(
                entry = entry,
                entryType = analysis.entryType,
                strategySummary = analysis.strategySummary,
                validationError = analysis.validationError,
                isEditingTitle = isEditingTitle,
                editedTitle = editedTitle,
                strategyReady = analysis.strategyReady
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.update { DetailUiState() }
    }
}
