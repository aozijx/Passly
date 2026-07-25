package com.aozijx.passly.core.error

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

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Failure -> default
    }

    // 惰性求值默认值
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

    fun mapFailure(transform: (AppError) -> AppError): AppResult<T> = when (this) {
        is Success -> this
        is Failure -> Failure(transform(error))
    }

    fun <R> flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
    }

    // 错误恢复（将失败转为成功）
    inline fun recover(transform: (AppError) -> @UnsafeVariance T): AppResult<T> = when (this) {
        is Success -> this
        is Failure -> Success(transform(error))
    }

    // 错误恢复（将失败转为另一个 Result）
    inline fun recoverWith(transform: (AppError) -> AppResult<@UnsafeVariance T>): AppResult<T> =
        when (this) {
            is Success -> this
            is Failure -> transform(error)
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

        // 强制要求传入 operation，避免忘记填写
        inline fun <T> runCatching(
            operation: String,  // 无默认值
            layer: ErrorLayer = ErrorLayer.DOMAIN,
            block: () -> T
        ): AppResult<T> {
            return try {
                success(block())
            } catch (e: AppError) {
                failure(e)
            } catch (e: Exception) {
                failure(AppError.fromThrowable(e, layer = layer, operation = operation))
            }
        }

        suspend inline fun <T> runSuspendCatching(
            operation: String,  // 无默认值
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

// ─── 与 Kotlin 原生 Result 互操作 ──────────────

fun <T> AppResult<T>.toResult(): Result<T> = when (this) {
    is AppResult.Success -> Result.success(data)
    is AppResult.Failure -> Result.failure(error)
}

fun <T> Result<T>.toAppResult(layer: ErrorLayer = ErrorLayer.DOMAIN): AppResult<T> = fold(
    onSuccess = { AppResult.success(it) },
    onFailure = {
        AppResult.failure(
            AppError.fromThrowable(
                it,
                layer = layer,
                operation = "Result转化"
            )
        )
    }
)