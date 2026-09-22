package com.aozijx.passly.core.telemetry

import java.util.Locale

object TelemetryEventFormatter {
    fun format(event: TelemetryEvent, multiline: Boolean = false): String = buildString {
        append(event.level.name)
        append(' ')
        append(event.category.name)
        append(' ')
        append(event.name)
        if (event.fields.isNotEmpty()) {
            append(' ')
            append(event.fields.entries.sortedBy(Map.Entry<String, SafeLogValue>::key)
                .joinToString(", ") { (key, value) -> "$key=${formatField(value)}" })
        }
        event.throwableType?.let { append(" error=$it") }
        append(" correlation=")
        append(event.correlationId)
        if (event.appStackFrames.isNotEmpty()) {
            if (multiline) {
                event.appStackFrames.forEach { frame ->
                    append("\n  at ")
                    append(frame)
                }
            } else {
                append(" frames=")
                append(event.appStackFrames.joinToString(" <- "))
            }
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
