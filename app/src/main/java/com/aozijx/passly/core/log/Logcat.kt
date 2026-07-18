package com.aozijx.passly.core.log

import android.content.Context
import android.security.keystore.UserNotAuthenticatedException
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.diagnostics.DiagnosticsRuntime
import com.aozijx.passly.core.diagnostics.LogCategory
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.ErrorLayer
import java.io.File

/** Temporary source-compatibility adapter. New code must inject AppLogger. */
@Deprecated("Use AppLogger/AppLog")
class Logcat(private val context: Context, filter: LogFilter = NoOpFilter()) {
    init {
        @Suppress("UNUSED_VARIABLE") val ignored = filter
    }

    companion object {
        fun init(logcat: Logcat) = Unit
        fun v(tag: String = "AppLog", msg: String) = AppLog.d(LogCategory.APPLICATION, tag, mapOf("event" to msg))
        fun d(tag: String = "AppLog", msg: String) = AppLog.d(LogCategory.APPLICATION, tag, mapOf("event" to msg))
        fun i(tag: String = "AppLog", msg: String) = AppLog.i(LogCategory.APPLICATION, tag, mapOf("event" to msg))
        fun w(tag: String = "AppLog", msg: String, tr: Throwable? = null) =
            AppLog.w(LogCategory.APPLICATION, tag, mapOf("event" to msg), tr)
        fun e(tag: String = "AppLog", msg: String, tr: Throwable? = null) =
            AppLog.e(LogCategory.APPLICATION, tag, mapOf("event" to msg), tr)

        fun logCryptoException(tag: String, action: String, error: Exception) {
            if (error is UserNotAuthenticatedException) {
                AppLog.w(LogCategory.SECURITY, tag, mapOf("operation" to action, "reason" to "not_authenticated"))
            } else {
                AppLog.e(LogCategory.SECURITY, tag, mapOf("operation" to action), error)
            }
        }

        fun getLogFolder(): AppResult<File> = AppResult.runCatching("getLogFolder", ErrorLayer.DATA) {
            DiagnosticsRuntime.directory(com.aozijx.passly.app.PasslyApplication.context).apply { mkdirs() }
        }

        fun flushLogs() = DiagnosticsRuntime.flush()
        fun clearOldLogs(daysToKeep: Int = 7) = Unit
        fun readAllLogs(): AppResult<String> = AppResult.success(DiagnosticsRuntime.readAll().joinToString("\n"))
        fun clearAllLogs() = DiagnosticsRuntime.clear()
        fun shutdown() = DiagnosticsRuntime.shutdown()
    }
}
