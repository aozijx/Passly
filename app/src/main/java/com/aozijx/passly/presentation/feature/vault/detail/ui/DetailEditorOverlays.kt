package com.aozijx.passly.presentation.feature.vault.detail.ui

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.FaviconCropScreen
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.FaviconEditorSheet
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.TagEditorSheet
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailEditorOverlaysUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailEditorOverlayCallbacks

@Composable
fun DetailEditorOverlays(
    model: DetailEditorOverlaysUiModel,
    callbacks: DetailEditorOverlayCallbacks,
) {
    if (model.tagEditor.visible) {
        TagEditorSheet(
            state = model.tagEditor,
            isSaving = model.savingTags,
            onInputChanged = callbacks::onTagInputChanged,
            onSubmit = callbacks::onTagSubmitted,
            onRemove = callbacks::onTagRemoved,
            onSave = callbacks::onTagsSaveRequested,
            onDismiss = callbacks::onTagEditorDismissed,
            onConfirmDiscard = callbacks::onTagDiscardConfirmed,
            onKeepEditing = callbacks::onTagEditingContinued,
        )
    }

    if (model.faviconEditor.visible) {
        FaviconEditorSheet(
            state = model.faviconEditor,
            isSaving = model.savingIcon,
            onTabSelected = callbacks::onFaviconTabSelected,
            onSearchChanged = callbacks::onFaviconSearchChanged,
            onSourceSelected = callbacks::onFaviconSourceSelected,
            onUploadRequested = callbacks::onFaviconUploadRequested,
            onImageUrlChanged = callbacks::onFaviconImageUrlChanged,
            onDownloadRequested = callbacks::onFaviconDownloadRequested,
            onSave = callbacks::onFaviconSaveRequested,
            onDismiss = callbacks::onFaviconEditorDismissed,
            onConfirmDiscard = callbacks::onFaviconDiscardConfirmed,
            onKeepEditing = callbacks::onFaviconEditingContinued,
        )
    }

    model.faviconEditor.pendingInputPath?.let { path ->
        FaviconCropScreen(
            stagedPath = path,
            processing = model.faviconEditor.processing,
            onCrop = { zoom, x, y ->
                callbacks.onFaviconCropRequested(zoom, x, y)
            },
            onUseWithoutCrop = callbacks::onFaviconUseWithoutCropRequested,
            onCancel = callbacks::onFaviconCropCancelled,
        )
    }
}
