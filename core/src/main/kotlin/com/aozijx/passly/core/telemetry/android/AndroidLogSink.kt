package com.aozijx.passly.core.telemetry.android

import android.util.Log
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.SafeLogValue
import com.aozijx.passly.core.telemetry.TelemetryEvent
import com.aozijx.passly.core.telemetry.TelemetryReporter
import java.util.Locale

/**
 * Android Logcat 遥测发射器实现。
 *
 * 将 [TelemetryEvent] 输出到 [Log.v]/[Log.d]/[Log.i]/[Log.w]/[Log.e]。
 */
class AndroidLogSink(
    private val minimumLevel: EventLevel,
    private val enabled: () -> Boolean = { true }
) : TelemetryReporter {

    override fun emit(event: TelemetryEvent) {
        if (!enabled()) return
        if (event.level.ordinal < minimumLevel.ordinal) return

        val tag = "Passly/${event.category.name.take(12)}"
        val message = formatEvent(event)

        when (event.level) {
            EventLevel.VERBOSE -> Log.v(tag, message)
            EventLevel.DEBUG -> Log.d(tag, message)
            EventLevel.INFO -> Log.i(tag, message)
            EventLevel.WARN -> Log.w(tag, message)
            EventLevel.ERROR, EventLevel.FATAL -> Log.e(tag, message)
        }
    }

    private fun formatEvent(event: TelemetryEvent): String = buildString {
        append(event.name)
        if (event.fields.isNotEmpty()) {
            append(' ') // single space separator, not pipe
            append(event.fields.entries.joinToString(", ") { (k, v) ->
                "$k=${formatField(v)}"
            })
        }
        event.throwableType?.let { append(" [${it}]") }
        if (event.appStackFrames.isNotEmpty()) {
            append(" frames=")
            append(event.appStackFrames.joinToString(";") { it.substringAfterLast('.') })
        }
    }

    private fun formatField(value: SafeLogValue): String = when (value) {
        is SafeLogValue.Count -> value.value.toString()
        is SafeLogValue.DurationMs -> "${value.value}ms"
        is SafeLogValue.Ratio -> String.format(Locale.ROOT, "%.2f", value.value)
        is SafeLogValue.BooleanValue -> value.value.toString()
        is SafeLogValue.EnumName -> value.name
        is SafeLogValue.ErrorCodeValue -> value.code.value
        is SafeLogValue.OperationCodeValue -> value.code.value
    }
}
