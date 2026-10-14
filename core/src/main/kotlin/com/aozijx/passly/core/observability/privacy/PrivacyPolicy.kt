package com.aozijx.passly.core.observability.privacy

import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.ErrorPayload
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.IdentifierNamespace
import com.aozijx.passly.core.observability.LogFieldKey
import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.PendingLogEvent
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SecureSessionId
import com.aozijx.passly.core.observability.TraceSnapshot
import com.aozijx.passly.core.observability.category

data class SanitizationContext(
    val eventId: EventId,
    val timestampMs: Long,
    val elapsedRealtimeMs: Long,
    val processSessionId: ProcessSessionId,
    val secureSessionId: SecureSessionId?,
    val trace: TraceSnapshot,
)

enum class ContractViolationBehavior { THROW, REJECT }

enum class ContractViolation {
    EVENT_NOT_REGISTERED,
    FIELD_NOT_REGISTERED,
    FIELD_TYPE_MISMATCH,
    INVALID_FIELD_VALUE,
    INVALID_IDENTIFIER_VALUE,
    PREHASHED_IDENTIFIER_NOT_ALLOWED,
    INVALID_ERROR_CONTRACT,
}

class LogContractViolation internal constructor(
    val violation: ContractViolation,
) : IllegalArgumentException("Log contract violation: ${violation.name}")

sealed interface SanitizationResult {
    data class Accepted(
        val record: SanitizedLogRecord,
        val droppedFieldCount: Int,
    ) : SanitizationResult

    data class Rejected(val violation: ContractViolation) : SanitizationResult
}

fun interface LogSanitizer {
    fun sanitize(event: PendingLogEvent, context: SanitizationContext): SanitizationResult
}

class PrivacyPolicy(
    private val identifierHasher: IdentifierHasher,
    private val violationBehavior: ContractViolationBehavior,
    private val exceptionSnapshotFactory: ExceptionSnapshotFactory = ExceptionSnapshotFactory(),
) : LogSanitizer {
    override fun sanitize(
        event: PendingLogEvent,
        context: SanitizationContext,
    ): SanitizationResult {
        if (event.event !in Events.All) return violation(ContractViolation.EVENT_NOT_REGISTERED)
        validateErrorContract(event)?.let { return violation(it) }

        var droppedFieldCount = 0
        val safeFields = linkedMapOf<LogFieldKey, LogValue>()
        for ((key, value) in event.fields.values) {
            val expectedKind = SAFE_FIELD_CATALOG[key]
                ?: return violation(ContractViolation.FIELD_NOT_REGISTERED)
            if (kindOf(value) != expectedKind) {
                return violation(
                    if (value is LogValue.HashedIdentifierValue) {
                        ContractViolation.PREHASHED_IDENTIFIER_NOT_ALLOWED
                    } else {
                        ContractViolation.FIELD_TYPE_MISMATCH
                    },
                )
            }

            when (value) {
                is LogValue.Count -> {
                    if (value.value < 0) return violation(ContractViolation.INVALID_FIELD_VALUE)
                    safeFields[key] = value
                }
                is LogValue.DurationMs -> {
                    if (value.value < 0) return violation(ContractViolation.INVALID_FIELD_VALUE)
                    safeFields[key] = value
                }
                is LogValue.Ratio -> {
                    if (!value.value.isFinite() || value.value !in 0.0..1.0) {
                        return violation(ContractViolation.INVALID_FIELD_VALUE)
                    }
                    safeFields[key] = value
                }
                is LogValue.HashedIdentifierInput -> {
                    if (value.namespace != expectedKind.identifierNamespace) {
                        return violation(ContractViolation.FIELD_TYPE_MISMATCH)
                    }
                    if (!isValidIdentifierInput(value.rawValue)) {
                        return violation(ContractViolation.INVALID_IDENTIFIER_VALUE)
                    }
                    val hashed = try {
                        identifierHasher.hash(value.namespace, value.rawValue)
                    } catch (_: Exception) {
                        null
                    }
                    if (hashed == null) {
                        droppedFieldCount += 1
                    } else {
                        safeFields[key] = LogValue.HashedIdentifierValue(hashed)
                    }
                }
                is LogValue.HashedIdentifierValue ->
                    return violation(ContractViolation.PREHASHED_IDENTIFIER_NOT_ALLOWED)
                else -> safeFields[key] = value
            }
        }

        val pendingError = event.error
        val safeError = pendingError?.let {
            ErrorPayload(
                errorCode = it.errorCode,
                reason = it.reason,
                diagnosticId = it.diagnosticId,
                exception = it.throwable?.let(exceptionSnapshotFactory::create),
            )
        }
        return SanitizationResult.Accepted(
            record = SanitizedLogRecord(
                eventId = context.eventId,
                timestampMs = context.timestampMs,
                elapsedRealtimeMs = context.elapsedRealtimeMs,
                level = event.level,
                category = event.event.category(),
                event = event.event,
                processSessionId = context.processSessionId,
                secureSessionId = context.secureSessionId,
                trace = context.trace,
                fields = LogFields.sanitized(safeFields),
                error = safeError,
            ),
            droppedFieldCount = droppedFieldCount,
        )
    }

    private fun validateErrorContract(event: PendingLogEvent): ContractViolation? = when (event.level) {
        LogLevel.DEBUG, LogLevel.INFO ->
            ContractViolation.INVALID_ERROR_CONTRACT.takeIf { event.error != null }
        LogLevel.WARN ->
            ContractViolation.INVALID_ERROR_CONTRACT.takeIf { event.error == null }
        LogLevel.ERROR, LogLevel.FATAL ->
            ContractViolation.INVALID_ERROR_CONTRACT.takeIf {
                event.error == null || event.error.throwable == null
            }
    }

    private fun violation(violation: ContractViolation): SanitizationResult = when (violationBehavior) {
        ContractViolationBehavior.THROW -> throw LogContractViolation(violation)
        ContractViolationBehavior.REJECT -> SanitizationResult.Rejected(violation)
    }

    private fun kindOf(value: LogValue): FieldKind = when (value) {
        is LogValue.Count -> FieldKind.COUNT
        is LogValue.DurationMs -> FieldKind.DURATION
        is LogValue.Ratio -> FieldKind.RATIO
        is LogValue.BooleanValue -> FieldKind.BOOLEAN
        is LogValue.EnumValue -> FieldKind.ENUM
        is LogValue.ErrorCodeValue -> FieldKind.ERROR_CODE
        is LogValue.ReasonCodeValue -> FieldKind.REASON
        is LogValue.OperationCodeValue -> FieldKind.OPERATION
        is LogValue.HashedIdentifierInput -> when (value.namespace) {
            IdentifierNamespace.TARGET_PACKAGE -> FieldKind.HASHED_TARGET_PACKAGE
            IdentifierNamespace.BROWSER_PACKAGE -> FieldKind.HASHED_BROWSER_PACKAGE
        }
        is LogValue.HashedIdentifierValue -> FieldKind.PREHASHED_IDENTIFIER
    }

    private fun isValidIdentifierInput(value: String): Boolean =
        value.length <= 255 && TARGET_PACKAGE_PATTERN.matches(value)

    private enum class FieldKind(val identifierNamespace: IdentifierNamespace? = null) {
        COUNT,
        DURATION,
        RATIO,
        BOOLEAN,
        ENUM,
        ERROR_CODE,
        REASON,
        OPERATION,
        HASHED_TARGET_PACKAGE(IdentifierNamespace.TARGET_PACKAGE),
        HASHED_BROWSER_PACKAGE(IdentifierNamespace.BROWSER_PACKAGE),
        PREHASHED_IDENTIFIER,
    }

    private companion object {
        val TARGET_PACKAGE_PATTERN =
            Regex("^[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+$")

        val SAFE_FIELD_CATALOG = mapOf(
            LogFieldKeys.RetryCount to FieldKind.COUNT,
            LogFieldKeys.ElapsedMs to FieldKind.DURATION,
            LogFieldKeys.TargetPackage to FieldKind.HASHED_TARGET_PACKAGE,
            LogFieldKeys.BrowserPackage to FieldKind.HASHED_BROWSER_PACKAGE,
            LogFieldKeys.FromState to FieldKind.ENUM,
            LogFieldKeys.ToState to FieldKind.ENUM,
            LogFieldKeys.LockType to FieldKind.ENUM,
            LogFieldKeys.Reason to FieldKind.REASON,
            LogFieldKeys.Owner to FieldKind.ENUM,
            LogFieldKeys.TimeoutMs to FieldKind.DURATION,
            LogFieldKeys.ActiveLeaseCount to FieldKind.COUNT,
            LogFieldKeys.LockReason to FieldKind.REASON,
            LogFieldKeys.Trigger to FieldKind.ENUM,
            LogFieldKeys.Result to FieldKind.ENUM,
            LogFieldKeys.AuthenticationMethod to FieldKind.ENUM,
            LogFieldKeys.ExitStatus to FieldKind.COUNT,
            LogFieldKeys.ExitReason to FieldKind.ENUM,
            LogFieldKeys.Importance to FieldKind.ENUM,
            LogFieldKeys.PssKb to FieldKind.COUNT,
            LogFieldKeys.RssKb to FieldKind.COUNT,
            LogFieldKeys.Source to FieldKind.ENUM,
        )
    }
}
