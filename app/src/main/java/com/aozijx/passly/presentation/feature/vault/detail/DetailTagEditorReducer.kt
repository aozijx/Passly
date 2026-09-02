package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.ui.vault.detail.model.DetailTagEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.TagEditorValidationErrorUiModel

internal object DetailTagEditorReducer {
    fun reduce(
        state: DetailTagEditorUiModel,
        mutation: DetailTagEditorMutation,
    ): DetailTagEditorUiModel = when (mutation) {
        is DetailMutation.TagEditorOpened -> {
            val tags = when (val normalized = DetailTagNormalizer.normalize(mutation.currentTags)) {
                is TagNormalizationResult.Valid -> normalized.tags
                else -> mutation.currentTags
            }
            DetailTagEditorUiModel(
                visible = true,
                initialTags = tags,
                draftTags = tags,
                availableTags = mutation.availableTags,
            )
        }

        is DetailMutation.TagInputChanged -> state.copy(
            input = mutation.value,
            suggestions = DetailTagNormalizer.suggestions(
                existingTags = state.availableTags,
                prefix = mutation.value.substringAfterLast(',').substringAfterLast('\n'),
                selectedTags = state.draftTags,
            ),
            validationError = null,
        )

        is DetailMutation.TagSubmitted -> when (
            val normalized = DetailTagNormalizer.normalize(state.draftTags + mutation.value)
        ) {
            is TagNormalizationResult.Valid -> state.copy(
                draftTags = normalized.tags,
                input = "",
                suggestions = emptyList(),
                validationError = null,
            )

            is TagNormalizationResult.TooMany -> state.copy(
                validationError = TagEditorValidationErrorUiModel.TOO_MANY_TAGS,
            )

            is TagNormalizationResult.TooLong -> state.copy(
                validationError = TagEditorValidationErrorUiModel.TAG_TOO_LONG,
            )
        }

        is DetailMutation.TagRemoved -> state.copy(
            draftTags = state.draftTags
                .filterNot { it.equals(mutation.value, ignoreCase = true) }
                .toCollection(linkedSetOf()),
            validationError = null,
        )

        DetailMutation.TagEditorDismissRequested -> {
            if (state.dirty) state.copy(confirmDiscard = true) else DetailTagEditorUiModel()
        }

        DetailMutation.TagEditorDiscardConfirmed -> DetailTagEditorUiModel()
        DetailMutation.TagEditorDiscardCancelled -> state.copy(confirmDiscard = false)
    }
}
