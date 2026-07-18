package com.aozijx.passly.core.diagnostics

import android.content.Context
import com.aozijx.passly.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object DiagnosticsRuntime {
    private val androidEnabled = AtomicBoolean(true)
    private val fileEnabled = AtomicBoolean(false)

    @Volatile
    private var logger: StructuredLogger? = null

    @Volatile
    private var fileSink: EncryptedFileLogSink? = null

    fun start(context: Context, scope: CoroutineScope) {
        val appContext = context.applicationContext
        val policyStore = DiagnosticsPolicyStore(appContext)
        val encryptedSink = EncryptedFileLogSink(appContext) { fileEnabled.get() }
        val structured = StructuredLogger(
            CompositeLogSink(
                listOf(
                    AndroidLogSink { androidEnabled.get() },
                    encryptedSink
                )
            )
        )
        fileSink = encryptedSink
        logger = structured
        AppLog.install(structured)
        DiagnosticsCrashHandler.install()

        scope.launch {
            policyStore.settings.collectLatest { settings ->
                androidEnabled.set(settings.androidSinkEnabled)
                fileEnabled.set(
                    BuildConfig.DEBUG ||
                        settings.fileLoggingEnabledUntilMs > System.currentTimeMillis()
                )
            }
        }
    }

    fun flush(timeoutMs: Long = 300L): Boolean = logger?.flush(timeoutMs) ?: false

    fun emergency(event: LogEvent, timeoutMs: Long = 200L): Boolean =
        fileSink?.emergencyWrite(LogSanitizer.sanitize(event), timeoutMs) ?: false

    fun readAll(): List<String> = fileSink?.readAll().orEmpty()
    fun clear() = fileSink?.clear()
    fun directory(context: Context): File = File(context.noBackupFilesDir, "diagnostics")

    fun shutdown() {
        logger?.close()
        logger = null
        fileSink = null
    }
}
