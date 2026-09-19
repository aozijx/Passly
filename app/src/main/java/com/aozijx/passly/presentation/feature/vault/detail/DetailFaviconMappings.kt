package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.core.platform.media.FaviconProcessingException
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

internal fun Throwable.toFaviconProcessingError(): DetailFaviconProcessingError =
    (this as? FaviconProcessingException)
        ?.failure
        ?.name
        ?.let(DetailFaviconProcessingError::valueOf)
        ?: DetailFaviconProcessingError.INVALID_IMAGE
