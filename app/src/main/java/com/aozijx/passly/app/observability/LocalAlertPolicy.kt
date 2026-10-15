package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.DiagnosticId
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogSink
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

data class LocalDiagnosticAlert(
    val errorCode: ErrorCode,
    val diagnosticId: DiagnosticId,
    val createdAtMs: Long,
)

@Singleton
class LocalAlertPolicy @Inject constructor() : LogSink {
    private val databaseOpenFailures = ArrayDeque<Failure>()
    private var alert: LocalDiagnosticAlert? = null

    override fun emit(record: SanitizedLogRecord) {
        observe(record)
    }

    @Synchronized
    fun observe(record: SanitizedLogRecord): LocalDiagnosticAlert? {
        expire(record.timestampMs)
        val error = record.error ?: return null
        if (error.errorCode != ErrorCodes.DatabaseSessionOpenFailed) return null
        databaseOpenFailures.addLast(Failure(record.timestampMs, error.diagnosticId))
        prune(record.timestampMs)
        if (alert == null && databaseOpenFailures.size >= ALERT_THRESHOLD) {
            alert = LocalDiagnosticAlert(error.errorCode, error.diagnosticId, record.timestampMs)
            return alert
        }
        return null
    }

    @Synchronized
    fun activeAlert(nowMs: Long): LocalDiagnosticAlert? {
        expire(nowMs)
        return alert
    }

    private fun expire(nowMs: Long) {
        if (alert?.let { nowMs - it.createdAtMs > WINDOW_MS } == true) {
            alert = null
            databaseOpenFailures.clear()
        }
        prune(nowMs)
    }

    private fun prune(nowMs: Long) {
        while (databaseOpenFailures.firstOrNull()?.let { nowMs - it.timestampMs > WINDOW_MS } == true) {
            databaseOpenFailures.removeFirst()
        }
    }

    private data class Failure(val timestampMs: Long, val diagnosticId: DiagnosticId)

    private companion object {
        const val ALERT_THRESHOLD = 3
        const val WINDOW_MS = 10 * 60 * 1000L
    }
}
