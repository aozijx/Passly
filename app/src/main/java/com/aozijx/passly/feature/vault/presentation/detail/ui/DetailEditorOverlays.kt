package com.aozijx.passly.feature.vault.presentation.detail.ui

import androidx.compose.runtime.Composable
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.FaviconCropScreen
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.FaviconEditorSheet
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.TagEditorSheet
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailEditorOverlaysUiModel
import com.aozijx.passly.feature.vault.presentation.detail.DetailFaviconEditorState
import com.aozijx.passly.feature.vault.presentation.detail.DetailTagEditorState
import com.aozijx.passly.feature.vault.presentation.detail.DetailFaviconWorkState

@Composable
fun DetailEditorOverlays(
    model: DetailEditorOverlaysUiModel,
    onAction: (DetailUiAction) -> Unit,
    onFaviconUploadRequested: () -> Unit,
) {
    when (val tagEditor = model.tagEditor) {
        DetailTagEditorState.Closed -> {
            Unit
        }

        is DetailTagEditorState.Open -> {
            TagEditorSheet(
                state = tagEditor,
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
    }

    when (val faviconEditor = model.faviconEditor) {
        DetailFaviconEditorState.Closed -> {
            Unit
        }

        is DetailFaviconEditorState.Open -> {
            FaviconEditorSheet(
                state = faviconEditor,
                isSaving = model.savingIcon,
                onTabSelected = { onAction(DetailUiAction.SelectFaviconTab(it)) },
                onSearchChanged = { onAction(DetailUiAction.UpdateFaviconSearch(it)) },
                onSourceSelected = { onAction(DetailUiAction.SelectFaviconSource(it)) },
                onUploadRequested = onFaviconUploadRequested,
                onImageUrlChanged = { onAction(DetailUiAction.UpdateFaviconImageUrl(it)) },
                onDownloadRequested = { onAction(DetailUiAction.DownloadFaviconImage) },
                onSave = { onAction(DetailUiAction.SaveFavicon) },
                onDismiss = { onAction(DetailUiAction.DismissFaviconEditor) },
                onConfirmDiscard = { onAction(DetailUiAction.ConfirmDiscardFavicon) },
                onKeepEditing = { onAction(DetailUiAction.KeepEditingFavicon) },
            )
            faviconEditor.pendingInputPath?.let { path ->
                FaviconCropScreen(
                    stagedPath = path,
                    processing = faviconEditor.work == DetailFaviconWorkState.Processing,
                    onCrop = { zoom, x, y ->
                        onAction(DetailUiAction.CropFaviconImage(zoom, x, y))
                    },
                    onUseWithoutCrop = { onAction(DetailUiAction.UseFaviconWithoutCrop) },
                    onCancel = { onAction(DetailUiAction.CancelFaviconCrop) },
                )
            }
        }
    }
}
