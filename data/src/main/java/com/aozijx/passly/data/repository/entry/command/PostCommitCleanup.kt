package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.fields
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/** Runs retryable housekeeping after a durable write without rewriting its transaction result. */
@Singleton
internal class PostCommitCleanup @Inject constructor(
    private val logger: Logger,
) {
    suspend fun <T> afterCommit(
        result: AppResult<T>,
        operation: OperationCode = Operations.ResourceCleanup,
        reason: ReasonCode = Reasons.StorageFailure,
        action: suspend (T) -> Unit,
    ): AppResult<T> = when (result) {
        is AppResult.Failure -> result
        is AppResult.Success -> {
            run(operation, reason) { action(result.data) }
            result
        }
    }

    suspend fun run(
        operation: OperationCode = Operations.ResourceCleanup,
        reason: ReasonCode = Reasons.StorageFailure,
        action: suspend () -> Unit,
    ) {
        try {
            logger.inSpan(operation) { action() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            try {
                logger.warn(
                    event = Events.Application.ResourceCleanupFailed,
                    errorCode = ErrorCodes.Application.ResourceCleanupFailed,
                    reason = reason,
                    throwable = error,
                    fields = fields { operation(LogFieldKeys.Operation, operation) },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (reportingError: Exception) {
                if (reportingError !== error) error.addSuppressed(reportingError)
            }
        }
    }
}
