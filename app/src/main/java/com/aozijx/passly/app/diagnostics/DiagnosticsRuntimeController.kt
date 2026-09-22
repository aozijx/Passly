package com.aozijx.passly.app.diagnostics

import android.os.Process
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.telemetry.android.AndroidLogSink
import com.aozijx.passly.core.telemetry.CompositeTelemetryReporter
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.TelemetryEvent
import com.aozijx.passly.core.telemetry.TelemetryEventFormatter
import com.aozijx.passly.core.telemetry.TelemetryFileStoreFactory
import com.aozijx.passly.core.telemetry.TelemetryRuntime
import com.aozijx.passly.core.telemetry.toTelemetrySnapshot
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsRuntimeController @Inject constructor(
    fileStoreFactory: TelemetryFileStoreFactory,
) : DiagnosticsLogStore {
    private val fileStore = fileStoreFactory.create()
    val reporter: TelemetryReporter = CompositeTelemetryReporter(
        AndroidLogSink(
            minimumLevel = if (BuildConfig.DEBUG) EventLevel.DEBUG else EventLevel.WARN,
        ),
        TelemetryReporter(fileStore::write)
    )

    @Volatile
    private var previousCrashHandler: Thread.UncaughtExceptionHandler? = null

    fun start() {
        TelemetryRuntime.install(reporter)
        installCrashHandler()
    }

    fun flush(timeoutMs: Long = 300L): Boolean = fileStore.flush(timeoutMs)

    override suspend fun readLines(limit: Int): List<String> = withContext(Dispatchers.IO) {
        fileStore.readEvents(limit).map(::formatEvent)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        fileStore.clear()
    }

    fun shutdown() {
        fileStore.close()
        if (Thread.getDefaultUncaughtExceptionHandler() === crashHandler) {
            Thread.setDefaultUncaughtExceptionHandler(previousCrashHandler)
        }
    }

    private val crashHandler = Thread.UncaughtExceptionHandler { thread, error ->
        val throwable = error.toTelemetrySnapshot()
        val event = TelemetryEvent(
            level = EventLevel.FATAL,
            category = EventCategory.APPLICATION,
            name = "application.crash",
            throwableType = throwable.type,
            appStackFrames = throwable.appStackFrames,
        )
        reporter.emit(event)
        if (!flush(300L)) fileStore.crashEmergencyWrite(event, 200L)
        previousCrashHandler?.uncaughtException(thread, error)
            ?: Process.killProcess(Process.myPid())
    }

    private fun installCrashHandler() {
        if (Thread.getDefaultUncaughtExceptionHandler() === crashHandler) return
        previousCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
    }

    private fun formatEvent(event: TelemetryEvent): String = buildString {
        append(LOG_TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(event.timestampMs)))
        append(' ')
        append(TelemetryEventFormatter.format(event, multiline = true))
    }

    private companion object {
        val LOG_TIMESTAMP_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault())
    }
}
