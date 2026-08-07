package com.aozijx.passly.core.error

import com.aozijx.passly.core.error.boundary.CryptoException
import com.aozijx.passly.core.error.boundary.DatabaseException
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
    val trace = ErrorTrace(layer, operation)
    return when (throwable) {
        is AppError -> throwable
        is DatabaseException.InvalidPassphraseException -> DatabaseLocked(
            message = "数据库解锁失败，请检查认证状态",
            trace = trace,
            cause = throwable
        )

        is DatabaseException -> DatabaseInitFailed(
            message = "数据库初始化失败",
            trace = trace,
            cause = throwable
        )

        is CryptoException.TagVerificationFailed -> CryptoDataCorrupted(
            message = "加密数据损坏或密钥不匹配",
            trace = trace,
            cause = throwable
        )

        is CryptoException -> CryptoError(
            message = "加解密失败",
            trace = trace,
            cause = throwable
        )

        is FileNotFoundException -> FileIOError(
            message = "文件未找到",
            trace = trace,
            cause = throwable
        )

        is IOException -> NetworkError(
            message = "网络或文件 IO 异常",
            trace = trace,
            cause = throwable
        )

        is GeneralSecurityException -> CryptoError(
            message = "加解密失败",
            trace = trace,
            cause = throwable
        )

        else -> Unexpected(
            message = "发生未知错误",
            trace = trace,
            cause = throwable
        )
    }
}
