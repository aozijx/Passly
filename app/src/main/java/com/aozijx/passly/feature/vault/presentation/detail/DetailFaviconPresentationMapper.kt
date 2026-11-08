package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailFaviconEditorUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.FaviconOverlayUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.FaviconProcessingErrorUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.FaviconWorkUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.FaviconImageUrlDownloadUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.FaviconIconUiModel
import com.aozijx.passly.presentation.shared.components.VaultIcons
import java.net.URI
import java.net.URISyntaxException

internal fun DetailFaviconEditorState.toFaviconEditorUiModel(): DetailFaviconEditorUiModel =
    when (this) {
        DetailFaviconEditorState.Closed -> {
            DetailFaviconEditorUiModel.Hidden
        }

        is DetailFaviconEditorState.Open -> {
            DetailFaviconEditorUiModel.Visible(
                initialSource = initialSource,
                source = source,
                selectedTab = selectedTab,
                searchQuery = searchQuery,
                matchingIcons = VaultIcons.search(searchQuery).map { FaviconIconUiModel(it.key, it.labelRes, it.imageVector) },
                imageUrl = imageUrl,
                imageUrlDownloadState = imageUrl.downloadUiState(),
                work = work.toUiModel(),
                pendingInputPath = pendingInputPath,
                promotedCandidatePath = promotedCandidatePath,
                overlay = overlay.toUiModel(),
            )
        }
    }

private fun String.downloadUiState(): FaviconImageUrlDownloadUiState {
    val uri =
        try {
            URI(trim())
        } catch (_: URISyntaxException) {
            return FaviconImageUrlDownloadUiState.Disabled
        }
    return if (uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) {
        FaviconImageUrlDownloadUiState.Enabled
    } else {
        FaviconImageUrlDownloadUiState.Disabled
    }
}

private fun DetailFaviconProcessingError.toUiModel(): FaviconProcessingErrorUiModel =
    when (this) {
        DetailFaviconProcessingError.INVALID_URL -> FaviconProcessingErrorUiModel.INVALID_URL
        DetailFaviconProcessingError.URL_NOT_ALLOWED -> FaviconProcessingErrorUiModel.URL_NOT_ALLOWED
        DetailFaviconProcessingError.DOWNLOAD_FAILED -> FaviconProcessingErrorUiModel.DOWNLOAD_FAILED
        DetailFaviconProcessingError.NOT_IMAGE -> FaviconProcessingErrorUiModel.NOT_IMAGE
        DetailFaviconProcessingError.IMAGE_TOO_LARGE -> FaviconProcessingErrorUiModel.IMAGE_TOO_LARGE
        DetailFaviconProcessingError.INVALID_IMAGE -> FaviconProcessingErrorUiModel.INVALID_IMAGE
        DetailFaviconProcessingError.SAVE_FAILED -> FaviconProcessingErrorUiModel.SAVE_FAILED
    }

private fun DetailFaviconWorkState.toUiModel(): FaviconWorkUiState =
    when (this) {
        DetailFaviconWorkState.Idle -> FaviconWorkUiState.Idle
        DetailFaviconWorkState.Processing -> FaviconWorkUiState.Processing
        is DetailFaviconWorkState.Failed -> FaviconWorkUiState.Failed(error.toUiModel())
    }

private fun DetailFaviconOverlay.toUiModel(): FaviconOverlayUiState =
    when (this) {
        DetailFaviconOverlay.None -> FaviconOverlayUiState.None
        DetailFaviconOverlay.ConfirmDiscard -> FaviconOverlayUiState.ConfirmDiscard
    }
