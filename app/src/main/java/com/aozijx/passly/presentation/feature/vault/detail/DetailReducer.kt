package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState

internal sealed interface DetailMutation {
    data object StateCleared : DetailMutation
    data class AccessHistoryChanged(val enabled: Boolean) : DetailMutation
    data class EntryPresented(
        val entry: Entry,
        val entryType: EntryType,
        val strategySummary: String,
        val validationError: String?,
        val strategyReady: Boolean,
        val isEditingTitle: Boolean,
        val editedTitle: String,
    ) : DetailMutation

    data object TitleEditingStarted : DetailMutation
    data object TitleEditingCancelled : DetailMutation
    data class EditedTitleChanged(val value: String) : DetailMutation
    data class RevealedFieldChanged(val key: String, val value: SensitiveValue?) : DetailMutation
    data object RevealedFieldsCleared : DetailMutation
    data class SensitiveFieldPresenceChanged(val keys: Set<SensitiveFieldKey>) : DetailMutation
    data class HistoryChanged(val history: List<EntryActivity>) : DetailMutation
    data class RelatedEntriesChanged(val entries: List<Entry>) : DetailMutation
    data class FaviconDownloadingChanged(val downloading: Boolean) : DetailMutation
}

internal object DetailReducer {
    fun reduce(state: DetailUiState, mutation: DetailMutation): DetailUiState =
        when (mutation) {
            DetailMutation.StateCleared -> DetailUiState()
            is DetailMutation.AccessHistoryChanged ->
                state.copy(isAccessHistoryEnabled = mutation.enabled)

            is DetailMutation.EntryPresented -> state.copy(
                entry = mutation.entry,
                entryType = mutation.entryType,
                strategySummary = mutation.strategySummary,
                validationError = mutation.validationError,
                strategyReady = mutation.strategyReady,
                isEditingTitle = mutation.isEditingTitle,
                editedTitle = mutation.editedTitle,
            )

            DetailMutation.TitleEditingStarted -> state.copy(
                isEditingTitle = true,
                editedTitle = state.entry?.title.orEmpty(),
            )

            DetailMutation.TitleEditingCancelled -> state.copy(
                isEditingTitle = false,
                editedTitle = state.entry?.title.orEmpty(),
            )

            is DetailMutation.EditedTitleChanged -> state.copy(editedTitle = mutation.value)
            is DetailMutation.RevealedFieldChanged -> state.copy(
                revealedFields = if (mutation.value == null) {
                    state.revealedFields - mutation.key
                } else {
                    state.revealedFields + (mutation.key to mutation.value)
                },
            )

            DetailMutation.RevealedFieldsCleared -> state.copy(revealedFields = emptyMap())
            is DetailMutation.SensitiveFieldPresenceChanged ->
                state.copy(sensitiveFieldKeys = mutation.keys)

            is DetailMutation.HistoryChanged -> state.copy(history = mutation.history)
            is DetailMutation.RelatedEntriesChanged ->
                state.copy(relatedEntries = mutation.entries)

            is DetailMutation.FaviconDownloadingChanged ->
                state.copy(isFaviconDownloading = mutation.downloading)
        }
}
