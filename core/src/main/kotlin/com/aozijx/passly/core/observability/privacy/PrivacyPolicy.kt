package com.aozijx.passly.core.observability.privacy

import com.aozijx.passly.core.observability.ErrorPayload
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.ExceptionMapper
import com.aozijx.passly.core.observability.IdentifierNamespace
import com.aozijx.passly.core.observability.LogFieldKey
import com.aozijx.passly.core.observability.LogFieldKind
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
) : AssertionError("Log contract violation: ${violation.name}")

sealed interface SanitizationResult {
    data class Accepted(
        val record: SanitizedLogRecord,
        val droppedFieldCount: Int,
    ) : SanitizationResult

    data class Rejected(
        val violation: ContractViolation,
    ) : SanitizationResult
}

fun interface LogSanitizer {
    fun sanitize(
        event: PendingLogEvent,
        context: SanitizationContext,
    ): SanitizationResult
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
            val expectedKind = key.kind ?: return violation(ContractViolation.FIELD_NOT_REGISTERED)
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
                    val hashed =
                        try {
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

                is LogValue.HashedIdentifierValue -> {
                    return violation(ContractViolation.PREHASHED_IDENTIFIER_NOT_ALLOWED)
                }

                else -> {
                    safeFields[key] = value
                }
            }
        }

        val pendingError = event.error
        val safeError =
            pendingError?.let {
                ErrorPayload(
                    errorCode = it.errorCode,
                    reason = it.reason,
                    exception =
                        it.throwable?.let { throwable ->
                            exceptionSnapshotFactory.create(
                                throwable = throwable,
                                kind = ExceptionMapper.map(it.errorCode, it.reason, throwable),
                            )
                        },
                )
            }
        return SanitizationResult.Accepted(
            record =
                SanitizedLogRecord(
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

    private fun validateErrorContract(event: PendingLogEvent): ContractViolation? =
        when (event.level) {
            LogLevel.DEBUG, LogLevel.INFO -> {
                ContractViolation.INVALID_ERROR_CONTRACT.takeIf { event.error != null }
            }

            LogLevel.WARN -> {
                ContractViolation.INVALID_ERROR_CONTRACT.takeIf { event.error == null }
            }

            LogLevel.ERROR, LogLevel.FATAL -> {
                ContractViolation.INVALID_ERROR_CONTRACT.takeIf {
                    event.error == null || event.error.throwable == null
                }
            }
        }

    private fun violation(violation: ContractViolation): SanitizationResult =
        when (violationBehavior) {
            ContractViolationBehavior.THROW -> throw LogContractViolation(violation)
            ContractViolationBehavior.REJECT -> SanitizationResult.Rejected(violation)
        }

    private fun kindOf(value: LogValue): LogFieldKind =
        when (value) {
            is LogValue.Count -> {
                LogFieldKind.COUNT
            }

            is LogValue.DurationMs -> {
                LogFieldKind.DURATION
            }

            is LogValue.Ratio -> {
                LogFieldKind.RATIO
            }

            is LogValue.BooleanValue -> {
                LogFieldKind.BOOLEAN
            }

            is LogValue.EnumValue -> {
                LogFieldKind.ENUM
            }

            is LogValue.ErrorCodeValue -> {
                LogFieldKind.ERROR_CODE
            }

            is LogValue.ReasonCodeValue -> {
                LogFieldKind.REASON
            }

            is LogValue.OperationCodeValue -> {
                LogFieldKind.OPERATION
            }

            is LogValue.HashedIdentifierInput -> {
                when (value.namespace) {
                    IdentifierNamespace.TARGET_PACKAGE -> LogFieldKind.HASHED_TARGET_PACKAGE
                    IdentifierNamespace.BROWSER_PACKAGE -> LogFieldKind.HASHED_BROWSER_PACKAGE
                }
            }

            is LogValue.HashedIdentifierValue -> {
                LogFieldKind.PREHASHED_IDENTIFIER
            }
        }

    private fun isValidIdentifierInput(value: String): Boolean = value.length <= 255 && TARGET_PACKAGE_PATTERN.matches(value)

    private companion object {
        val TARGET_PACKAGE_PATTERN =
            Regex("^[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+$")
    }
}
