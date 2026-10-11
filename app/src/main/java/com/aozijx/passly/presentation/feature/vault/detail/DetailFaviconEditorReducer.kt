package com.aozijx.passly.presentation.feature.vault.detail

internal object DetailFaviconEditorReducer {
    fun reduce(
        state: DetailFaviconEditorState,
        mutation: DetailFaviconEditorMutation,
    ): DetailFaviconEditorState = when (mutation) {
        is DetailMutation.FaviconEditorOpened -> DetailFaviconEditorState.Open(
            initialSource = mutation.source,
            selectedTab = when (mutation.source) {
                is DetailFaviconSource.PrivateImage ->
                    DetailFaviconTab.CUSTOM_IMAGE
                else ->
                    DetailFaviconTab.ICON_LIBRARY
            },
        )

        else -> when (state) {
            DetailFaviconEditorState.Closed -> state
            is DetailFaviconEditorState.Open -> reduceOpen(state, mutation)
        }
    }

    private fun reduceOpen(
        state: DetailFaviconEditorState.Open,
        mutation: DetailFaviconEditorMutation,
    ): DetailFaviconEditorState = when (mutation) {
        is DetailMutation.FaviconEditorOpened -> state
        is DetailMutation.FaviconSourceChanged -> state.copy(
            source = mutation.source,
            work = DetailFaviconWorkState.Idle,
            pendingInputPath = null,
            promotedCandidatePath = null,
            overlay = DetailFaviconOverlay.None,
        )

        is DetailMutation.FaviconTabChanged -> state.copy(selectedTab = mutation.tab)
        is DetailMutation.FaviconSearchChanged -> state.copy(searchQuery = mutation.value)
        is DetailMutation.FaviconImageUrlChanged -> state.copy(
            imageUrl = mutation.value,
            work = DetailFaviconWorkState.Idle,
        )

        DetailMutation.FaviconProcessingStarted -> state.copy(
            work = DetailFaviconWorkState.Processing,
        )

        is DetailMutation.FaviconInputStaged -> state.copy(
            work = DetailFaviconWorkState.Idle,
            pendingInputPath = mutation.path,
        )

        is DetailMutation.FaviconSourcePromoted -> state.copy(
            source = DetailFaviconSource.PrivateImage(mutation.path),
            work = DetailFaviconWorkState.Idle,
            pendingInputPath = null,
            promotedCandidatePath = mutation.path,
            overlay = DetailFaviconOverlay.None,
        )

        is DetailMutation.FaviconProcessingFailed -> state.copy(
            work = DetailFaviconWorkState.Failed(mutation.error),
        )

        DetailMutation.FaviconCropCancelled -> state.copy(pendingInputPath = null)
        DetailMutation.FaviconEditorDismissRequested -> {
            if (state.dirty) {
                state.copy(overlay = DetailFaviconOverlay.ConfirmDiscard)
            } else {
                DetailFaviconEditorState.Closed
            }
        }

        DetailMutation.FaviconEditorDiscardConfirmed -> DetailFaviconEditorState.Closed
        DetailMutation.FaviconEditorDiscardCancelled -> state.copy(
            overlay = DetailFaviconOverlay.None,
        )
    }
}
