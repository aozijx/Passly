package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailTagEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.TagEditorValidationErrorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconEditorTabUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconProcessingErrorUiModel
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
    data class SaveStarted(val completion: DetailEditCompletion) : DetailMutation
    data class SaveSucceeded(val completion: DetailEditCompletion) : DetailMutation
    data class SaveFailed(
        val completion: DetailEditCompletion,
        val errorCode: String,
    ) : DetailMutation
    data class TagEditorOpened(
        val currentTags: Set<String>,
        val availableTags: Set<String>,
    ) : DetailMutation
    data class TagInputChanged(val value: String) : DetailMutation
    data class TagSubmitted(val value: String) : DetailMutation
    data class TagRemoved(val value: String) : DetailMutation
    data object TagEditorDismissRequested : DetailMutation
    data object TagEditorDiscardConfirmed : DetailMutation
    data object TagEditorDiscardCancelled : DetailMutation
    data class FaviconEditorOpened(val source: FaviconDraftSourceUiModel) : DetailMutation
    data class FaviconSourceChanged(val source: FaviconDraftSourceUiModel) : DetailMutation
    data class FaviconTabChanged(val tab: FaviconEditorTabUiModel) : DetailMutation
    data class FaviconSearchChanged(val value: String) : DetailMutation
    data class FaviconImageUrlChanged(val value: String) : DetailMutation
    data object FaviconProcessingStarted : DetailMutation
    data class FaviconInputStaged(val path: String) : DetailMutation
    data class FaviconProcessingFailed(val error: FaviconProcessingErrorUiModel) : DetailMutation
    data object FaviconCropCancelled : DetailMutation
    data object FaviconEditorDismissRequested : DetailMutation
    data object FaviconEditorDiscardConfirmed : DetailMutation
    data object FaviconEditorDiscardCancelled : DetailMutation
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

            is DetailMutation.SaveStarted -> state.copy(
                savingEdit = mutation.completion,
                saveErrorCode = null,
            )

            is DetailMutation.SaveSucceeded -> {
                if (state.savingEdit != mutation.completion) {
                    state
                } else {
                    state.copy(
                        isEditingTitle = if (mutation.completion == DetailEditCompletion.Title) {
                            false
                        } else {
                            state.isEditingTitle
                        },
                        savingEdit = null,
                        completedEdit = mutation.completion,
                        saveCompletionId = state.saveCompletionId + 1,
                        saveErrorCode = null,
                        tagEditor = if (mutation.completion == DetailEditCompletion.Tags) {
                            DetailTagEditorUiModel()
                        } else {
                            state.tagEditor
                        },
                        faviconEditor = if (mutation.completion == DetailEditCompletion.Icon) {
                            DetailFaviconEditorUiModel()
                        } else {
                            state.faviconEditor
                        },
                    )
                }
            }

            is DetailMutation.SaveFailed -> {
                if (state.savingEdit != mutation.completion) {
                    state
                } else {
                    state.copy(
                        savingEdit = null,
                        saveErrorCode = mutation.errorCode,
                    )
                }
            }

            is DetailMutation.TagEditorOpened -> {
                val tags = when (val normalized = DetailTagNormalizer.normalize(mutation.currentTags)) {
                    is TagNormalizationResult.Valid -> normalized.tags
                    else -> mutation.currentTags
                }
                state.copy(
                    tagEditor = DetailTagEditorUiModel(
                        visible = true,
                        initialTags = tags,
                        draftTags = tags,
                        availableTags = mutation.availableTags,
                    ),
                )
            }

            is DetailMutation.TagInputChanged -> state.copy(
                tagEditor = state.tagEditor.copy(
                    input = mutation.value,
                    suggestions = DetailTagNormalizer.suggestions(
                        existingTags = state.tagEditor.availableTags,
                        prefix = mutation.value.substringAfterLast(',').substringAfterLast('\n'),
                        selectedTags = state.tagEditor.draftTags,
                    ),
                    validationError = null,
                ),
            )

            is DetailMutation.TagSubmitted -> {
                when (
                    val normalized = DetailTagNormalizer.normalize(
                        state.tagEditor.draftTags + mutation.value,
                    )
                ) {
                    is TagNormalizationResult.Valid -> state.copy(
                        tagEditor = state.tagEditor.copy(
                            draftTags = normalized.tags,
                            input = "",
                            suggestions = emptyList(),
                            validationError = null,
                        ),
                    )

                    is TagNormalizationResult.TooMany -> state.copy(
                        tagEditor = state.tagEditor.copy(
                            validationError = TagEditorValidationErrorUiModel.TOO_MANY_TAGS,
                        ),
                    )

                    is TagNormalizationResult.TooLong -> state.copy(
                        tagEditor = state.tagEditor.copy(
                            validationError = TagEditorValidationErrorUiModel.TAG_TOO_LONG,
                        ),
                    )
                }
            }

            is DetailMutation.TagRemoved -> state.copy(
                tagEditor = state.tagEditor.copy(
                    draftTags = state.tagEditor.draftTags
                        .filterNot { it.equals(mutation.value, ignoreCase = true) }
                        .toCollection(linkedSetOf()),
                    validationError = null,
                ),
            )

            DetailMutation.TagEditorDismissRequested -> {
                if (state.tagEditor.dirty) {
                    state.copy(tagEditor = state.tagEditor.copy(confirmDiscard = true))
                } else {
                    state.copy(tagEditor = DetailTagEditorUiModel())
                }
            }

            DetailMutation.TagEditorDiscardConfirmed ->
                state.copy(tagEditor = DetailTagEditorUiModel())

            DetailMutation.TagEditorDiscardCancelled -> state.copy(
                tagEditor = state.tagEditor.copy(confirmDiscard = false),
            )

            is DetailMutation.FaviconEditorOpened -> state.copy(
                faviconEditor = DetailFaviconEditorUiModel(
                    visible = true,
                    initialSource = mutation.source,
                    source = mutation.source,
                ),
            )

            is DetailMutation.FaviconSourceChanged -> state.copy(
                faviconEditor = state.faviconEditor.copy(
                    source = mutation.source,
                    pendingInputPath = null,
                    confirmDiscard = false,
                ),
            )

            is DetailMutation.FaviconTabChanged -> state.copy(
                faviconEditor = state.faviconEditor.copy(selectedTab = mutation.tab),
            )

            is DetailMutation.FaviconSearchChanged -> state.copy(
                faviconEditor = state.faviconEditor.copy(searchQuery = mutation.value),
            )

            is DetailMutation.FaviconImageUrlChanged -> state.copy(
                faviconEditor = state.faviconEditor.copy(
                    imageUrl = mutation.value,
                    processingError = null,
                ),
            )

            DetailMutation.FaviconProcessingStarted -> state.copy(
                faviconEditor = state.faviconEditor.copy(processing = true, processingError = null),
            )

            is DetailMutation.FaviconInputStaged -> state.copy(
                faviconEditor = state.faviconEditor.copy(
                    processing = false,
                    pendingInputPath = mutation.path,
                    processingError = null,
                ),
            )

            is DetailMutation.FaviconProcessingFailed -> state.copy(
                faviconEditor = state.faviconEditor.copy(
                    processing = false,
                    processingError = mutation.error,
                ),
            )

            DetailMutation.FaviconCropCancelled -> state.copy(
                faviconEditor = state.faviconEditor.copy(pendingInputPath = null),
            )

            DetailMutation.FaviconEditorDismissRequested -> {
                if (state.faviconEditor.dirty) {
                    state.copy(
                        faviconEditor = state.faviconEditor.copy(confirmDiscard = true),
                    )
                } else {
                    state.copy(faviconEditor = DetailFaviconEditorUiModel())
                }
            }

            DetailMutation.FaviconEditorDiscardConfirmed ->
                state.copy(faviconEditor = DetailFaviconEditorUiModel())

            DetailMutation.FaviconEditorDiscardCancelled -> state.copy(
                faviconEditor = state.faviconEditor.copy(confirmDiscard = false),
            )
        }
}
