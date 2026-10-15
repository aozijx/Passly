package com.aozijx.passly.core.observability

/** A complete, typed logging instruction. Business code does not assemble protocol fields. */
sealed interface LogStatement {
    val event: EventKey
    fun writeTo(logger: Logger): DiagnosticId?
}

sealed interface FatalLogStatement : LogStatement

private data class InfoStatement(
    override val event: EventKey,
    val fields: LogFields,
) : LogStatement {
    override fun writeTo(logger: Logger): DiagnosticId? {
        logger.info(event, fields)
        return null
    }
}

private data class DebugStatement(
    override val event: EventKey,
    val fields: LogFields,
) : LogStatement {
    override fun writeTo(logger: Logger): DiagnosticId? {
        logger.debug(event, fields)
        return null
    }
}

private data class DegradedStatement(
    override val event: EventKey,
    val errorCode: ErrorCode,
    val reason: ReasonCode,
    val fields: LogFields,
) : LogStatement {
    override fun writeTo(logger: Logger): DiagnosticId =
        logger.degraded(event, errorCode, reason, fields)
}

private data class ErrorStatement(
    override val event: EventKey,
    val errorCode: ErrorCode,
    val reason: ReasonCode,
    val throwable: Throwable,
    val fields: LogFields,
) : LogStatement {
    override fun writeTo(logger: Logger): DiagnosticId =
        logger.error(event, errorCode, reason, throwable, fields)
}

private data class WarningStatement(
    override val event: EventKey,
    val errorCode: ErrorCode,
    val reason: ReasonCode,
    val throwable: Throwable,
    val fields: LogFields,
) : LogStatement {
    override fun writeTo(logger: Logger): DiagnosticId =
        logger.warn(event, errorCode, reason, throwable, fields)
}

private data class FatalStatement(
    override val event: EventKey,
    val errorCode: ErrorCode,
    val reason: ReasonCode,
    val throwable: Throwable,
    val fields: LogFields,
) : FatalLogStatement {
    override fun writeTo(logger: Logger): DiagnosticId =
        logger.fatal(event, errorCode, reason, throwable, fields)
}

object LogStatements {
    object Application {
        fun traceResumeUnavailable(): LogStatement =
            DebugStatement(Events.Application.TraceResumeUnavailable, LogFields.Empty)

        fun previousProcessExit(
            exitReason: Enum<*>,
            status: Int?,
            importance: Enum<*>,
            pssKb: Long,
            rssKb: Long,
        ): LogStatement = InfoStatement(
            Events.Application.PreviousProcessExit,
            fields {
                enum(LogFieldKeys.ExitReason, exitReason)
                status?.takeIf { it >= 0 }?.let { count(LogFieldKeys.ExitStatus, it) }
                enum(LogFieldKeys.Importance, importance)
                count(LogFieldKeys.PssKb, pssKb)
                count(LogFieldKeys.RssKb, rssKb)
            },
        )

        fun backgroundLockSkipped(): LogStatement =
            DebugStatement(Events.Application.BackgroundLockSkipped, LogFields.Empty)

        fun nativeLibraryLoadFailed(throwable: Throwable): LogStatement = ErrorStatement(
            Events.Application.NativeLibraryLoadFailed,
            ErrorCodes.NativeLibraryLoadFailed,
            Reasons.NativeLibraryUnavailable,
            throwable,
            LogFields.Empty,
        )

        fun unhandledException(throwable: Throwable): FatalLogStatement = FatalStatement(
            Events.Application.UnhandledException,
            ErrorCodes.ApplicationUnhandledException,
            Reasons.UnknownFailure,
            throwable,
            LogFields.Empty,
        )
    }

    object Database {
        fun sessionFields(
            fromState: Enum<*>,
            toState: Enum<*>,
            lockType: Enum<*>,
            reason: ReasonCode,
            owner: Enum<*>,
            elapsedMs: Long,
            timeoutMs: Long?,
            activeLeaseCount: Int,
        ): LogFields = fields {
            enum(LogFieldKeys.FromState, fromState)
            enum(LogFieldKeys.ToState, toState)
            enum(LogFieldKeys.LockType, lockType)
            reason(LogFieldKeys.Reason, reason)
            enum(LogFieldKeys.Owner, owner)
            durationMs(LogFieldKeys.ElapsedMs, elapsedMs)
            timeoutMs?.let { durationMs(LogFieldKeys.TimeoutMs, it) }
            if (activeLeaseCount > 0) count(LogFieldKeys.ActiveLeaseCount, activeLeaseCount)
        }

        fun sessionTransition(fields: LogFields): LogStatement =
            InfoStatement(Events.Database.SessionTransition, fields)

        fun sessionOpenFailed(throwable: Throwable, reason: ReasonCode, fields: LogFields): LogStatement =
            ErrorStatement(
                Events.Database.SessionTransitionFailed,
                ErrorCodes.DatabaseSessionOpenFailed,
                reason,
                throwable,
                fields,
            )

        fun sessionCloseFailed(throwable: Throwable, reason: ReasonCode, fields: LogFields): LogStatement =
            ErrorStatement(
                Events.Database.SessionTransitionFailed,
                ErrorCodes.DatabaseSessionCloseFailed,
                reason,
                throwable,
                fields,
            )

        fun sessionDrainTimeout(reason: ReasonCode, fields: LogFields): LogStatement =
            DegradedStatement(
                Events.Database.SessionTransitionFailed,
                ErrorCodes.DatabaseSessionCloseFailed,
                reason,
                fields,
            )
    }

    object Security {
        fun lockFields(
            lockReason: ReasonCode,
            trigger: Enum<*>,
            timeoutMs: Long,
            result: Enum<*>,
            elapsedMs: Long,
        ): LogFields = fields {
            reason(LogFieldKeys.LockReason, lockReason)
            enum(LogFieldKeys.Trigger, trigger)
            durationMs(LogFieldKeys.TimeoutMs, timeoutMs)
            enum(LogFieldKeys.Result, result)
            durationMs(LogFieldKeys.ElapsedMs, elapsedMs)
        }

        fun authenticationFields(
            method: Enum<*>?,
            result: Enum<*>,
            elapsedMs: Long,
        ): LogFields = fields {
            method?.let { enum(LogFieldKeys.AuthenticationMethod, it) }
            enum(LogFieldKeys.Result, result)
            durationMs(LogFieldKeys.ElapsedMs, elapsedMs)
        }

        fun lockStarted(fields: LogFields): LogStatement =
            InfoStatement(Events.Security.LockStarted, fields)

        fun lockCompleted(fields: LogFields): LogStatement =
            InfoStatement(Events.Security.LockCompleted, fields)

        fun lockFailed(throwable: Throwable, fields: LogFields): LogStatement = ErrorStatement(
            Events.Security.LockCompleted,
            ErrorCodes.SecurityLockFailed,
            Reasons.SecurityLockOperationFailed,
            throwable,
            fields,
        )

        fun authenticationStarted(fields: LogFields = LogFields.Empty): LogStatement =
            InfoStatement(Events.Security.UnlockStarted, fields)

        fun authenticationCompleted(fields: LogFields): LogStatement =
            InfoStatement(Events.Security.UnlockCompleted, fields)

        fun authenticationRejected(fields: LogFields): LogStatement = DegradedStatement(
            Events.Security.AuthenticationFailed,
            ErrorCodes.AuthenticationFailed,
            Reasons.AuthenticationRejected,
            fields,
        )

        fun authenticationFailed(
            throwable: Throwable,
            fields: LogFields,
            reason: ReasonCode = Reasons.UnknownFailure,
        ): LogStatement = ErrorStatement(
            Events.Security.AuthenticationFailed,
            ErrorCodes.AuthenticationFailed,
            reason,
            throwable,
            fields,
        )
    }

    object Autofill {
        fun context(source: Enum<*>, targetPackage: String?): LogFields = fields {
            enum(LogFieldKeys.Source, source)
            if (!targetPackage.isNullOrBlank()) {
                hashedIdentifier(
                    LogFieldKeys.TargetPackage,
                    IdentifierNamespace.TARGET_PACKAGE,
                    targetPackage,
                )
            }
        }

        fun discoveryStarted(fields: LogFields = LogFields.Empty): LogStatement =
            InfoStatement(Events.Autofill.DiscoveryStarted, fields)

        fun discoveryCompleted(fields: LogFields = LogFields.Empty): LogStatement =
            InfoStatement(Events.Autofill.DiscoveryCompleted, fields)

        fun discoveryUnavailable(
            reason: ReasonCode,
            fields: LogFields = LogFields.Empty,
        ): LogStatement = DegradedStatement(
            Events.Autofill.DiscoveryUnavailable,
            ErrorCodes.AutofillDiscoveryUnavailable,
            reason,
            fields,
        )

        fun discoveryFailed(
            throwable: Throwable,
            reason: ReasonCode = Reasons.UnknownFailure,
            fields: LogFields = LogFields.Empty,
        ): LogStatement = ErrorStatement(
            Events.Autofill.DiscoveryUnavailable,
            ErrorCodes.AutofillDiscoveryUnavailable,
            reason,
            throwable,
            fields,
        )

        fun authenticationStarted(): LogStatement =
            InfoStatement(Events.Autofill.AuthenticationStarted, LogFields.Empty)

        fun authenticationCompleted(): LogStatement =
            InfoStatement(Events.Autofill.AuthenticationCompleted, LogFields.Empty)

        fun resultDeliveryStarted(): LogStatement =
            InfoStatement(Events.Autofill.ResultDeliveryStarted, LogFields.Empty)

        fun resultDelivered(): LogStatement =
            InfoStatement(Events.Autofill.ResultDelivered, LogFields.Empty)

        fun resultDeliveryFailed(throwable: Throwable): LogStatement = ErrorStatement(
            Events.Autofill.ResultDeliveryFailed,
            ErrorCodes.AutofillResultDeliveryFailed,
            Reasons.PlatformCallbackFailed,
            throwable,
            LogFields.Empty,
        )

        fun sessionCloseStarted(): LogStatement =
            InfoStatement(Events.Autofill.SessionCloseStarted, LogFields.Empty)

        fun sessionClosed(): LogStatement =
            InfoStatement(Events.Autofill.SessionClosed, LogFields.Empty)

        fun sessionCloseFailed(throwable: Throwable): LogStatement = ErrorStatement(
            Events.Autofill.SessionCloseFailed,
            ErrorCodes.AutofillSessionCloseFailed,
            Reasons.SessionCloseFailed,
            throwable,
            LogFields.Empty,
        )

        fun saveStarted(): LogStatement =
            InfoStatement(Events.Autofill.SaveStarted, LogFields.Empty)

        fun saveStaged(): LogStatement =
            InfoStatement(Events.Autofill.SaveStaged, LogFields.Empty)

        fun saveCompleted(): LogStatement =
            InfoStatement(Events.Autofill.SaveCompleted, LogFields.Empty)

        fun saveUnavailable(reason: ReasonCode): LogStatement = DegradedStatement(
            Events.Autofill.SaveFailed,
            ErrorCodes.AutofillSaveFailed,
            reason,
            LogFields.Empty,
        )

        fun saveFailed(
            throwable: Throwable,
            reason: ReasonCode = Reasons.StorageFailure,
        ): LogStatement = ErrorStatement(
            Events.Autofill.SaveFailed,
            ErrorCodes.AutofillSaveFailed,
            reason,
            throwable,
            LogFields.Empty,
        )

        fun actionIgnored(): LogStatement =
            DebugStatement(Events.Autofill.ActionIgnored, LogFields.Empty)
    }
}
