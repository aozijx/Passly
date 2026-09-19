package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.app.entry.favicon.FaviconDownloadException
import com.aozijx.passly.app.entry.favicon.FaviconDownloadFailure
import com.aozijx.passly.app.entry.favicon.FaviconImageException
import com.aozijx.passly.app.entry.favicon.FaviconImageFailure
import com.aozijx.passly.app.entry.favicon.FaviconUrlException
import com.aozijx.passly.app.entry.favicon.FaviconUrlFailure
import com.aozijx.passly.domain.entry.model.EntryIcon
internal fun EntryIcon.toDetailFaviconSource(): DetailFaviconSource {
    val privatePath = customReference
    val builtInName = name
    return when {
        !privatePath.isNullOrBlank() -> DetailFaviconSource.PrivateImage(privatePath)
        !builtInName.isNullOrBlank() -> DetailFaviconSource.BuiltIn(builtInName, color)
        else -> DetailFaviconSource.InferredDefault
    }
}

internal fun DetailFaviconSource.toEntryIcon(): EntryIcon = when (this) {
    DetailFaviconSource.InferredDefault -> EntryIcon()
    is DetailFaviconSource.BuiltIn -> EntryIcon(name = key, color = colorToken)
    is DetailFaviconSource.PrivateImage -> EntryIcon(customReference = localPath)
}

internal fun DetailFaviconEditorState.privateImagePath(): String? =
    (source as? DetailFaviconSource.PrivateImage)?.localPath

internal fun Throwable.toFaviconProcessingError(): DetailFaviconProcessingError = when (this) {
    is FaviconUrlException -> when (reason) {
        FaviconUrlFailure.INVALID_URL,
        FaviconUrlFailure.HTTPS_REQUIRED,
            -> DetailFaviconProcessingError.INVALID_URL

        FaviconUrlFailure.CREDENTIALS_NOT_ALLOWED,
        FaviconUrlFailure.HOST_NOT_ALLOWED,
        FaviconUrlFailure.PRIVATE_ADDRESS,
            -> DetailFaviconProcessingError.URL_NOT_ALLOWED
    }

    is FaviconDownloadException -> when (reason) {
        FaviconDownloadFailure.NOT_IMAGE -> DetailFaviconProcessingError.NOT_IMAGE
        FaviconDownloadFailure.TOO_LARGE -> DetailFaviconProcessingError.IMAGE_TOO_LARGE
        else -> DetailFaviconProcessingError.DOWNLOAD_FAILED
    }

    is FaviconImageException -> when (reason) {
        FaviconImageFailure.TOO_LARGE -> DetailFaviconProcessingError.IMAGE_TOO_LARGE
        FaviconImageFailure.SAVE_FAILED -> DetailFaviconProcessingError.SAVE_FAILED
        else -> DetailFaviconProcessingError.INVALID_IMAGE
    }

    else -> DetailFaviconProcessingError.INVALID_IMAGE
}
