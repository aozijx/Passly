package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconEditorTabUiModel

internal object DetailFaviconEditorReducer {
    fun reduce(
        state: DetailFaviconEditorUiModel,
        mutation: DetailFaviconEditorMutation,
    ): DetailFaviconEditorUiModel = when (mutation) {
        is DetailMutation.FaviconEditorOpened -> DetailFaviconEditorUiModel(
            visible = true,
            initialSource = mutation.source,
            source = mutation.source,
            selectedTab = when (mutation.source) {
                is FaviconDraftSourceUiModel.PrivateImage ->
                    FaviconEditorTabUiModel.CUSTOM_IMAGE
                else ->
                    FaviconEditorTabUiModel.ICON_LIBRARY
            },
        )

        is DetailMutation.FaviconSourceChanged -> state.copy(
            source = mutation.source,
            processing = false,
            pendingInputPath = null,
            promotedCandidatePath = null,
            confirmDiscard = false,
        )

        is DetailMutation.FaviconTabChanged -> state.copy(selectedTab = mutation.tab)
        is DetailMutation.FaviconSearchChanged -> state.copy(searchQuery = mutation.value)
        is DetailMutation.FaviconImageUrlChanged -> state.copy(
            imageUrl = mutation.value,
            processingError = null,
        )

        DetailMutation.FaviconProcessingStarted -> state.copy(
            processing = true,
            processingError = null,
        )

        is DetailMutation.FaviconInputStaged -> state.copy(
            processing = false,
            pendingInputPath = mutation.path,
            processingError = null,
        )

        is DetailMutation.FaviconSourcePromoted -> state.copy(
            source = FaviconDraftSourceUiModel.PrivateImage(mutation.path),
            processing = false,
            pendingInputPath = null,
            promotedCandidatePath = mutation.path,
            confirmDiscard = false,
        )

        is DetailMutation.FaviconProcessingFailed -> state.copy(
            processing = false,
            processingError = mutation.error,
        )

        DetailMutation.FaviconCropCancelled -> state.copy(pendingInputPath = null)
        DetailMutation.FaviconEditorDismissRequested -> {
            if (state.dirty) state.copy(confirmDiscard = true) else DetailFaviconEditorUiModel()
        }

        DetailMutation.FaviconEditorDiscardConfirmed -> DetailFaviconEditorUiModel()
        DetailMutation.FaviconEditorDiscardCancelled -> state.copy(
            confirmDiscard = false,
        )
    }
}
