package com.aozijx.passly.core.error.result

import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.error.model.AppError
import kotlinx.coroutines.CancellationException

sealed class AppResult<out T> {

    data class Success<T>(val data: T) : AppResult<T>()
    data class Failure(val error: AppError) : AppResult<Nothing>()

    // ── 状态查询 ──
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    // ── 取值 ──
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    inline fun getOrElse(defaultBlock: (AppError) -> @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Failure -> defaultBlock(error)
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw error
    }

    // ── 副作用回调 ──
    fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    suspend fun onSuccessSuspend(action: suspend (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    fun onFailure(action: (AppError) -> Unit): AppResult<T> {
        if (this is Failure) action(error)
        return this
    }

    // ── 变换 ──
    fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    fun <R> flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
    }

    // ── 折叠 ──
    fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (AppError) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Failure -> onFailure(error)
    }

    // ── 构造器 ──
    companion object {
        fun <T> success(data: T): AppResult<T> = Success(data)
        fun failure(error: AppError): AppResult<Nothing> = Failure(error)

        suspend inline fun <T> runSuspendCatching(
            operation: String,
            crossinline block: suspend () -> T
        ): AppResult<T> {
            return try {
                success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure(AppError.fromThrowable(e))
            }
        }
    }
}