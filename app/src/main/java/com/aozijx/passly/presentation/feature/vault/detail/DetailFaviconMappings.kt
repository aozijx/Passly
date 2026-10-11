package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.core.platform.media.FaviconProcessingException
import com.aozijx.passly.core.platform.media.FaviconProcessingFailure
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

internal fun DetailFaviconEditorState.privateImagePath(): String? = when (this) {
    DetailFaviconEditorState.Closed -> null
    is DetailFaviconEditorState.Open ->
        (source as? DetailFaviconSource.PrivateImage)?.localPath
}

internal fun Throwable.toFaviconProcessingError(): DetailFaviconProcessingError =
    when ((this as? FaviconProcessingException)?.failure) {
        FaviconProcessingFailure.INVALID_URL ->
            DetailFaviconProcessingError.INVALID_URL

        FaviconProcessingFailure.URL_NOT_ALLOWED ->
            DetailFaviconProcessingError.URL_NOT_ALLOWED

        FaviconProcessingFailure.DOWNLOAD_FAILED ->
            DetailFaviconProcessingError.DOWNLOAD_FAILED

        FaviconProcessingFailure.NOT_IMAGE ->
            DetailFaviconProcessingError.NOT_IMAGE

        FaviconProcessingFailure.IMAGE_TOO_LARGE ->
            DetailFaviconProcessingError.IMAGE_TOO_LARGE

        FaviconProcessingFailure.INVALID_IMAGE ->
            DetailFaviconProcessingError.INVALID_IMAGE

        FaviconProcessingFailure.SAVE_FAILED ->
            DetailFaviconProcessingError.SAVE_FAILED

        null -> DetailFaviconProcessingError.INVALID_IMAGE
    }
