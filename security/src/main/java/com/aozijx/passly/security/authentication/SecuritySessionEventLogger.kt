package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class SecuritySessionEventLogger @Inject internal constructor(
    private val logger: Logger,
) {
    suspend fun <T> traceLock(
        request: SessionLockRequest,
        timeoutMs: Long,
        block: suspend LockTraceScope.() -> T,
    ): T = logger.inSpan(Operations.SecurityLock) {
        val startedAt = elapsedRealtimeMs()
        val traceScope = LockTraceScope()
        logger.log(
            LogStatements.Security.lockStarted(
                lockFields(request, timeoutMs, SecurityResult.STARTED, 0),
            ),
        )
        try {
            traceScope.block().also {
                val handledFailure = traceScope.failure
                if (handledFailure != null) {
                    logger.log(
                        LogStatements.Security.lockFailed(
                            handledFailure.throwable,
                            handledFailure.reason,
                            lockFields(
                                request,
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
                                request,
                                timeoutMs,
                                SecurityResult.SUCCESS,
                                elapsedSince(startedAt),
                            ),
                        ),
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            logger.log(
                LogStatements.Security.lockCancelled(
                    lockFields(
                        request,
                        timeoutMs,
                        SecurityResult.CANCELLED,
                        elapsedSince(startedAt),
                    ),
                ),
            )
            throw cancelled
        } catch (failure: Throwable) {
            logger.log(
                LogStatements.Security.lockFailed(
                    failure,
                    Reasons.UnknownFailure,
                    lockFields(
                        request,
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
        purpose: AuthenticationPurpose,
        method: AuthenticationMethod,
        block: suspend () -> T,
    ): T = logger.inSpan(Operations.SecurityAuthentication) {
        val startedAt = elapsedRealtimeMs()
        logger.log(
            LogStatements.Security.authenticationStarted(
                authenticationFields(purpose, method, SecurityResult.STARTED, 0),
            ),
        )
        try {
            block().also {
                logger.log(
                    LogStatements.Security.authenticationCompleted(
                        authenticationFields(purpose, method, SecurityResult.SUCCESS, elapsedSince(startedAt)),
                    ),
                )
            }
        } catch (cancelled: CancellationException) {
            logger.log(
                LogStatements.Security.authenticationCancelled(
                    authenticationFields(
                        purpose,
                        method,
                        SecurityResult.CANCELLED,
                        elapsedSince(startedAt),
                    ),
                ),
            )
            throw cancelled
        } catch (failure: Throwable) {
            logger.log(
                LogStatements.Security.authenticationFailed(
                    failure,
                    authenticationFields(purpose, method, SecurityResult.FAILURE, elapsedSince(startedAt)),
                ),
            )
            throw failure
        }
    }

    suspend fun traceAuthentication(
        purpose: AuthenticationPurpose,
        block: suspend AuthenticationTraceScope.() -> AuthenticationResult,
    ): AuthenticationResult = logger.inSpan(Operations.SecurityAuthentication) {
        val startedAt = elapsedRealtimeMs()
        val traceScope = AuthenticationTraceScope()
        logger.log(
            LogStatements.Security.authenticationStarted(
                authenticationFields(purpose, null, SecurityResult.STARTED, 0),
            ),
        )
        val result = try {
            traceScope.block()
        } catch (cancelled: CancellationException) {
            logger.log(
                LogStatements.Security.authenticationCancelled(
                    LogStatements.Security.authenticationFields(
                        method = null,
                        purpose = purpose.toLogPurpose(),
                        result = SecurityResult.CANCELLED,
                        elapsedMs = elapsedSince(startedAt),
                    ),
                ),
            )
            throw cancelled
        } catch (failure: Throwable) {
            logger.log(
                LogStatements.Security.authenticationFailed(
                    failure,
                    LogStatements.Security.authenticationFields(
                        method = null,
                        purpose = purpose.toLogPurpose(),
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
            purpose = purpose.toLogPurpose(),
            result = outcome,
            elapsedMs = elapsedSince(startedAt),
        )
        val handledFailure = traceScope.failure
        if (handledFailure != null) {
            logger.log(LogStatements.Security.authenticationFailed(handledFailure, resultFields))
        } else {
            when (result) {
                is AuthenticationResult.Success ->
                    logger.log(LogStatements.Security.authenticationCompleted(resultFields))
                is AuthenticationResult.Cancelled ->
                    logger.log(LogStatements.Security.authenticationCancelled(resultFields))
                is AuthenticationResult.Failure ->
                    logger.log(LogStatements.Security.authenticationRejected(resultFields))
            }
        }
        result
    }

    class LockTraceScope internal constructor() {
        internal var failure: LockFailure? = null
            private set

        fun recordFailure(failure: Throwable, reason: ReasonCode) {
            if (this.failure == null) this.failure = LockFailure(failure, reason)
        }
    }

    internal data class LockFailure(
        val throwable: Throwable,
        val reason: ReasonCode,
    )

    class AuthenticationTraceScope internal constructor() {
        internal var failure: Throwable? = null
            private set

        fun recordFailure(failure: Throwable) {
            if (this.failure == null) this.failure = failure
        }
    }

    private fun lockFields(
        request: SessionLockRequest,
        timeoutMs: Long,
        result: SecurityResult,
        elapsedMs: Long,
    ): LogFields = LogStatements.Security.lockFields(
        lockReason = request.reason.toReasonCode(),
        trigger = request.trigger.toLogTrigger(),
        timeoutMs = timeoutMs,
        result = result,
        elapsedMs = elapsedMs,
    )

    private fun authenticationFields(
        purpose: AuthenticationPurpose,
        method: AuthenticationMethod?,
        result: SecurityResult,
        elapsedMs: Long,
    ): LogFields = LogStatements.Security.authenticationFields(
        method = method?.toLogMethod(),
        purpose = purpose.toLogPurpose(),
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

    private fun LockTrigger.toLogTrigger(): SecurityTrigger = when (this) {
        LockTrigger.USER_ACTION -> SecurityTrigger.USER_ACTION
        LockTrigger.IDLE_TIMER -> SecurityTrigger.IDLE_TIMER
        LockTrigger.AUTOFILL_COMPLETION -> SecurityTrigger.AUTOFILL_COMPLETION
        LockTrigger.APP_BACKGROUND -> SecurityTrigger.APP_BACKGROUND
        LockTrigger.SCREEN_OFF -> SecurityTrigger.SCREEN_OFF
        LockTrigger.DEVICE_FLIP -> SecurityTrigger.DEVICE_FLIP
        LockTrigger.RECOVERY_EXIT -> SecurityTrigger.RECOVERY_EXIT
        LockTrigger.INTEGRITY_FAILURE -> SecurityTrigger.INTEGRITY_FAILURE
        LockTrigger.APP_EXIT -> SecurityTrigger.APP_EXIT
    }

    private fun AuthenticationMethod.toLogMethod(): AuthenticationMethodLog = when (this) {
        AuthenticationMethod.BIOMETRIC -> AuthenticationMethodLog.BIOMETRIC
        AuthenticationMethod.APP_PASSWORD -> AuthenticationMethodLog.MASTER_PASSWORD
        AuthenticationMethod.RECOVERY_CODE -> AuthenticationMethodLog.RECOVERY
    }

    private fun AuthenticationPurpose.toLogPurpose(): AuthenticationPurposeLog = when (this) {
        AuthenticationPurpose.UNLOCK_VAULT -> AuthenticationPurposeLog.VAULT_UNLOCK
        AuthenticationPurpose.REAUTHENTICATE -> AuthenticationPurposeLog.REAUTHENTICATION
        AuthenticationPurpose.AUTOFILL -> AuthenticationPurposeLog.AUTOFILL
        AuthenticationPurpose.REVEAL_SECRET -> AuthenticationPurposeLog.SECRET_REVEAL
        AuthenticationPurpose.REVEAL_HIGH_SENSITIVITY_SECRET -> AuthenticationPurposeLog.HIGH_SENSITIVITY_REVEAL
        AuthenticationPurpose.COPY_SECRET -> AuthenticationPurposeLog.SECRET_COPY
        AuthenticationPurpose.DELETE_ENTRY -> AuthenticationPurposeLog.ENTRY_DELETE
        AuthenticationPurpose.BACKUP_EXPORT -> AuthenticationPurposeLog.BACKUP_EXPORT
        AuthenticationPurpose.BACKUP_IMPORT -> AuthenticationPurposeLog.BACKUP_IMPORT
        AuthenticationPurpose.MANAGE_APP_PASSWORD -> AuthenticationPurposeLog.APP_PASSWORD_MANAGEMENT
        AuthenticationPurpose.MANAGE_RECOVERY_CODE -> AuthenticationPurposeLog.RECOVERY_CODE_MANAGEMENT
        AuthenticationPurpose.RECOVER_AUTH_METHODS -> AuthenticationPurposeLog.AUTH_METHOD_RECOVERY
        AuthenticationPurpose.CHANGE_BIOMETRIC_POLICY -> AuthenticationPurposeLog.BIOMETRIC_POLICY_CHANGE
        AuthenticationPurpose.EXPORT_DIAGNOSTICS -> AuthenticationPurposeLog.DIAGNOSTICS_EXPORT
        AuthenticationPurpose.CLEAR_DATABASE -> AuthenticationPurposeLog.DATABASE_CLEAR
    }

    private fun elapsedRealtimeMs(): Long = System.nanoTime() / NANOS_PER_MILLISECOND
    private fun elapsedSince(startedAt: Long): Long = (elapsedRealtimeMs() - startedAt).coerceAtLeast(0)

    private enum class SecurityTrigger {
        USER_ACTION,
        IDLE_TIMER,
        AUTOFILL_COMPLETION,
        APP_BACKGROUND,
        SCREEN_OFF,
        DEVICE_FLIP,
        RECOVERY_EXIT,
        INTEGRITY_FAILURE,
        APP_EXIT,
    }
    private enum class SecurityResult { STARTED, SUCCESS, FAILURE, CANCELLED }
    private enum class AuthenticationMethodLog { BIOMETRIC, MASTER_PASSWORD, RECOVERY }
    private enum class AuthenticationPurposeLog {
        VAULT_UNLOCK,
        REAUTHENTICATION,
        AUTOFILL,
        SECRET_REVEAL,
        HIGH_SENSITIVITY_REVEAL,
        SECRET_COPY,
        ENTRY_DELETE,
        BACKUP_EXPORT,
        BACKUP_IMPORT,
        APP_PASSWORD_MANAGEMENT,
        RECOVERY_CODE_MANAGEMENT,
        AUTH_METHOD_RECOVERY,
        BIOMETRIC_POLICY_CHANGE,
        DIAGNOSTICS_EXPORT,
        DATABASE_CLEAR,
    }

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
