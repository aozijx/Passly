package com.aozijx.passly.core.error.mapping

import com.aozijx.passly.core.error.boundary.CryptoException
import com.aozijx.passly.core.error.boundary.DatabaseException
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.CryptoDataCorrupted
import com.aozijx.passly.core.error.model.CryptoError
import com.aozijx.passly.core.error.model.DatabaseInitFailed
import com.aozijx.passly.core.error.model.DatabaseLocked
import com.aozijx.passly.core.error.model.FileIOError
import com.aozijx.passly.core.error.model.Unexpected
import java.io.FileNotFoundException
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * 异常到 AppError 的映射。独立文件，避免 AppError.kt 循环依赖域子类。
 */
fun AppError.Companion.fromThrowable(
    throwable: Throwable
): AppError {
    return when (throwable) {
        is AppError -> throwable
        is DatabaseException.InvalidPassphraseException -> DatabaseLocked()
        is DatabaseException -> DatabaseInitFailed(throwableType = throwable.javaClass.simpleName)
        is CryptoException.TagVerificationFailed -> CryptoDataCorrupted()
        is CryptoException -> CryptoError(throwableType = throwable.javaClass.simpleName)
        is FileNotFoundException -> FileIOError(throwableType = throwable.javaClass.simpleName)
        is IOException -> FileIOError(throwableType = throwable.javaClass.simpleName)
        is GeneralSecurityException -> CryptoError(throwableType = throwable.javaClass.simpleName)
        else -> Unexpected(throwableType = throwable.javaClass.simpleName)
    }
}