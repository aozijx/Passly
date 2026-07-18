package com.aozijx.passly.core.diagnostics

import android.util.Log
import com.aozijx.passly.BuildConfig
import java.util.concurrent.CopyOnWriteArrayList

fun interface AppLogger {
    fun log(event: LogEvent)
}

interface LogSink {
    fun write(event: SanitizedLogEvent)
    fun flush(timeoutMs: Long): Boolean = true
    fun close() = Unit
}

class AndroidLogSink(
    private val enabled: () -> Boolean = { true }
) : LogSink {
    override fun write(event: SanitizedLogEvent) {
        if (!enabled()) return
        val minimum = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
        if (event.level.ordinal < minimum.ordinal) return
        val tag = "Passly/${event.category.name.take(12)}"
        val message = event.encodeLine()
        when (event.level) {
            LogLevel.VERBOSE -> Log.v(tag, message)
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR, LogLevel.FATAL -> Log.e(tag, message)
        }
    }
}

class CompositeLogSink(sinks: Collection<LogSink>) : LogSink {
    private val sinks = CopyOnWriteArrayList(sinks)

    override fun write(event: SanitizedLogEvent) {
        sinks.forEach { sink -> runCatching { sink.write(event) } }
    }

    override fun flush(timeoutMs: Long): Boolean = sinks.all { it.flush(timeoutMs) }
    override fun close() = sinks.forEach(LogSink::close)
}

class StructuredLogger(private val sink: LogSink) : AppLogger {
    override fun log(event: LogEvent) = sink.write(LogSanitizer.sanitize(event))
    fun flush(timeoutMs: Long): Boolean = sink.flush(timeoutMs)
    fun close() = sink.close()
}

object AppLog : AppLogger {
    @Volatile
    private var delegate: AppLogger = StructuredLogger(AndroidLogSink())

    fun install(logger: AppLogger) {
        delegate = logger
    }

    override fun log(event: LogEvent) = delegate.log(event)

    fun d(category: LogCategory, name: String, fields: Map<String, String> = emptyMap()) =
        log(LogEvent(LogLevel.DEBUG, category, name, fields))

    fun i(category: LogCategory, name: String, fields: Map<String, String> = emptyMap()) =
        log(LogEvent(LogLevel.INFO, category, name, fields))

    fun w(
        category: LogCategory,
        name: String,
        fields: Map<String, String> = emptyMap(),
        throwable: Throwable? = null
    ) = log(LogEvent(LogLevel.WARN, category, name, fields, throwable))

    fun e(
        category: LogCategory,
        name: String,
        fields: Map<String, String> = emptyMap(),
        throwable: Throwable? = null
    ) = log(LogEvent(LogLevel.ERROR, category, name, fields, throwable))
}
