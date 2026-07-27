package com.aozijx.passly.core.error

import java.io.FileNotFoundException
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * 异常到 AppError 的映射。独立文件，避免 AppError.kt 循环依赖域子类。
 */
fun AppError.Companion.fromThrowable(
    throwable: Throwable,
    layer: ErrorLayer,
    operation: String? = null
): AppError {
    return when (throwable) {
        is AppError -> throwable
        is FileNotFoundException -> FileIOError(
            message = "文件未找到: ${throwable.message}",
            trace = ErrorTrace(layer, operation),
            cause = throwable
        )

        is IOException -> NetworkError(
            message = throwable.message ?: "网络 IO 异常",
            trace = ErrorTrace(layer, operation),
            cause = throwable
        )

        is GeneralSecurityException -> CryptoError(
            message = "加解密失败: ${throwable.message}",
            trace = ErrorTrace(layer, operation),
            cause = throwable
        )

        else -> Unexpected(
            message = throwable.message ?: "发生未知错误",
            trace = ErrorTrace(layer, operation),
            cause = throwable
        )
    }
}