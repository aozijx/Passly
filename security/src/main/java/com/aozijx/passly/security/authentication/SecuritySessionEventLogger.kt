package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.fields
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.LockReason
import javax.inject.Inject

class SecuritySessionEventLogger @Inject internal constructor(
    private val logger: Logger,
) {
    suspend fun <T> traceLock(
        reason: LockReason,
        timeoutMs: Long,
        block: suspend LockTraceScope.() -> T,
    ): T = logger.withRootTrace(Operations.SecurityLock) {
        val startedAt = elapsedRealtimeMs()
        val traceScope = LockTraceScope()
        logger.info(Events.Security.LockStarted, lockFields(reason, timeoutMs, SecurityResult.STARTED, 0))
        try {
            traceScope.block().also {
                val handledFailure = traceScope.failure
                if (handledFailure != null) {
                    logger.error(
                        Events.Security.LockCompleted,
                        ErrorCodes.SecurityLockFailed,
                        Reasons.SecurityLockOperationFailed,
                        handledFailure,
                        lockFields(reason, timeoutMs, SecurityResult.FAILURE, elapsedSince(startedAt)),
                    )
                } else {
                logger.info(
                        Events.Security.LockCompleted,
                        lockFields(reason, timeoutMs, SecurityResult.SUCCESS, elapsedSince(startedAt)),
                    )
                }
            }
        } catch (failure: Throwable) {
            logger.error(
                Events.Security.LockCompleted,
                ErrorCodes.SecurityLockFailed,
                Reasons.SecurityLockOperationFailed,
                failure,
                lockFields(reason, timeoutMs, SecurityResult.FAILURE, elapsedSince(startedAt)),
            )
            throw failure
        }
    }

    suspend fun <T> traceAuthentication(
        method: AuthenticationMethod,
        block: suspend () -> T,
    ): T = logger.withRootTrace(Operations.SecurityAuthentication) {
        val startedAt = elapsedRealtimeMs()
        logger.info(Events.Security.UnlockStarted, authenticationFields(method, SecurityResult.STARTED, 0))
        try {
            block().also {
                logger.info(
                    Events.Security.UnlockCompleted,
                    authenticationFields(method, SecurityResult.SUCCESS, elapsedSince(startedAt)),
                )
            }
        } catch (failure: Throwable) {
            logger.error(
                Events.Security.UnlockCompleted,
                ErrorCodes.AuthenticationFailed,
                Reasons.UnknownFailure,
                failure,
                authenticationFields(method, SecurityResult.FAILURE, elapsedSince(startedAt)),
            )
            throw failure
        }
    }

    suspend fun traceAuthentication(
        block: suspend AuthenticationTraceScope.() -> AuthenticationResult,
    ): AuthenticationResult = logger.withRootTrace(Operations.SecurityAuthentication) {
        val startedAt = elapsedRealtimeMs()
        val traceScope = AuthenticationTraceScope()
        logger.info(Events.Security.UnlockStarted)
        val result = try {
            traceScope.block()
        } catch (failure: Throwable) {
            logger.error(
                Events.Security.AuthenticationFailed,
                ErrorCodes.AuthenticationFailed,
                Reasons.UnknownFailure,
                failure,
                fields { durationMs(LogFieldKeys.ElapsedMs, elapsedSince(startedAt)) },
            )
            throw failure
        }
        val method = (result as? AuthenticationResult.Success)?.method
        val outcome = when (result) {
            is AuthenticationResult.Success -> SecurityResult.SUCCESS
            is AuthenticationResult.Cancelled -> SecurityResult.CANCELLED
            is AuthenticationResult.Failure -> SecurityResult.FAILURE
        }
        val resultFields = fields {
            method?.let { enum(LogFieldKeys.AuthenticationMethod, it.toLogMethod()) }
            enum(LogFieldKeys.Result, outcome)
            durationMs(LogFieldKeys.ElapsedMs, elapsedSince(startedAt))
        }
        val handledFailure = traceScope.failure
        if (handledFailure != null) {
            logger.error(
                Events.Security.AuthenticationFailed,
                ErrorCodes.AuthenticationFailed,
                Reasons.UnknownFailure,
                handledFailure,
                resultFields,
            )
        } else if (result is AuthenticationResult.Failure) {
            logger.degraded(
                Events.Security.AuthenticationFailed,
                ErrorCodes.AuthenticationFailed,
                Reasons.AuthenticationRejected,
                resultFields,
            )
        } else {
            logger.info(Events.Security.UnlockCompleted, resultFields)
        }
        result
    }

    class LockTraceScope internal constructor() {
        internal var failure: Throwable? = null
            private set

        fun recordFailure(failure: Throwable) {
            if (this.failure == null) this.failure = failure
        }
    }

    class AuthenticationTraceScope internal constructor() {
        internal var failure: Throwable? = null
            private set

        fun recordFailure(failure: Throwable) {
            if (this.failure == null) this.failure = failure
        }
    }

    private fun lockFields(
        reason: LockReason,
        timeoutMs: Long,
        result: SecurityResult,
        elapsedMs: Long,
    ): LogFields = fields {
        reason(LogFieldKeys.LockReason, reason.toReasonCode())
        enum(LogFieldKeys.Trigger, reason.toTrigger())
        durationMs(LogFieldKeys.TimeoutMs, timeoutMs)
        enum(LogFieldKeys.Result, result)
        durationMs(LogFieldKeys.ElapsedMs, elapsedMs)
    }

    private fun authenticationFields(
        method: AuthenticationMethod,
        result: SecurityResult,
        elapsedMs: Long,
    ): LogFields = fields {
        enum(LogFieldKeys.AuthenticationMethod, method.toLogMethod())
        enum(LogFieldKeys.Result, result)
        durationMs(LogFieldKeys.ElapsedMs, elapsedMs)
    }

    private fun LockReason.toReasonCode(): ReasonCode = when (this) {
        LockReason.USER -> Reasons.User
        LockReason.IDLE_TIMEOUT -> Reasons.IdleTimeout
        LockReason.AUTOFILL_REQUEST_FINISHED -> Reasons.AutofillRequestFinished
        LockReason.BACKGROUND -> Reasons.Background
        LockReason.RECOVERY_EXIT -> Reasons.RecoveryExit
        LockReason.INTEGRITY_FAILURE -> Reasons.IntegrityFailure
        LockReason.APP_EXIT -> Reasons.AppExit
    }

    private fun LockReason.toTrigger(): SecurityTrigger = when (this) {
        LockReason.USER -> SecurityTrigger.USER
        LockReason.IDLE_TIMEOUT -> SecurityTrigger.IDLE_TIMER
        LockReason.AUTOFILL_REQUEST_FINISHED -> SecurityTrigger.AUTOFILL
        LockReason.BACKGROUND, LockReason.APP_EXIT -> SecurityTrigger.LIFECYCLE
        LockReason.RECOVERY_EXIT -> SecurityTrigger.RECOVERY
        LockReason.INTEGRITY_FAILURE -> SecurityTrigger.INTEGRITY
    }

    private fun AuthenticationMethod.toLogMethod(): AuthenticationMethodLog = when (this) {
        AuthenticationMethod.BIOMETRIC -> AuthenticationMethodLog.BIOMETRIC
        AuthenticationMethod.APP_PASSWORD -> AuthenticationMethodLog.MASTER_PASSWORD
        AuthenticationMethod.RECOVERY_CODE -> AuthenticationMethodLog.RECOVERY
    }

    private fun elapsedRealtimeMs(): Long = System.nanoTime() / NANOS_PER_MILLISECOND
    private fun elapsedSince(startedAt: Long): Long = (elapsedRealtimeMs() - startedAt).coerceAtLeast(0)

    private enum class SecurityTrigger { USER, IDLE_TIMER, AUTOFILL, LIFECYCLE, RECOVERY, INTEGRITY }
    private enum class SecurityResult { STARTED, SUCCESS, FAILURE, CANCELLED }
    private enum class AuthenticationMethodLog { BIOMETRIC, MASTER_PASSWORD, RECOVERY }

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
