package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.ui.shared.media.ImageType
import com.aozijx.passly.presentation.ui.shared.media.rememberImagePicker
import com.aozijx.passly.presentation.ui.vault.detail.component.FaviconCropScreen
import com.aozijx.passly.presentation.ui.vault.detail.component.FaviconEditorSheet
import com.aozijx.passly.presentation.ui.vault.detail.component.TagEditorSheet

@Composable
internal fun DetailEditorOverlaysHost(
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val pickFaviconImage = rememberImagePicker { uri, _ ->
        onAction(DetailUiAction.PickedFaviconImage(uri))
    }

    if (uiState.tagEditor.visible) {
        TagEditorSheet(
            state = uiState.tagEditor,
            isSaving = uiState.savingEdit == DetailEditCompletion.Tags,
            onInputChanged = { onAction(DetailUiAction.UpdateTagInput(it)) },
            onSubmit = { onAction(DetailUiAction.SubmitTag(it)) },
            onRemove = { onAction(DetailUiAction.RemoveTag(it)) },
            onSave = { onAction(DetailUiAction.SaveTags) },
            onDismiss = { onAction(DetailUiAction.DismissTagEditor) },
            onConfirmDiscard = { onAction(DetailUiAction.ConfirmDiscardTags) },
            onKeepEditing = { onAction(DetailUiAction.KeepEditingTags) },
        )
    }

    if (uiState.faviconEditor.visible) {
        key(uiState.faviconEditor.presentationId) {
            FaviconEditorSheet(
                state = uiState.faviconEditor,
                isSaving = uiState.savingEdit == DetailEditCompletion.Icon,
                onTabSelected = { onAction(DetailUiAction.SelectFaviconTab(it)) },
                onSearchChanged = { onAction(DetailUiAction.UpdateFaviconSearch(it)) },
                onSourceSelected = { onAction(DetailUiAction.SelectFaviconSource(it)) },
                onUploadRequested = { pickFaviconImage(ImageType.SCREEN) },
                onImageUrlChanged = { onAction(DetailUiAction.UpdateFaviconImageUrl(it)) },
                onDownloadRequested = { onAction(DetailUiAction.DownloadFaviconImage) },
                onSave = { onAction(DetailUiAction.SaveFavicon) },
                onDismiss = { onAction(DetailUiAction.DismissFaviconEditor) },
                onConfirmDiscard = { onAction(DetailUiAction.ConfirmDiscardFavicon) },
                onKeepEditing = { onAction(DetailUiAction.KeepEditingFavicon) },
            )
        }
    }

    uiState.faviconEditor.pendingInputPath?.let { path ->
        FaviconCropScreen(
            stagedPath = path,
            processing = uiState.faviconEditor.processing,
            onCrop = { zoom, x, y -> onAction(DetailUiAction.CropFaviconImage(zoom, x, y)) },
            onUseWithoutCrop = { onAction(DetailUiAction.UseFaviconWithoutCrop) },
            onCancel = { onAction(DetailUiAction.CancelFaviconCrop) },
        )
    }
}
