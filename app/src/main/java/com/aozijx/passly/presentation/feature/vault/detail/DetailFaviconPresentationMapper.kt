package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconEditorTabUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconOverlayUiState
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconProcessingErrorUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconWorkUiState

internal fun DetailFaviconEditorState.toFaviconEditorUiModel(): DetailFaviconEditorUiModel =
    when (this) {
        DetailFaviconEditorState.Closed -> DetailFaviconEditorUiModel.Hidden
        is DetailFaviconEditorState.Open -> DetailFaviconEditorUiModel.Visible(
            initialSource = initialSource.toUiModel(),
            source = source.toUiModel(),
            selectedTab = selectedTab.toUiModel(),
            searchQuery = searchQuery,
            imageUrl = imageUrl,
            work = work.toUiModel(),
            pendingInputPath = pendingInputPath,
            promotedCandidatePath = promotedCandidatePath,
            overlay = overlay.toUiModel(),
        )
    }

internal fun FaviconDraftSourceUiModel.toDetailFaviconSource(): DetailFaviconSource = when (this) {
    FaviconDraftSourceUiModel.InferredDefault -> DetailFaviconSource.InferredDefault
    is FaviconDraftSourceUiModel.BuiltIn -> DetailFaviconSource.BuiltIn(key, colorToken)
    is FaviconDraftSourceUiModel.PrivateImage -> DetailFaviconSource.PrivateImage(localPath)
}

internal fun FaviconEditorTabUiModel.toDetailFaviconTab(): DetailFaviconTab = when (this) {
    FaviconEditorTabUiModel.ICON_LIBRARY -> DetailFaviconTab.ICON_LIBRARY
    FaviconEditorTabUiModel.CUSTOM_IMAGE -> DetailFaviconTab.CUSTOM_IMAGE
}

private fun DetailFaviconSource.toUiModel(): FaviconDraftSourceUiModel = when (this) {
    DetailFaviconSource.InferredDefault -> FaviconDraftSourceUiModel.InferredDefault
    is DetailFaviconSource.BuiltIn -> FaviconDraftSourceUiModel.BuiltIn(key, colorToken)
    is DetailFaviconSource.PrivateImage -> FaviconDraftSourceUiModel.PrivateImage(localPath)
}

private fun DetailFaviconTab.toUiModel(): FaviconEditorTabUiModel = when (this) {
    DetailFaviconTab.ICON_LIBRARY -> FaviconEditorTabUiModel.ICON_LIBRARY
    DetailFaviconTab.CUSTOM_IMAGE -> FaviconEditorTabUiModel.CUSTOM_IMAGE
}

private fun DetailFaviconProcessingError.toUiModel(): FaviconProcessingErrorUiModel = when (this) {
    DetailFaviconProcessingError.INVALID_URL -> FaviconProcessingErrorUiModel.INVALID_URL
    DetailFaviconProcessingError.URL_NOT_ALLOWED -> FaviconProcessingErrorUiModel.URL_NOT_ALLOWED
    DetailFaviconProcessingError.DOWNLOAD_FAILED -> FaviconProcessingErrorUiModel.DOWNLOAD_FAILED
    DetailFaviconProcessingError.NOT_IMAGE -> FaviconProcessingErrorUiModel.NOT_IMAGE
    DetailFaviconProcessingError.IMAGE_TOO_LARGE -> FaviconProcessingErrorUiModel.IMAGE_TOO_LARGE
    DetailFaviconProcessingError.INVALID_IMAGE -> FaviconProcessingErrorUiModel.INVALID_IMAGE
    DetailFaviconProcessingError.SAVE_FAILED -> FaviconProcessingErrorUiModel.SAVE_FAILED
}

private fun DetailFaviconWorkState.toUiModel(): FaviconWorkUiState = when (this) {
    DetailFaviconWorkState.Idle -> FaviconWorkUiState.Idle
    DetailFaviconWorkState.Processing -> FaviconWorkUiState.Processing
    is DetailFaviconWorkState.Failed -> FaviconWorkUiState.Failed(error.toUiModel())
}

private fun DetailFaviconOverlay.toUiModel(): FaviconOverlayUiState = when (this) {
    DetailFaviconOverlay.None -> FaviconOverlayUiState.None
    DetailFaviconOverlay.ConfirmDiscard -> FaviconOverlayUiState.ConfirmDiscard
}
