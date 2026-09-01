package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.app.entry.favicon.FaviconDownloadException
import com.aozijx.passly.app.entry.favicon.FaviconDownloadFailure
import com.aozijx.passly.app.entry.favicon.FaviconImageException
import com.aozijx.passly.app.entry.favicon.FaviconImageFailure
import com.aozijx.passly.app.entry.favicon.FaviconUrlException
import com.aozijx.passly.app.entry.favicon.FaviconUrlFailure
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconProcessingErrorUiModel

internal fun EntryIcon.toFaviconDraftSource(): FaviconDraftSourceUiModel {
    val privatePath = customReference
    val builtInName = name
    return when {
        !privatePath.isNullOrBlank() -> FaviconDraftSourceUiModel.PrivateImage(privatePath)
        !builtInName.isNullOrBlank() -> FaviconDraftSourceUiModel.BuiltIn(builtInName, color)
        else -> FaviconDraftSourceUiModel.InferredDefault
    }
}

internal fun FaviconDraftSourceUiModel.toEntryIcon(): EntryIcon = when (this) {
    FaviconDraftSourceUiModel.InferredDefault -> EntryIcon()
    is FaviconDraftSourceUiModel.BuiltIn -> EntryIcon(name = key, color = colorToken)
    is FaviconDraftSourceUiModel.PrivateImage -> EntryIcon(customReference = localPath)
}

internal fun DetailFaviconEditorUiModel.privateImagePath(): String? =
    (source as? FaviconDraftSourceUiModel.PrivateImage)?.localPath

internal fun Throwable.toFaviconUiError(): FaviconProcessingErrorUiModel = when (this) {
    is FaviconUrlException -> when (reason) {
        FaviconUrlFailure.INVALID_URL,
        FaviconUrlFailure.HTTPS_REQUIRED,
            -> FaviconProcessingErrorUiModel.INVALID_URL

        FaviconUrlFailure.CREDENTIALS_NOT_ALLOWED,
        FaviconUrlFailure.HOST_NOT_ALLOWED,
        FaviconUrlFailure.PRIVATE_ADDRESS,
            -> FaviconProcessingErrorUiModel.URL_NOT_ALLOWED
    }

    is FaviconDownloadException -> when (reason) {
        FaviconDownloadFailure.NOT_IMAGE -> FaviconProcessingErrorUiModel.NOT_IMAGE
        FaviconDownloadFailure.TOO_LARGE -> FaviconProcessingErrorUiModel.IMAGE_TOO_LARGE
        else -> FaviconProcessingErrorUiModel.DOWNLOAD_FAILED
    }

    is FaviconImageException -> when (reason) {
        FaviconImageFailure.TOO_LARGE -> FaviconProcessingErrorUiModel.IMAGE_TOO_LARGE
        FaviconImageFailure.SAVE_FAILED -> FaviconProcessingErrorUiModel.SAVE_FAILED
        else -> FaviconProcessingErrorUiModel.INVALID_IMAGE
    }

    else -> FaviconProcessingErrorUiModel.INVALID_IMAGE
}
