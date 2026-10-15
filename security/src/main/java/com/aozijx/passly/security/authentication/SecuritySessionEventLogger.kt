package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
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
        logger.log(
            LogStatements.Security.lockStarted(
                lockFields(reason, timeoutMs, SecurityResult.STARTED, 0),
            ),
        )
        try {
            traceScope.block().also {
                val handledFailure = traceScope.failure
                if (handledFailure != null) {
                    logger.log(
                        LogStatements.Security.lockFailed(
                            handledFailure,
                            lockFields(
                                reason,
                                timeoutMs,
                                SecurityResult.FAILURE,
                                elapsedSince(startedAt),
                            ),
                        ),
                    )
                } else {
                    logger.log(
                        LogStatements.Security.lockCompleted(
                            lockFields(
                                reason,
                                timeoutMs,
                                SecurityResult.SUCCESS,
                                elapsedSince(startedAt),
                            ),
                        ),
                    )
                }
            }
        } catch (failure: Throwable) {
            logger.log(
                LogStatements.Security.lockFailed(
                    failure,
                    lockFields(
                        reason,
                        timeoutMs,
                        SecurityResult.FAILURE,
                        elapsedSince(startedAt),
                    ),
                ),
            )
            throw failure
        }
    }

    suspend fun <T> traceAuthentication(
        method: AuthenticationMethod,
        block: suspend () -> T,
    ): T = logger.withRootTrace(Operations.SecurityAuthentication) {
        val startedAt = elapsedRealtimeMs()
        logger.log(
            LogStatements.Security.authenticationStarted(
                authenticationFields(method, SecurityResult.STARTED, 0),
            ),
        )
        try {
            block().also {
                logger.log(
                    LogStatements.Security.authenticationCompleted(
                        authenticationFields(method, SecurityResult.SUCCESS, elapsedSince(startedAt)),
                    ),
                )
            }
        } catch (failure: Throwable) {
            logger.log(
                LogStatements.Security.authenticationFailed(
                    failure,
                    authenticationFields(method, SecurityResult.FAILURE, elapsedSince(startedAt)),
                ),
            )
            throw failure
        }
    }

    suspend fun traceAuthentication(
        block: suspend AuthenticationTraceScope.() -> AuthenticationResult,
    ): AuthenticationResult = logger.withRootTrace(Operations.SecurityAuthentication) {
        val startedAt = elapsedRealtimeMs()
        val traceScope = AuthenticationTraceScope()
        logger.log(LogStatements.Security.authenticationStarted())
        val result = try {
            traceScope.block()
        } catch (failure: Throwable) {
            logger.log(
                LogStatements.Security.authenticationFailed(
                    failure,
                    LogStatements.Security.authenticationFields(
                        method = null,
                        result = SecurityResult.FAILURE,
                        elapsedMs = elapsedSince(startedAt),
                    ),
                ),
            )
            throw failure
        }
        val method = (result as? AuthenticationResult.Success)?.method
        val outcome = when (result) {
            is AuthenticationResult.Success -> SecurityResult.SUCCESS
            is AuthenticationResult.Cancelled -> SecurityResult.CANCELLED
            is AuthenticationResult.Failure -> SecurityResult.FAILURE
        }
        val resultFields = LogStatements.Security.authenticationFields(
            method = method?.toLogMethod(),
            result = outcome,
            elapsedMs = elapsedSince(startedAt),
        )
        val handledFailure = traceScope.failure
        if (handledFailure != null) {
            logger.log(LogStatements.Security.authenticationFailed(handledFailure, resultFields))
        } else if (result is AuthenticationResult.Failure) {
            logger.log(LogStatements.Security.authenticationRejected(resultFields))
        } else {
            logger.log(LogStatements.Security.authenticationCompleted(resultFields))
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
    ): LogFields = LogStatements.Security.lockFields(
        lockReason = reason.toReasonCode(),
        trigger = reason.toTrigger(),
        timeoutMs = timeoutMs,
        result = result,
        elapsedMs = elapsedMs,
    )

    private fun authenticationFields(
        method: AuthenticationMethod,
        result: SecurityResult,
        elapsedMs: Long,
    ): LogFields = LogStatements.Security.authenticationFields(
        method = method.toLogMethod(),
        result = result,
        elapsedMs = elapsedMs,
    )

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
