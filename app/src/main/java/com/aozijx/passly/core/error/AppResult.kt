package com.aozijx.passly.core.error

import kotlinx.coroutines.CancellationException

sealed class AppResult<out T> {

    data class Success<T>(val data: T) : AppResult<T>()

    data class Failure(val error: AppError) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success

    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    fun onFailure(action: (AppError) -> Unit): AppResult<T> {
        if (this is Failure) action(error)
        return this
    }

    companion object {
        fun <T> success(data: T): AppResult<T> = Success(data)
        fun failure(error: AppError): AppResult<Nothing> = Failure(error)

        inline fun <T> runCatching(block: () -> T): AppResult<T> {
            return try {
                success(block())
            } catch (e: AppError) {
                failure(e)
            } catch (e: Exception) {
                failure(AppError.Unexpected(cause = e))
            }
        }

        suspend inline fun <T> runSuspendCatching(
            operation: String,
            layer: ErrorLayer = ErrorLayer.DOMAIN,
            crossinline block: suspend () -> T
        ): AppResult<T> {
            return try {
                success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                failure(AppError.fromThrowable(e, layer = layer, operation = operation))
            }
        }
    }
}