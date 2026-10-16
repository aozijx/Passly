package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.sink.LogSink
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

data class LocalDiagnosticAlert(
    val errorCode: ErrorCode,
    val traceId: TraceId,
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
        if (error.errorCode != ErrorCodes.Database.DatabaseSessionOpenFailed) return null
        databaseOpenFailures.addLast(Failure(record.timestampMs))
        prune(record.timestampMs)
        if (alert == null && databaseOpenFailures.size >= ALERT_THRESHOLD) {
            alert = LocalDiagnosticAlert(error.errorCode, record.trace.traceId, record.timestampMs)
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

    private data class Failure(val timestampMs: Long)

    private companion object {
        const val ALERT_THRESHOLD = 3
        const val WINDOW_MS = 10 * 60 * 1000L
    }
}
