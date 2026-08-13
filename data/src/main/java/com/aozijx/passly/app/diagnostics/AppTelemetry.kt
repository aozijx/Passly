package com.aozijx.passly.app.diagnostics

import android.security.keystore.UserNotAuthenticatedException
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.OperationCode
import com.aozijx.passly.core.telemetry.SafeLogValue
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.TelemetryEvent

/**
 * Android framework callbacks and legacy utility objects cannot receive constructor injection.
 * This application-bound bridge is installed once by [DiagnosticsRuntimeController].
 *
 * Free-form messages are never persisted. Structured names are accepted only when they match the
 * telemetry identifier grammar, and structured calls accept only [SafeLogValue] fields.
 */
object AppTelemetry {
    @Volatile
    private var reporter: TelemetryReporter = TelemetryReporter { }

    fun install(value: TelemetryReporter) {
        reporter = value
    }

    fun v(tag: String, message: String) =
        emitLegacy(EventLevel.VERBOSE, tag, message)

    fun d(tag: String, message: String) =
        emitLegacy(EventLevel.DEBUG, tag, message)

    fun i(tag: String, message: String) =
        emitLegacy(EventLevel.INFO, tag, message)

    fun w(tag: String, message: String, throwable: Throwable? = null) =
        emitLegacy(EventLevel.WARN, tag, message, throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) =
        emitLegacy(EventLevel.ERROR, tag, message, throwable)

    fun d(
        category: EventCategory,
        name: String,
        fields: Map<String, SafeLogValue> = emptyMap()
    ) = emit(EventLevel.DEBUG, category, name, fields)

    fun i(
        category: EventCategory,
        name: String,
        fields: Map<String, SafeLogValue> = emptyMap()
    ) = emit(EventLevel.INFO, category, name, fields)

    fun w(
        category: EventCategory,
        name: String,
        fields: Map<String, SafeLogValue> = emptyMap(),
        throwable: Throwable? = null
    ) = emit(EventLevel.WARN, category, name, fields, throwable)

    fun e(
        category: EventCategory,
        name: String,
        fields: Map<String, SafeLogValue> = emptyMap(),
        throwable: Throwable? = null
    ) = emit(EventLevel.ERROR, category, name, fields, throwable)

    fun logCryptoException(tag: String, action: String, error: Exception) {
        val level = if (error is UserNotAuthenticatedException) {
            EventLevel.WARN
        } else {
            EventLevel.ERROR
        }
        emit(
            level = level,
            category = EventCategory.SECURITY,
            name = if (level == EventLevel.WARN) {
                "crypto.not_authenticated"
            } else {
                "crypto.operation_failed"
            },
            fields = mapOf(
                "component" to SafeLogValue.EnumName(safeEnumName(tag)),
                "operation" to SafeLogValue.OperationCodeValue(
                    OperationCode(safeOperationCode(action))
                )
            ),
            throwable = error
        )
    }

    private fun emitLegacy(
        level: EventLevel,
        @Suppress("UNUSED_PARAMETER") tag: String,
        @Suppress("UNUSED_PARAMETER") message: String,
        throwable: Throwable? = null
    ) {
        emit(
            level = level,
            category = EventCategory.APPLICATION,
            name = "legacy.framework",
            fields = emptyMap(),
            throwable = throwable
        )
    }

    private fun emit(
        level: EventLevel,
        category: EventCategory,
        name: String,
        fields: Map<String, SafeLogValue>,
        throwable: Throwable? = null
    ) {
        val safeName = name.takeIf(EVENT_NAME::matches)
            ?: "legacy.${category.name.lowercase()}"
        val safeFields = fields.filterKeys(FIELD_NAME::matches)
        val throwableType = throwable?.javaClass?.simpleName
            ?.takeIf(ENUM_NAME::matches)
        val frames = throwable?.stackTrace
            ?.asSequence()
            ?.filter { it.className.startsWith(APP_PACKAGE_PREFIX) }
            ?.take(MAX_STACK_FRAMES)
            ?.map { "${it.className}.${it.methodName}" }
            ?.toList()
            .orEmpty()
        reporter.emit(
            TelemetryEvent(
                level = level,
                category = category,
                name = safeName,
                fields = safeFields,
                throwableType = throwableType,
                appStackFrames = frames
            )
        )
    }

    private fun safeEnumName(value: String): String =
        value.uppercase()
            .map { if (it.isLetterOrDigit()) it else '_' }
            .joinToString("")
            .trim('_')
            .take(64)
            .ifBlank { "UNKNOWN" }

    private fun safeOperationCode(value: String): String =
        value.lowercase()
            .map { if (it.isLetterOrDigit()) it else '_' }
            .joinToString("")
            .trim('_')
            .take(128)
            .let { normalized ->
                if (normalized.length >= 3) normalized else "unknown"
            }

    private val EVENT_NAME = Regex("[a-z][a-z0-9_.]{2,95}")
    private val FIELD_NAME = Regex("[a-z][a-z0-9_]{0,63}")
    private val ENUM_NAME = Regex("[A-Z][A-Z0-9_]{0,63}")
    private const val APP_PACKAGE_PREFIX = "com.aozijx.passly."
    private const val MAX_STACK_FRAMES = 16
}
