package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconEditorTabUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.FaviconProcessingErrorUiModel

internal fun DetailFaviconEditorState.toFaviconEditorUiModel() = DetailFaviconEditorUiModel(
    visible = visible,
    initialSource = initialSource.toUiModel(),
    source = source.toUiModel(),
    selectedTab = selectedTab.toUiModel(),
    searchQuery = searchQuery,
    imageUrl = imageUrl,
    processing = processing,
    pendingInputPath = pendingInputPath,
    promotedCandidatePath = promotedCandidatePath,
    processingError = processingError?.toUiModel(),
    confirmDiscard = confirmDiscard,
)

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
