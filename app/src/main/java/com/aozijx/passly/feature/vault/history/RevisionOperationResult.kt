package com.aozijx.passly.feature.vault.history

import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationFailure

internal sealed interface RevisionOperationResult<out T> {
    data class Succeeded<T>(val value: T) : RevisionOperationResult<T>
    data object Missing : RevisionOperationResult<Nothing>
    data object Stale : RevisionOperationResult<Nothing>
    data object InvalidSnapshot : RevisionOperationResult<Nothing>
    data class AuthenticationDenied(val failure: AuthenticationFailure) : RevisionOperationResult<Nothing>
    data object Cancelled : RevisionOperationResult<Nothing>
    data class Failed(val error: AppError) : RevisionOperationResult<Nothing>
}

internal fun <T> AppResult<T>.toRevisionOperationResult(): RevisionOperationResult<T> = when (this) {
    is AppResult.Success -> RevisionOperationResult.Succeeded(data)
    is AppResult.Failure -> error.toRevisionOperationFailure()
}

internal fun AppError.toRevisionOperationFailure(): RevisionOperationResult<Nothing> = when (this) {
    is NotFound -> RevisionOperationResult.Missing
    is Conflict -> RevisionOperationResult.Stale
    is ValidationError -> RevisionOperationResult.InvalidSnapshot
    else -> RevisionOperationResult.Failed(this)
}
