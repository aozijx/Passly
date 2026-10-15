package com.aozijx.passly.app.observability

import android.util.Log
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogSink

internal fun interface LogcatRecordFormatter {
    fun format(record: SanitizedLogRecord): String
}

class LogcatSink internal constructor(
    private val isDebugBuild: Boolean,
    private val gateway: AndroidLogGateway = PlatformAndroidLogGateway,
    private val formatter: LogcatRecordFormatter = SafeLogcatRecordFormatter,
) : LogSink {
    override fun emit(record: SanitizedLogRecord) {
        if (!isDebugBuild) return
        val tag = tagFor(record.category)
        val priority = priorityFor(record.level)
        if (!gateway.isLoggable(tag, priority)) return

        val fullSummary = formatter.format(record).singleLine()
        val summary = if (fullSummary.length <= MAX_SUMMARY_CHARS) {
            fullSummary
        } else {
            truncatedSummary(record)
        }
        gateway.println(priority, tag, summary)
    }

    private fun tagFor(category: LogCategory): String = when (category) {
        LogCategory.APPLICATION -> "Passly/App"
        LogCategory.DATABASE -> "Passly/Db"
        LogCategory.SECURITY -> "Passly/Auth"
        LogCategory.AUTOFILL -> "Passly/Autofill"
        LogCategory.BACKUP -> "Passly/Backup"
    }

    private fun priorityFor(level: LogLevel): AndroidLogPriority = when (level) {
        LogLevel.DEBUG -> AndroidLogPriority.DEBUG
        LogLevel.INFO -> AndroidLogPriority.INFO
        LogLevel.WARN -> AndroidLogPriority.WARN
        LogLevel.ERROR, LogLevel.FATAL -> AndroidLogPriority.ERROR
    }

    private fun truncatedSummary(record: SanitizedLogRecord): String =
        "event_id=${record.eventId.value} name=${record.event.value} " +
            "trace_id=${record.trace.traceId.value} span_id=${record.trace.spanId.value} truncated=true"

    private fun String.singleLine(): String =
        replace('\n', ' ').replace('\r', ' ').replace('\u0000', ' ')

    companion object {
        const val MAX_SUMMARY_CHARS = 1_024
    }
}

private object SafeLogcatRecordFormatter : LogcatRecordFormatter {
    override fun format(record: SanitizedLogRecord): String = buildString {
        append("event_id=").append(record.eventId.value)
        append(" name=").append(record.event.value)
        append(" level=").append(record.level.name)
        append(" timestamp_ms=").append(record.timestampMs)
        append(" elapsed_realtime_ms=").append(record.elapsedRealtimeMs)
        append(" process_session_id=").append(record.processSessionId.value)
        record.secureSessionId?.let { append(" secure_session_id=").append(it.value) }
        append(" trace_id=").append(record.trace.traceId.value)
        append(" span_id=").append(record.trace.spanId.value)
        record.trace.parentSpanId?.let { append(" parent_span_id=").append(it.value) }
        record.fields.values.toSortedMap(compareBy { it.value }).forEach { (key, value) ->
            append(' ').append(key.value).append('=').append(value.safeText())
        }
        record.error?.let { error ->
            append(" error_code=").append(error.errorCode.value)
            append(" reason=").append(error.reason.value)
            error.exception?.let { exception ->
                append(" exception_type=").append(exception.type.value)
                append(" app_stack=").append(exception.appStackFrames.joinToString(",") { it.value })
                append(" cause_types=").append(exception.causeTypes.joinToString(",") { it.value })
                append(" foreign_frame_count=").append(exception.foreignFrameCount)
            }
        }
    }

    private fun LogValue.safeText(): String = when (this) {
        is LogValue.Count -> value.toString()
        is LogValue.DurationMs -> value.toString()
        is LogValue.Ratio -> value.toString()
        is LogValue.BooleanValue -> value.toString()
        is LogValue.EnumValue -> value.value
        is LogValue.ErrorCodeValue -> value.value
        is LogValue.ReasonCodeValue -> value.value
        is LogValue.OperationCodeValue -> value.value
        is LogValue.HashedIdentifierValue -> value.value
        is LogValue.HashedIdentifierInput -> "invalid_pending_identifier"
    }
}

private object PlatformAndroidLogGateway : AndroidLogGateway {
    override fun isLoggable(tag: String, priority: AndroidLogPriority): Boolean =
        Log.isLoggable(tag, priority.androidValue)

    override fun println(priority: AndroidLogPriority, tag: String, message: String) {
        Log.println(priority.androidValue, tag, message)
    }

    private val AndroidLogPriority.androidValue: Int
        get() = when (this) {
            AndroidLogPriority.DEBUG -> Log.DEBUG
            AndroidLogPriority.INFO -> Log.INFO
            AndroidLogPriority.WARN -> Log.WARN
            AndroidLogPriority.ERROR -> Log.ERROR
        }
}
