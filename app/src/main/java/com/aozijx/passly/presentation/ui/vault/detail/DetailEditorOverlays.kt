package com.aozijx.passly.presentation.ui.vault.detail

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.ui.vault.detail.component.FaviconCropScreen
import com.aozijx.passly.presentation.ui.vault.detail.component.FaviconEditorSheet
import com.aozijx.passly.presentation.ui.vault.detail.component.TagEditorSheet
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailEditorOverlayEvent
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailEditorOverlaysUiModel

@Composable
fun DetailEditorOverlays(
    model: DetailEditorOverlaysUiModel,
    onEvent: (DetailEditorOverlayEvent) -> Unit,
) {
    if (model.tagEditor.visible) {
        TagEditorSheet(
            state = model.tagEditor,
            isSaving = model.savingTags,
            onInputChanged = { onEvent(DetailEditorOverlayEvent.UpdateTagInput(it)) },
            onSubmit = { onEvent(DetailEditorOverlayEvent.SubmitTag(it)) },
            onRemove = { onEvent(DetailEditorOverlayEvent.RemoveTag(it)) },
            onSave = { onEvent(DetailEditorOverlayEvent.SaveTags) },
            onDismiss = { onEvent(DetailEditorOverlayEvent.DismissTagEditor) },
            onConfirmDiscard = { onEvent(DetailEditorOverlayEvent.ConfirmDiscardTags) },
            onKeepEditing = { onEvent(DetailEditorOverlayEvent.KeepEditingTags) },
        )
    }

    if (model.faviconEditor.visible) {
        FaviconEditorSheet(
            state = model.faviconEditor,
            isSaving = model.savingIcon,
            onTabSelected = { onEvent(DetailEditorOverlayEvent.SelectFaviconTab(it)) },
            onSearchChanged = { onEvent(DetailEditorOverlayEvent.UpdateFaviconSearch(it)) },
            onSourceSelected = { onEvent(DetailEditorOverlayEvent.SelectFaviconSource(it)) },
            onUploadRequested = { onEvent(DetailEditorOverlayEvent.UploadFavicon) },
            onImageUrlChanged = { onEvent(DetailEditorOverlayEvent.UpdateFaviconImageUrl(it)) },
            onDownloadRequested = { onEvent(DetailEditorOverlayEvent.DownloadFaviconImage) },
            onSave = { onEvent(DetailEditorOverlayEvent.SaveFavicon) },
            onDismiss = { onEvent(DetailEditorOverlayEvent.DismissFaviconEditor) },
            onConfirmDiscard = { onEvent(DetailEditorOverlayEvent.ConfirmDiscardFavicon) },
            onKeepEditing = { onEvent(DetailEditorOverlayEvent.KeepEditingFavicon) },
        )
    }

    model.faviconEditor.pendingInputPath?.let { path ->
        FaviconCropScreen(
            stagedPath = path,
            processing = model.faviconEditor.processing,
            onCrop = { zoom, x, y ->
                onEvent(DetailEditorOverlayEvent.CropFavicon(zoom, x, y))
            },
            onUseWithoutCrop = { onEvent(DetailEditorOverlayEvent.UseFaviconWithoutCrop) },
            onCancel = { onEvent(DetailEditorOverlayEvent.CancelFaviconCrop) },
        )
    }
}
