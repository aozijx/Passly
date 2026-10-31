package com.aozijx.passly.core.observability

import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.CONFLICT
import com.aozijx.passly.core.error.model.ErrorSeverity
import com.aozijx.passly.core.error.model.NOT_FOUND
import com.aozijx.passly.core.error.model.SESSION_MODE_RESTRICTED
import com.aozijx.passly.core.error.model.VALIDATION_ERROR
import com.aozijx.passly.core.observability.reporting.ErrorReportContext
import com.aozijx.passly.core.observability.sink.LogDropCounts

/** A complete, typed logging instruction. Business code does not assemble protocol fields. */
sealed interface LogStatement {
    val event: EventKey
    fun writeTo(logger: Logger)
}

sealed interface FatalLogStatement : LogStatement

private data class InfoStatement(
    override val event: EventKey,
    val fields: LogFields,
) : LogStatement {
    override fun writeTo(logger: Logger) {
        logger.info(event, fields)
    }
}

private data class DebugStatement(
    override val event: EventKey,
    val fields: LogFields,
) : LogStatement {
    override fun writeTo(logger: Logger) {
        logger.debug(event, fields)
    }
}

private data class DegradedStatement(
    override val event: EventKey,
    val errorCode: ErrorCode,
    val reason: ReasonCode,
    val fields: LogFields,
) : LogStatement {
    override fun writeTo(logger: Logger) =
        logger.degraded(event, errorCode, reason, fields)
}

private data class ErrorStatement(
    override val event: EventKey,
    val errorCode: ErrorCode,
    val reason: ReasonCode,
    val throwable: Throwable,
    val fields: LogFields,
) : LogStatement {
    override fun writeTo(logger: Logger) =
        logger.error(event, errorCode, reason, throwable, fields)
}

private data class WarningStatement(
    override val event: EventKey,
    val errorCode: ErrorCode,
    val reason: ReasonCode,
    val throwable: Throwable,
    val fields: LogFields,
) : LogStatement {
    override fun writeTo(logger: Logger) =
        logger.warn(event, errorCode, reason, throwable, fields)
}

private data class FatalStatement(
    override val event: EventKey,
    val errorCode: ErrorCode,
    val reason: ReasonCode,
    val throwable: Throwable,
    val fields: LogFields,
) : FatalLogStatement {
    override fun writeTo(logger: Logger) =
        logger.fatal(event, errorCode, reason, throwable, fields)
}

object LogStatements {
    object ApplicationError {
        fun failure(error: AppError, context: ErrorReportContext): LogStatement {
            val event = when (context.category) {
                LogCategory.APPLICATION -> Events.Application.Error
                LogCategory.DATABASE -> Events.Database.Error
                LogCategory.SECURITY -> Events.Security.Error
                LogCategory.AUTOFILL -> Events.Autofill.Error
                LogCategory.BACKUP -> Events.Backup.Error
                LogCategory.VAULT -> Events.Vault.Error
            }
            val fields = fields {
                enum(LogFieldKeys.ErrorLayer, error.layer)
                enum(LogFieldKeys.Severity, error.severity)
                boolean(LogFieldKeys.Recoverable, error.recoverable)
                operation(LogFieldKeys.Operation, context.operation)
            }
            val errorCode = ErrorCode(error.code)
            val reason = ReasonCode(error.code)
            return when (error.severity) {
                ErrorSeverity.WARNING -> WarningStatement(event, errorCode, reason, error, fields)
                ErrorSeverity.ERROR -> ErrorStatement(event, errorCode, reason, error, fields)
            }
        }
    }

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
            ErrorCodes.Application.NativeLibraryLoadFailed,
            Reasons.NativeLibraryUnavailable,
            throwable,
            LogFields.Empty,
        )

        fun unhandledException(throwable: Throwable): FatalLogStatement = FatalStatement(
            Events.Application.UnhandledException,
            ErrorCodes.Application.ApplicationUnhandledException,
            Reasons.UnknownFailure,
            throwable,
            LogFields.Empty,
        )

        fun packageIconNotFound(throwable: Throwable): LogStatement = WarningStatement(
            Events.Application.PackageIconUnavailable,
            ErrorCodes.Application.PackageIconUnavailable,
            Reasons.PackageNotInstalled,
            throwable,
            LogFields.Empty,
        )

        fun packageIconDecodeFailed(throwable: Throwable): LogStatement = ErrorStatement(
            Events.Application.PackageIconUnavailable,
            ErrorCodes.Application.PackageIconUnavailable,
            Reasons.ImageDecodeFailed,
            throwable,
            LogFields.Empty,
        )

        fun resourceCleanupFailed(throwable: Throwable): LogStatement = WarningStatement(
            Events.Application.ResourceCleanupFailed,
            ErrorCodes.Application.ResourceCleanupFailed,
            Reasons.FileDeleteFailed,
            throwable,
            LogFields.Empty,
        )

        fun deviceLockCompleted(
            trigger: Enum<*>,
            clearTask: Boolean,
        ): LogStatement = InfoStatement(
            Events.Application.DeviceLockCompleted,
            fields {
                enum(LogFieldKeys.Trigger, trigger)
                boolean(LogFieldKeys.ClearTask, clearTask)
            },
        )

        fun deviceLockSkipped(trigger: Enum<*>): LogStatement = DebugStatement(
            Events.Application.DeviceLockSkipped,
            fields { enum(LogFieldKeys.Trigger, trigger) },
        )

        fun diagnosticsExportFailed(throwable: Throwable): LogStatement = ErrorStatement(
            Events.Application.DiagnosticsExportFailed,
            ErrorCodes.Application.DiagnosticsExportFailed,
            Reasons.StorageFailure,
            throwable,
            LogFields.Empty,
        )

        fun scannedOtpInvalid(throwable: Throwable): LogStatement = WarningStatement(
            Events.Application.ScannedOtpInvalid,
            ErrorCodes.Application.ScannedOtpInvalid,
            Reasons.InvalidInput,
            throwable,
            LogFields.Empty,
        )

        fun scannerFailed(throwable: Throwable): LogStatement = ErrorStatement(
            Events.Application.ScannerFailed,
            ErrorCodes.Application.ScannerFailed,
            Reasons.ScannerProcessingFailed,
            throwable,
            LogFields.Empty,
        )

        fun logOverflow(dropped: LogDropCounts): LogStatement = InfoStatement(
            Events.Application.LogOverflow,
            fields {
                count(LogFieldKeys.DroppedDebugCount, dropped.debug)
                count(LogFieldKeys.DroppedInfoCount, dropped.info)
                count(LogFieldKeys.DroppedWarnCount, dropped.warn)
                count(LogFieldKeys.DroppedErrorCount, dropped.error)
                count(LogFieldKeys.DroppedFatalCount, dropped.fatal)
            },
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
                ErrorCodes.Database.DatabaseSessionOpenFailed,
                reason,
                throwable,
                fields,
            )

        fun sessionCloseFailed(throwable: Throwable, reason: ReasonCode, fields: LogFields): LogStatement =
            ErrorStatement(
                Events.Database.SessionTransitionFailed,
                ErrorCodes.Database.DatabaseSessionCloseFailed,
                reason,
                throwable,
                fields,
            )

        fun sessionDrainTimeout(reason: ReasonCode, fields: LogFields): LogStatement =
            DegradedStatement(
                Events.Database.SessionTransitionFailed,
                ErrorCodes.Database.DatabaseSessionCloseFailed,
                reason,
                fields,
            )

        fun opened(): LogStatement = InfoStatement(Events.Database.Opened, LogFields.Empty)

        fun openFailed(throwable: Throwable): LogStatement = ErrorStatement(
            Events.Database.OpenFailed,
            ErrorCodes.Database.DatabaseOpenFailed,
            Reasons.DatabaseOpenFailed,
            throwable,
            LogFields.Empty,
        )

        fun resetFailed(throwable: Throwable): LogStatement = ErrorStatement(
            Events.Database.ResetFailed,
            ErrorCodes.Database.DatabaseResetFailed,
            Reasons.DatabaseResetFailed,
            throwable,
            LogFields.Empty,
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
            purpose: Enum<*>?,
            result: Enum<*>,
            elapsedMs: Long,
        ): LogFields = fields {
            method?.let { enum(LogFieldKeys.AuthenticationMethod, it) }
            purpose?.let { enum(LogFieldKeys.AuthenticationPurpose, it) }
            enum(LogFieldKeys.Result, result)
            durationMs(LogFieldKeys.ElapsedMs, elapsedMs)
        }

        fun lockStarted(fields: LogFields): LogStatement =
            InfoStatement(Events.Security.LockStarted, fields)

        fun lockCompleted(fields: LogFields): LogStatement =
            InfoStatement(Events.Security.LockCompleted, fields)

        fun lockFailed(
            throwable: Throwable,
            reason: ReasonCode,
            fields: LogFields,
        ): LogStatement = ErrorStatement(
            Events.Security.LockFailed,
            ErrorCodes.Security.SecurityLockFailed,
            reason,
            throwable,
            fields,
        )

        fun lockCancelled(fields: LogFields): LogStatement =
            InfoStatement(Events.Security.LockCancelled, fields)

        fun authenticationStarted(fields: LogFields = LogFields.Empty): LogStatement =
            InfoStatement(Events.Security.AuthenticationStarted, fields)

        fun authenticationCompleted(fields: LogFields): LogStatement =
            InfoStatement(Events.Security.AuthenticationCompleted, fields)

        fun authenticationCancelled(fields: LogFields): LogStatement =
            InfoStatement(Events.Security.AuthenticationCancelled, fields)

        fun authenticationRejected(fields: LogFields): LogStatement = DegradedStatement(
            Events.Security.AuthenticationRejected,
            ErrorCodes.Security.AuthenticationFailed,
            Reasons.AuthenticationRejected,
            fields,
        )

        fun authenticationFailed(
            throwable: Throwable,
            fields: LogFields,
            reason: ReasonCode = Reasons.UnknownFailure,
        ): LogStatement = ErrorStatement(
            Events.Security.AuthenticationFailed,
            ErrorCodes.Security.AuthenticationFailed,
            reason,
            throwable,
            fields,
        )

        fun biometricPromptFailed(
            throwable: Throwable,
            reason: ReasonCode,
            degraded: Boolean = false,
        ): LogStatement = if (degraded) {
            WarningStatement(
                Events.Security.BiometricPromptFailed,
                ErrorCodes.Security.BiometricPromptFailed,
                reason,
                throwable,
                LogFields.Empty,
            )
        } else {
            ErrorStatement(
                Events.Security.BiometricPromptFailed,
                ErrorCodes.Security.BiometricPromptFailed,
                reason,
                throwable,
                LogFields.Empty,
            )
        }

        fun vaultInitialized(): LogStatement = InfoStatement(
            Events.Security.VaultInitialized,
            LogFields.Empty,
        )

        fun vaultUnlocked(): LogStatement = InfoStatement(
            Events.Security.VaultUnlocked,
            LogFields.Empty,
        )

        fun verificationTagCreated(): LogStatement = InfoStatement(
            Events.Security.VerificationTagCreated,
            LogFields.Empty,
        )

        fun envelopeRekeyed(): LogStatement = InfoStatement(
            Events.Security.EnvelopeRekeyed,
            LogFields.Empty,
        )

        fun vaultLockStarted(): LogStatement = InfoStatement(
            Events.Security.VaultLockStarted,
            LogFields.Empty,
        )

        fun vaultLocked(): LogStatement = InfoStatement(
            Events.Security.VaultLocked,
            LogFields.Empty,
        )

        fun vaultOperationFailed(
            throwable: Throwable,
            reason: ReasonCode,
            degraded: Boolean = false,
        ): LogStatement = if (degraded) {
            WarningStatement(
                Events.Security.VaultOperationFailed,
                ErrorCodes.Security.VaultOperationFailed,
                reason,
                throwable,
                LogFields.Empty,
            )
        } else {
            ErrorStatement(
                Events.Security.VaultOperationFailed,
                ErrorCodes.Security.VaultOperationFailed,
                reason,
                throwable,
                LogFields.Empty,
            )
        }
    }

    object Autofill {
        fun presentationSelected(mode: Enum<*>, inlineAvailable: Boolean): LogStatement =
            InfoStatement(
                Events.Autofill.PresentationSelected,
                fields {
                    enum(LogFieldKeys.Presentation, mode)
                    boolean(LogFieldKeys.InlineAvailable, inlineAvailable)
                },
            )

        fun fallbackStarted(fields: LogFields): LogStatement =
            InfoStatement(Events.Autofill.FallbackStarted, fields)

        fun fallbackUnavailable(reason: ReasonCode, fields: LogFields = LogFields.Empty): LogStatement =
            DegradedStatement(
                Events.Autofill.FallbackUnavailable,
                ErrorCodes.Autofill.AutofillDiscoveryUnavailable,
                reason,
                fields,
            )

        fun fallbackDelivered(fields: LogFields): LogStatement =
            InfoStatement(Events.Autofill.FallbackDelivered, fields)

        fun fallbackFailed(throwable: Throwable, fields: LogFields = LogFields.Empty): LogStatement =
            ErrorStatement(
                Events.Autofill.FallbackFailed,
                ErrorCodes.Autofill.AutofillResultDeliveryFailed,
                Reasons.PlatformCallbackFailed,
                throwable,
                fields,
            )

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

        fun discoveryCompleted(
            elapsedMs: Long,
            fields: LogFields = LogFields.Empty,
        ): LogStatement = InfoStatement(
            Events.Autofill.DiscoveryCompleted,
            fields.withElapsed(elapsedMs),
        )

        fun discoveryUnavailable(
            reason: ReasonCode,
            fields: LogFields = LogFields.Empty,
        ): LogStatement = DegradedStatement(
            Events.Autofill.DiscoveryUnavailable,
            ErrorCodes.Autofill.AutofillDiscoveryUnavailable,
            reason,
            fields,
        )

        fun discoveryFailed(
            throwable: Throwable,
            reason: ReasonCode = Reasons.UnknownFailure,
            fields: LogFields = LogFields.Empty,
        ): LogStatement = ErrorStatement(
            Events.Autofill.DiscoveryUnavailable,
            ErrorCodes.Autofill.AutofillDiscoveryUnavailable,
            reason,
            throwable,
            fields,
        )

        fun authenticationStarted(): LogStatement =
            InfoStatement(Events.Autofill.AuthenticationStarted, LogFields.Empty)

        fun authenticationCompleted(elapsedMs: Long): LogStatement = InfoStatement(
            Events.Autofill.AuthenticationCompleted,
            LogFields.Empty.withElapsed(elapsedMs),
        )

        fun resultDeliveryStarted(): LogStatement =
            InfoStatement(Events.Autofill.ResultDeliveryStarted, LogFields.Empty)

        fun resultDelivered(elapsedMs: Long): LogStatement = InfoStatement(
            Events.Autofill.ResultDelivered,
            LogFields.Empty.withElapsed(elapsedMs),
        )

        fun resultDeliveryFailed(throwable: Throwable, elapsedMs: Long): LogStatement = ErrorStatement(
            Events.Autofill.ResultDeliveryFailed,
            ErrorCodes.Autofill.AutofillResultDeliveryFailed,
            Reasons.PlatformCallbackFailed,
            throwable,
            LogFields.Empty.withElapsed(elapsedMs),
        )

        fun sessionCloseStarted(): LogStatement =
            InfoStatement(Events.Autofill.SessionCloseStarted, LogFields.Empty)

        fun sessionClosed(elapsedMs: Long): LogStatement = InfoStatement(
            Events.Autofill.SessionClosed,
            LogFields.Empty.withElapsed(elapsedMs),
        )

        fun sessionCloseFailed(throwable: Throwable, elapsedMs: Long): LogStatement = ErrorStatement(
            Events.Autofill.SessionCloseFailed,
            ErrorCodes.Autofill.AutofillSessionCloseFailed,
            Reasons.SessionCloseFailed,
            throwable,
            LogFields.Empty.withElapsed(elapsedMs),
        )

        fun saveStarted(): LogStatement =
            InfoStatement(Events.Autofill.SaveStarted, LogFields.Empty)

        fun saveStaged(): LogStatement =
            InfoStatement(Events.Autofill.SaveStaged, LogFields.Empty)

        fun saveCompleted(): LogStatement =
            InfoStatement(Events.Autofill.SaveCompleted, LogFields.Empty)

        fun saveUnavailable(reason: ReasonCode): LogStatement = DegradedStatement(
            Events.Autofill.SaveFailed,
            ErrorCodes.Autofill.AutofillSaveFailed,
            reason,
            LogFields.Empty,
        )

        fun saveFailed(
            throwable: Throwable,
            reason: ReasonCode = Reasons.StorageFailure,
        ): LogStatement = ErrorStatement(
            Events.Autofill.SaveFailed,
            ErrorCodes.Autofill.AutofillSaveFailed,
            reason,
            throwable,
            LogFields.Empty,
        )

        fun actionIgnored(): LogStatement =
            DebugStatement(Events.Autofill.ActionIgnored, LogFields.Empty)
    }

    object Backup {
        fun resourceCleanupFailed(throwable: Throwable): LogStatement = WarningStatement(
            Events.Backup.ResourceCleanupFailed,
            ErrorCodes.Application.ResourceCleanupFailed,
            Reasons.FileDeleteFailed,
            throwable,
            LogFields.Empty,
        )
    }

    object Vault {
        fun entryCreateStarted(entryType: Enum<*>, trigger: Enum<*>): LogStatement = InfoStatement(
            Events.Vault.EntryCreateStarted,
            entryFields(entryType, trigger),
        )

        fun entryCreateCompleted(
            entryType: Enum<*>,
            trigger: Enum<*>,
            elapsedMs: Long,
        ): LogStatement = InfoStatement(
            Events.Vault.EntryCreateCompleted,
            entryFields(entryType, trigger).withElapsed(elapsedMs),
        )

        fun entryCreateFailed(
            error: AppError,
            entryType: Enum<*>,
            trigger: Enum<*>,
            elapsedMs: Long,
        ): LogStatement {
            val reason = failureReason(error)
            val fields = entryFields(entryType, trigger).withElapsed(elapsedMs)
            return when (error.severity) {
                ErrorSeverity.WARNING -> WarningStatement(
                    Events.Vault.EntryCreateFailed,
                    ErrorCodes.Vault.VaultEntryCreateFailed,
                    reason,
                    error,
                    fields,
                )
                ErrorSeverity.ERROR -> ErrorStatement(
                    Events.Vault.EntryCreateFailed,
                    ErrorCodes.Vault.VaultEntryCreateFailed,
                    reason,
                    error,
                    fields,
                )
            }
        }

        fun entryCreateFailed(
            throwable: Throwable,
            entryType: Enum<*>,
            trigger: Enum<*>,
            elapsedMs: Long,
        ): LogStatement = ErrorStatement(
            Events.Vault.EntryCreateFailed,
            ErrorCodes.Vault.VaultEntryCreateFailed,
            Reasons.UnknownFailure,
            throwable,
            entryFields(entryType, trigger).withElapsed(elapsedMs),
        )

        fun entryUpdateStarted(): LogStatement =
            InfoStatement(Events.Vault.EntryUpdateStarted, LogFields.Empty)

        fun entryUpdateCompleted(entryType: Enum<*>?, elapsedMs: Long): LogStatement = InfoStatement(
            Events.Vault.EntryUpdateCompleted,
            entryTypeFields(entryType).withElapsed(elapsedMs),
        )

        fun entryUpdateFailed(
            error: AppError,
            entryType: Enum<*>?,
            elapsedMs: Long,
        ): LogStatement {
            val fields = entryTypeFields(entryType).withElapsed(elapsedMs)
            return when (error.severity) {
                ErrorSeverity.WARNING -> WarningStatement(
                    Events.Vault.EntryUpdateFailed,
                    ErrorCodes.Vault.VaultEntryUpdateFailed,
                    failureReason(error),
                    error,
                    fields,
                )
                ErrorSeverity.ERROR -> ErrorStatement(
                    Events.Vault.EntryUpdateFailed,
                    ErrorCodes.Vault.VaultEntryUpdateFailed,
                    failureReason(error),
                    error,
                    fields,
                )
            }
        }

        fun entryUpdateFailed(
            throwable: Throwable,
            entryType: Enum<*>?,
            elapsedMs: Long,
        ): LogStatement = ErrorStatement(
            Events.Vault.EntryUpdateFailed,
            ErrorCodes.Vault.VaultEntryUpdateFailed,
            Reasons.UnknownFailure,
            throwable,
            entryTypeFields(entryType).withElapsed(elapsedMs),
        )

        private fun entryFields(entryType: Enum<*>, trigger: Enum<*>): LogFields = fields {
            enum(LogFieldKeys.EntryType, entryType)
            enum(LogFieldKeys.Trigger, trigger)
        }

        private fun entryTypeFields(entryType: Enum<*>?): LogFields = fields {
            entryType?.let { enum(LogFieldKeys.EntryType, it) }
        }

        private fun failureReason(error: AppError): ReasonCode = when (error.code) {
            SESSION_MODE_RESTRICTED -> Reasons.SessionRestricted
            VALIDATION_ERROR -> Reasons.InvalidInput
            NOT_FOUND -> Reasons.NotFound
            CONFLICT -> Reasons.Conflict
            else -> Reasons.UnknownFailure
        }
    }

    private fun LogFields.withElapsed(elapsedMs: Long): LogFields =
        withDuration(LogFieldKeys.ElapsedMs, elapsedMs.coerceAtLeast(0))
}
