package com.aozijx.passly.app.diagnostics

import android.security.keystore.UserNotAuthenticatedException
import com.aozijx.passly.core.telemetry.ErrorCode
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.SafeLogValue
import com.aozijx.passly.core.telemetry.TelemetryEmitter
import com.aozijx.passly.core.telemetry.TelemetryEvent

/**
 * Android framework callbacks and legacy utility objects cannot receive constructor injection.
 * This application-bound bridge is installed once by [DiagnosticsRuntimeController].
 *
 * Free-form messages are never persisted. Structured names are accepted only when they match the
 * telemetry identifier grammar, and string fields are reduced to reviewed primitive forms.
 */
object AppTelemetry {
    @Volatile
    private var emitter: TelemetryEmitter = TelemetryEmitter { }

    fun install(value: TelemetryEmitter) {
        emitter = value
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
        fields: Map<String, String> = emptyMap()
    ) = emit(EventLevel.DEBUG, category, name, fields)

    fun i(
        category: EventCategory,
        name: String,
        fields: Map<String, String> = emptyMap()
    ) = emit(EventLevel.INFO, category, name, fields)

    fun w(
        category: EventCategory,
        name: String,
        fields: Map<String, String> = emptyMap(),
        throwable: Throwable? = null
    ) = emit(EventLevel.WARN, category, name, fields, throwable)

    fun e(
        category: EventCategory,
        name: String,
        fields: Map<String, String> = emptyMap(),
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
                "component" to tag,
                "operation" to action
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
        fields: Map<String, String>,
        throwable: Throwable? = null
    ) {
        val safeName = name.takeIf(EVENT_NAME::matches)
            ?: "legacy.${category.name.lowercase()}"
        val safeFields = fields.mapNotNull { (key, value) ->
            val safeKey = key.takeIf(FIELD_NAME::matches) ?: return@mapNotNull null
            val safeValue = when {
                value == "true" || value == "false" ->
                    SafeLogValue.BooleanValue(value.toBooleanStrict())
                value.toLongOrNull() != null ->
                    SafeLogValue.Count(value.toLong())
                ENUM_NAME.matches(value) ->
                    SafeLogValue.EnumName(value)
                ERROR_CODE.matches(value) ->
                    SafeLogValue.ErrorCodeValue(ErrorCode(value))
                else -> null
            } ?: return@mapNotNull null
            safeKey to safeValue
        }.toMap()
        val throwableType = throwable?.javaClass?.simpleName
            ?.takeIf(ENUM_NAME::matches)
        val frames = throwable?.stackTrace
            ?.asSequence()
            ?.filter { it.className.startsWith(APP_PACKAGE_PREFIX) }
            ?.take(MAX_STACK_FRAMES)
            ?.map { "${it.className}.${it.methodName}" }
            ?.toList()
            .orEmpty()
        emitter.emit(
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

    private val EVENT_NAME = Regex("[a-z][a-z0-9_.]{2,95}")
    private val FIELD_NAME = Regex("[a-z][a-z0-9_]{0,63}")
    private val ENUM_NAME = Regex("[A-Z][A-Z0-9_]{0,63}")
    private val ERROR_CODE = Regex("[A-Z_]{3,64}")
    private const val APP_PACKAGE_PREFIX = "com.aozijx.passly."
    private const val MAX_STACK_FRAMES = 16
}
