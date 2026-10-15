package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogSink

/** Keeps the current thread's fatal record available for the bounded emergency writer. */
internal class CrashRecordCaptureSink : LogSink {
    private val captured = ThreadLocal<SanitizedLogRecord?>()

    override fun emit(record: SanitizedLogRecord) {
        if (record.level == LogLevel.FATAL) captured.set(record)
    }

    fun clear() = captured.remove()
    fun take(): SanitizedLogRecord? = captured.get().also { captured.remove() }
}
