package com.aozijx.passly.presentation.shared.error

import androidx.annotation.StringRes
import com.aozijx.passly.R
import kotlinx.coroutines.CancellationException
import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.BACKUP_FAILED
import com.aozijx.passly.core.error.model.CRYPTO_DATA_CORRUPTED
import com.aozijx.passly.core.error.model.CRYPTO_ERROR
import com.aozijx.passly.core.error.model.DATABASE_INIT_FAILED
import com.aozijx.passly.core.error.model.DATABASE_LOCKED
import com.aozijx.passly.core.error.model.FILE_IO_ERROR
import com.aozijx.passly.core.error.model.NETWORK_ERROR
import com.aozijx.passly.core.error.model.NOT_FOUND
import com.aozijx.passly.core.error.model.UNEXPECTED
import com.aozijx.passly.core.error.model.VALIDATION_ERROR

/** A presentation resource key, resolved using the current rendering context. */
@JvmInline
value class UiErrorMessage(@param:StringRes @get:StringRes val resourceId: Int)

/** Inline feature feedback remains separate from global notice routing. */
fun AppError.toUiErrorMessage(@StringRes fallback: Int = R.string.ui_error_generic): UiErrorMessage =
    UiErrorMessage(when (code) {
        DATABASE_LOCKED -> R.string.ui_error_database_locked
        DATABASE_INIT_FAILED -> R.string.ui_error_database_init_failed
        BACKUP_FAILED -> R.string.ui_error_backup_failed
        NETWORK_ERROR -> R.string.ui_error_network
        FILE_IO_ERROR -> R.string.ui_error_file_io
        CRYPTO_ERROR -> R.string.ui_error_crypto
        CRYPTO_DATA_CORRUPTED -> R.string.ui_error_crypto_corrupted
        VALIDATION_ERROR -> R.string.ui_error_validation
        NOT_FOUND -> R.string.ui_error_not_found
        UNEXPECTED -> fallback
        else -> fallback
    })

fun Throwable.toUiErrorMessage(@StringRes fallback: Int = R.string.ui_error_generic): UiErrorMessage {
    if (this is CancellationException) throw this
    return if (this is AppError) toUiErrorMessage(fallback)
    else AppError.fromThrowable(this).toUiErrorMessage(fallback)
}
