package com.aozijx.passly.core.diagnostics

import android.os.Process

object DiagnosticsCrashHandler : Thread.UncaughtExceptionHandler {
    @Volatile
    private var previous: Thread.UncaughtExceptionHandler? = null

    fun install() {
        if (Thread.getDefaultUncaughtExceptionHandler() === this) return
        previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, error: Throwable) {
        val event = LogEvent(
            level = LogLevel.FATAL,
            category = LogCategory.APPLICATION,
            name = "application.crash",
            fields = mapOf("thread" to thread.name.take(48)),
            throwable = error
        )
        AppLog.log(event)
        if (!DiagnosticsRuntime.flush(300L)) {
            DiagnosticsRuntime.emergency(event, 200L)
        }
        previous?.uncaughtException(thread, error) ?: Process.killProcess(Process.myPid())
    }
}
