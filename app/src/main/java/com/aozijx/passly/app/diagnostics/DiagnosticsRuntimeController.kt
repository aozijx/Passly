package com.aozijx.passly.app.diagnostics

import android.os.Process
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.telemetry.android.AndroidLogSink
import com.aozijx.passly.core.telemetry.CompositeTelemetryReporter
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.TelemetryEvent
import com.aozijx.passly.core.telemetry.TelemetryFileStoreFactory
import com.aozijx.passly.core.telemetry.TelemetryPolicyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsRuntimeController @Inject constructor(
    fileStoreFactory: TelemetryFileStoreFactory,
    private val policyController: TelemetryPolicyController
) {
    private val androidEnabled = AtomicBoolean(true)
    private val fileEnabledUntil = AtomicLong(0L)
    private val fileStore = fileStoreFactory.create(fileEnabledUntil)
    val reporter: TelemetryReporter = CompositeTelemetryReporter(
        AndroidLogSink(
            minimumLevel = if (BuildConfig.DEBUG) EventLevel.DEBUG else EventLevel.WARN,
            enabled = androidEnabled::get,
        ),
        TelemetryReporter(fileStore::write)
    )

    @Volatile
    private var previousCrashHandler: Thread.UncaughtExceptionHandler? = null

    fun start(scope: CoroutineScope) {
        AppTelemetry.install(reporter)
        installCrashHandler()
        scope.launch {
            policyController.policies.collectLatest { policy ->
                androidEnabled.set(policy.androidSinkEnabled)
                fileEnabledUntil.set(policy.encryptedFileEnabledUntilMs)
            }
        }
    }

    fun flush(timeoutMs: Long = 300L): Boolean = fileStore.flush(timeoutMs)

    fun readLines(limit: Int = 500): List<String> =
        fileStore.readEvents(limit).map(::formatEvent)

    fun clear() = fileStore.clear()

    fun shutdown() {
        fileStore.close()
        if (Thread.getDefaultUncaughtExceptionHandler() === crashHandler) {
            Thread.setDefaultUncaughtExceptionHandler(previousCrashHandler)
        }
    }

    private val crashHandler = Thread.UncaughtExceptionHandler { thread, error ->
        val event = TelemetryEvent(
            level = EventLevel.FATAL,
            category = EventCategory.APPLICATION,
            name = "application.crash",
            throwableType = error.javaClass.simpleName.take(64),
            appStackFrames = error.stackTrace
                .asSequence()
                .filter { it.className.startsWith("com.aozijx.passly.") }
                .take(16)
                .map { "${it.className}.${it.methodName}" }
                .toList()
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
        append(event.timestampMs)
        append(' ')
        append(event.level.name)
        append(' ')
        append(event.category.name)
        append(' ')
        append(event.name)
        if (event.fields.isNotEmpty()) {
            append(' ')
            append(event.fields.keys.sorted().joinToString(","))
        }
        event.throwableType?.let {
            append(" error=")
            append(it)
        }
    }
}
