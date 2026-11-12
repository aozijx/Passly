package com.aozijx.passly.presentation.shared.error

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.CatalogErrorCodes
import kotlinx.coroutines.CancellationException

/** A presentation resource key, resolved using the current rendering context. */
@JvmInline
value class UiErrorMessage(
    @param:StringRes @get:StringRes val resourceId: Int,
)

/** Inline feature feedback remains separate from global notice routing. */
fun AppError.toUiErrorMessage(
    @StringRes fallback: Int = R.string.ui_error_generic,
): UiErrorMessage =
    UiErrorMessage(
        when (code) {
            CatalogErrorCodes.Database.Key.DATABASE_LOCKED -> R.string.ui_error_database_locked
            CatalogErrorCodes.Database.Initialization.DATABASE_INIT_FAILED -> R.string.ui_error_database_init_failed
            CatalogErrorCodes.Backup.Operation.BACKUP_FAILED -> R.string.ui_error_backup_failed
            CatalogErrorCodes.Application.Storage.FILE_IO_ERROR -> R.string.ui_error_file_io
            CatalogErrorCodes.Security.Cryptography.CRYPTO_ERROR -> R.string.ui_error_crypto
            CatalogErrorCodes.Security.Cryptography.CRYPTO_DATA_CORRUPTED -> R.string.ui_error_crypto_corrupted
            CatalogErrorCodes.Application.Validation.VALIDATION_ERROR -> R.string.ui_error_validation
            CatalogErrorCodes.Vault.Lookup.NOT_FOUND -> R.string.ui_error_not_found
            CatalogErrorCodes.Security.Session.SESSION_MODE_RESTRICTED -> R.string.ui_error_session_restricted
            else -> fallback
        },
    )

fun Throwable.toUiErrorMessage(
    @StringRes fallback: Int = R.string.ui_error_generic,
): UiErrorMessage {
    if (this is CancellationException) throw this
    if (this !is Exception) throw this
    return if (this is AppError) {
        toUiErrorMessage(fallback)
    } else {
        AppError.fromThrowable(this).toUiErrorMessage(fallback)
    }
}
