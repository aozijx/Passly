package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconEditorTabUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconProcessingErrorUiModel

internal fun DetailFaviconEditorState.toFaviconEditorUiModel() = DetailFaviconEditorUiModel(
    visible = visible,
    initialSource = initialSource.toUiModel(),
    source = source.toUiModel(),
    selectedTab = FaviconEditorTabUiModel.valueOf(selectedTab.name),
    searchQuery = searchQuery,
    imageUrl = imageUrl,
    processing = processing,
    pendingInputPath = pendingInputPath,
    promotedCandidatePath = promotedCandidatePath,
    processingError = processingError?.let {
        FaviconProcessingErrorUiModel.valueOf(it.name)
    },
    confirmDiscard = confirmDiscard,
)

internal fun FaviconDraftSourceUiModel.toDetailFaviconSource(): DetailFaviconSource = when (this) {
    FaviconDraftSourceUiModel.InferredDefault -> DetailFaviconSource.InferredDefault
    is FaviconDraftSourceUiModel.BuiltIn -> DetailFaviconSource.BuiltIn(key, colorToken)
    is FaviconDraftSourceUiModel.PrivateImage -> DetailFaviconSource.PrivateImage(localPath)
}

internal fun FaviconEditorTabUiModel.toDetailFaviconTab(): DetailFaviconTab =
    DetailFaviconTab.valueOf(name)

private fun DetailFaviconSource.toUiModel(): FaviconDraftSourceUiModel = when (this) {
    DetailFaviconSource.InferredDefault -> FaviconDraftSourceUiModel.InferredDefault
    is DetailFaviconSource.BuiltIn -> FaviconDraftSourceUiModel.BuiltIn(key, colorToken)
    is DetailFaviconSource.PrivateImage -> FaviconDraftSourceUiModel.PrivateImage(localPath)
}
