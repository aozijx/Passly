package com.aozijx.passly.data.failure

import com.aozijx.passly.domain.failure.AppFailure
import com.aozijx.passly.domain.failure.FailureOrigin
import com.aozijx.passly.domain.failure.FailureSeverity
import com.aozijx.passly.domain.failure.RecoveryAction
import com.aozijx.passly.domain.failure.StandardFailure
import java.io.IOException
import java.sql.SQLException

/**
 * Data/Security 边界异常到领域失败的统一映射。
 *
 * 使用方式：
 * ```kotlin
 * val failure = DataFailureMapper.map(throwable, "entry.fetch")
 * ```
 */
object DataFailureMapper {

    fun map(error: Throwable, operation: String): AppFailure {
        val code = resolveCode(error, operation)
        val severity = resolveSeverity(error)
        val recovery = resolveRecovery(error)
        return StandardFailure(
            code = code,
            origin = resolveOrigin(error),
            severity = severity,
            recoveryAction = recovery
        )
    }

    private fun resolveCode(error: Throwable, operation: String): String = when (error) {
        is IOException -> when {
            error.message?.contains("no space", ignoreCase = true) == true ->
                "DATA_DISK_FULL"

            error.message?.contains("permission denied", ignoreCase = true) == true ->
                "DATA_PERMISSION_DENIED"

            else -> "DATA_IO_ERROR"
        }

        is SQLException -> when {
            error.message?.contains("corrupt", ignoreCase = true) == true ->
                "DATA_DATABASE_CORRUPTED"

            error.message?.contains("locked", ignoreCase = true) == true ->
                "DATA_DATABASE_LOCKED"

            else -> "DATA_DATABASE_ERROR"
        }

        is SecurityException -> "DATA_SECURITY_VIOLATION"
        is IllegalArgumentException -> "DATA_INVALID_ARGUMENT"
        else -> "DATA_UNEXPECTED_ERROR"
    }

    private fun resolveOrigin(error: Throwable): FailureOrigin = when (error) {
        is SecurityException -> FailureOrigin.SECURITY
        else -> FailureOrigin.DATA
    }

    private fun resolveSeverity(error: Throwable): FailureSeverity = when (error) {
        is IOException, is SQLException -> FailureSeverity.ERROR
        is IllegalArgumentException -> FailureSeverity.WARNING
        else -> FailureSeverity.ERROR
    }

    private fun resolveRecovery(error: Throwable): RecoveryAction = when (error) {
        is IOException -> RecoveryAction.RETRY
        is SecurityException -> RecoveryAction.REAUTHENTICATE
        else -> RecoveryAction.NONE
    }
}
