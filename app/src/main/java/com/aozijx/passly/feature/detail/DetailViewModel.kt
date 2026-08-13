package com.aozijx.passly.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.entry.model.favicon.FaviconResult
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.FaviconRepository
import com.aozijx.passly.domain.entry.policy.EntryTypePolicy
import com.aozijx.passly.domain.entry.policy.EntryAccountGraph
import com.aozijx.passly.feature.detail.contract.DetailEffect
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.DetailUiState
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey
import com.aozijx.passly.feature.detail.page.internal.DetailEntryAnalyzer
import com.aozijx.passly.feature.detail.internal.presentation.DetailMutation
import com.aozijx.passly.feature.detail.internal.presentation.DetailReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
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
    private val faviconRepository: FaviconRepository,
    private val entryTypePolicy: EntryTypePolicy,
    private val accessPolicy: DetailAccessPolicy,
    private val authorizationGate: AuthorizationGate,
) : ViewModel() {
    private val entryAnalyzer = DetailEntryAnalyzer(entryTypePolicy)

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
                mutate(DetailMutation.AccessHistoryChanged(enabled))
            }
        }
    }

    fun handleIntent(event: DetailIntent) {
        if (!accessPolicy.canHandle(event)) {
            mutate(DetailMutation.StateCleared)
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
                mutate(DetailMutation.TitleEditingStarted)
            }

            DetailIntent.CancelTitleEdit -> {
                mutate(DetailMutation.TitleEditingCancelled)
            }

            is DetailIntent.UpdateEditedTitle -> {
                mutate(DetailMutation.EditedTitleChanged(event.value))
            }

            DetailIntent.SaveTitle -> {
                val state = _uiState.value
                val current = state.entry ?: return
                val newTitle = state.editedTitle.trim()
                if (newTitle.isBlank() || newTitle == current.title) {
                    mutate(DetailMutation.TitleEditingCancelled)
                } else {
                    commitEntryUpdate(
                        current.copy(profile = current.profile.copy(title = newTitle)),
                        isEditingTitle = false
                    )
                }
            }

            DetailIntent.ToggleFavorite -> {
                val current = _uiState.value.entry ?: return
                commitEntryUpdate(
                    current.copy(profile = current.profile.copy(favorite = !current.favorite))
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
                    mutate(DetailMutation.RevealedFieldChanged(key, null))
                    return
                }
                viewModelScope.launch {
                    revealHighSensitivityFields(current.id, setOf(key))
                }
            }

            is DetailIntent.RevealHighSensitivityFields -> {
                val current = _uiState.value.entry ?: return
                val hiddenKeys = event.keys.filterTo(linkedSetOf()) {
                    _uiState.value.revealed(it) == null
                }
                if (hiddenKeys.isEmpty()) return
                viewModelScope.launch {
                    revealHighSensitivityFields(current.id, hiddenKeys)
                }
            }

            is DetailIntent.RecordAction -> {
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

            is DetailIntent.ToggleAccessHistoryRecording -> {
                mutate(DetailMutation.AccessHistoryChanged(event.enabled))
                userConfigExtras.value =
                    userConfigExtras.value + (ACCESS_HISTORY_TOGGLE_KEY to event.enabled.toString())
            }

            DetailIntent.ClearSensitiveState -> {
                mutate(DetailMutation.StateCleared)
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
            autoDownloadFavicon(latest)
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

    private fun setRevealedField(key: String, value: String?) {
        mutate(DetailMutation.RevealedFieldChanged(key, value))
    }

    private suspend fun revealHighSensitivityFields(entryValue: EntryId, uiKeys: Set<String>) {
        if (!accessPolicy.hasFullAccess()) return
        val requested = uiKeys.mapNotNull { uiKey ->
            uiKey.toSensitiveFieldKey()?.let { fieldKey -> uiKey to fieldKey }
        }.toMap()
        if (requested.isEmpty()) return
        val entryId = entryValue
        authorizationGate.authorize(
            AuthorizationScope.SensitiveFields(
                entryId = entryId,
                fieldKeys = requested.values.toSet(),
                action = SensitiveAccessAction.REVEAL,
            ),
        ) authorize@{ permit ->
            val revealedFields = sensitiveFieldRepository.revealMany(
                entryId = entryId,
                keys = requested.values.toSet(),
                permit = permit,
            )
            revealedFields.forEach { revealed ->
                val uiKey = requested.entries.firstOrNull { it.value == revealed.key }?.key
                    ?: return@forEach
                val chars = revealed.value.toCharArray()
                try {
                    val value = String(chars).takeIf { it.isNotBlank() } ?: return@forEach
                    setRevealedField(uiKey, value)
                } finally {
                    chars.fill('\u0000')
                    revealed.value.wipe()
                }
            }
            if (revealedFields.isNotEmpty()) {
                activityRecorder.recordUsage(entryValue.value, ActivityType.VIEW)
            }
        }
    }

    private fun String.toSensitiveFieldKey(): SensitiveFieldKey? = when (this) {
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

    private fun commitEntryUpdate(entry: Entry, isEditingTitle: Boolean = _uiState.value.isEditingTitle) {
        val editedTitle = if (isEditingTitle) _uiState.value.editedTitle else entry.title
        refreshFromEntry(entry, isEditingTitle = isEditingTitle, editedTitle = editedTitle)
        emitEntryUpdated(entry)
    }

    private fun emitEntryUpdated(entry: Entry) {
        _effects.trySend(DetailEffect.EntryUpdated(entry))
    }

    private fun autoDownloadFavicon(entry: Entry) {
        val domain = entry.associations.primaryUrl ?: entry.associations.domains.firstOrNull()
        if (domain.isNullOrBlank() || !entry.icon.customReference.isNullOrBlank()) return
        viewModelScope.launch {
            downloadAndApplyFavicon(entry, domain, updateDomain = false)
        }
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

    private suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.download(input)
    }

    private suspend fun downloadAndApplyFavicon(
        entry: Entry,
        domain: String,
        updateDomain: Boolean
    ) {
        if (domain.isBlank()) return
        if (!accessPolicy.hasFullAccess()) return
        mutate(DetailMutation.FaviconDownloadingChanged(true))
        try {
            val outcome = downloadFavicon(domain)
            if (!accessPolicy.hasFullAccess()) return
            if (outcome.result != FaviconResult.SUCCESS || outcome.filePath == null) return
            val associations = if (updateDomain) {
                entry.profile.associations.copy(primaryUrl = domain.trim())
            } else {
                entry.profile.associations
            }
            val updatedProfile = entry.profile.copy(
                associations = associations,
                icon = entry.profile.icon.copy(
                    name = null,
                    customReference = outcome.filePath,
                ),
            )
            val updateResult = entryCommandRepository.updateEntry(
                entry.id,
                entry.version,
                EntryUpdate(profile = updatedProfile)
            )
            if (updateResult.isSuccess) {
                val latest = entryQueryRepository.getById(entry.id)
                if (latest != null) {
                    refreshFromEntry(
                        latest,
                        _uiState.value.isEditingTitle,
                        _uiState.value.editedTitle
                    )
                }
            }
        } finally {
            mutate(DetailMutation.FaviconDownloadingChanged(false))
        }
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
        super.onCleared()
        mutate(DetailMutation.StateCleared)
    }
}
