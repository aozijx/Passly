package com.aozijx.passly.presentation.feature.vault.detail.ui

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.FaviconCropScreen
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.FaviconEditorSheet
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.TagEditorSheet
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailEditorOverlaysUiModel

@Composable
fun DetailEditorOverlays(
    model: DetailEditorOverlaysUiModel,
    onAction: (DetailUiAction) -> Unit,
    onUploadFavicon: () -> Unit,
) {
    if (model.tagEditor.visible) {
        TagEditorSheet(
            state = model.tagEditor,
            isSaving = model.savingTags,
            onInputChanged = { onAction(DetailUiAction.UpdateTagInput(it)) },
            onSubmit = { onAction(DetailUiAction.SubmitTag(it)) },
            onRemove = { onAction(DetailUiAction.RemoveTag(it)) },
            onSave = { onAction(DetailUiAction.SaveTags) },
            onDismiss = { onAction(DetailUiAction.DismissTagEditor) },
            onConfirmDiscard = { onAction(DetailUiAction.ConfirmDiscardTags) },
            onKeepEditing = { onAction(DetailUiAction.KeepEditingTags) },
        )
    }

    if (model.faviconEditor.visible) {
        FaviconEditorSheet(
            state = model.faviconEditor,
            isSaving = model.savingIcon,
            onTabSelected = { onAction(DetailUiAction.SelectFaviconTab(it)) },
            onSearchChanged = { onAction(DetailUiAction.UpdateFaviconSearch(it)) },
            onSourceSelected = { onAction(DetailUiAction.SelectFaviconSource(it)) },
            onUploadRequested = { onUploadFavicon() },
            onImageUrlChanged = { onAction(DetailUiAction.UpdateFaviconImageUrl(it)) },
            onDownloadRequested = { onAction(DetailUiAction.DownloadFaviconImage) },
            onSave = { onAction(DetailUiAction.SaveFavicon) },
            onDismiss = { onAction(DetailUiAction.DismissFaviconEditor) },
            onConfirmDiscard = { onAction(DetailUiAction.ConfirmDiscardFavicon) },
            onKeepEditing = { onAction(DetailUiAction.KeepEditingFavicon) },
        )
    }

    model.faviconEditor.pendingInputPath?.let { path ->
        FaviconCropScreen(
            stagedPath = path,
            processing = model.faviconEditor.processing,
            onCrop = { zoom, x, y ->
                onAction(DetailUiAction.CropFaviconImage(zoom, x, y))
            },
            onUseWithoutCrop = { onAction(DetailUiAction.UseFaviconWithoutCrop) },
            onCancel = { onAction(DetailUiAction.CancelFaviconCrop) },
        )
    }
}
