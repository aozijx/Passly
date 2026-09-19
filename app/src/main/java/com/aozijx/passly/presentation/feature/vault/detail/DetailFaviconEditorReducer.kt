package com.aozijx.passly.presentation.feature.vault.detail

internal object DetailFaviconEditorReducer {
    fun reduce(
        state: DetailFaviconEditorState,
        mutation: DetailFaviconEditorMutation,
    ): DetailFaviconEditorState = when (mutation) {
        is DetailMutation.FaviconEditorOpened -> DetailFaviconEditorState(
            visible = true,
            initialSource = mutation.source,
            source = mutation.source,
            selectedTab = when (mutation.source) {
                is DetailFaviconSource.PrivateImage ->
                    DetailFaviconTab.CUSTOM_IMAGE
                else ->
                    DetailFaviconTab.ICON_LIBRARY
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
            source = DetailFaviconSource.PrivateImage(mutation.path),
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
            if (state.dirty) state.copy(confirmDiscard = true) else DetailFaviconEditorState()
        }

        DetailMutation.FaviconEditorDiscardConfirmed -> DetailFaviconEditorState()
        DetailMutation.FaviconEditorDiscardCancelled -> state.copy(
            confirmDiscard = false,
        )
    }
}
