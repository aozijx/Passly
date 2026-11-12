package com.aozijx.passly.feature.vault.presentation.detail

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.media.FaviconProcessingException
import com.aozijx.passly.core.platform.media.FaviconProcessingFailure
import com.aozijx.passly.domain.entry.model.EntryIcon

internal fun EntryIcon.toDetailFaviconSource(): DetailFaviconSource {
    val privatePath = customReference
    val builtInName = name
    return when {
        !privatePath.isNullOrBlank() -> {
            DetailFaviconSource.PrivateImage(privatePath)
        }

        !builtInName.isNullOrBlank() -> {
            DetailFaviconSource.BuiltIn(
                key = builtInName,
                color =
                    color?.let(DetailFaviconSource.ColorSelection::Token)
                        ?: DetailFaviconSource.ColorSelection.Automatic,
            )
        }

        else -> {
            DetailFaviconSource.InferredDefault
        }
    }
}

internal fun DetailFaviconSource.toEntryIcon(): EntryIcon =
    when (this) {
        DetailFaviconSource.InferredDefault -> EntryIcon()
        is DetailFaviconSource.BuiltIn -> EntryIcon(name = key, color = color.toStorageToken())
        is DetailFaviconSource.PrivateImage -> EntryIcon(customReference = localPath)
    }

internal fun DetailFaviconSource.ColorSelection.toStorageToken(): String? =
    when (this) {
        DetailFaviconSource.ColorSelection.Automatic -> null
        is DetailFaviconSource.ColorSelection.Token -> value
    }

@StringRes
internal fun Throwable.toFaviconProcessingMessageRes(): Int =
    when ((this as? FaviconProcessingException)?.failure) {
        FaviconProcessingFailure.INVALID_URL -> R.string.vault_detail_favicon_error_invalid_url
        FaviconProcessingFailure.URL_NOT_ALLOWED -> R.string.vault_detail_favicon_error_url_not_allowed
        FaviconProcessingFailure.DOWNLOAD_FAILED -> R.string.vault_detail_favicon_error_download_failed
        FaviconProcessingFailure.NOT_IMAGE -> R.string.vault_detail_favicon_error_not_image
        FaviconProcessingFailure.IMAGE_TOO_LARGE -> R.string.vault_detail_favicon_error_too_large
        FaviconProcessingFailure.INVALID_IMAGE, null -> R.string.vault_detail_favicon_error_invalid_image
        FaviconProcessingFailure.SAVE_FAILED -> R.string.vault_detail_favicon_error_save_failed
    }
