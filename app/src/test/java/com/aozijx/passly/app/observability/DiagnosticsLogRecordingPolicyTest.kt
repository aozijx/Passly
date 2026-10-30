package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import com.aozijx.passly.core.observability.sink.LogDropCounts
import com.aozijx.passly.core.observability.sink.LogClearResult
import com.aozijx.passly.core.observability.sink.LogRecordCursor
import com.aozijx.passly.core.observability.sink.LogRecordPage
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsLogRecordingPolicyTest {
    @Test
    fun `unloaded and disabled settings do not persist records`() {
        val policy = DiagnosticsLogRecordingPolicy()

        assertEquals(false, LogLevel.entries.any(policy::allows))
        policy.update(DiagnosticsLogRecordingMode.DISABLED)
        assertEquals(false, LogLevel.entries.any(policy::allows))
    }

    @Test
    fun `warn mode accepts only warning and higher levels`() {
        val policy = DiagnosticsLogRecordingPolicy().apply {
            update(DiagnosticsLogRecordingMode.WARN_AND_ABOVE)
        }

        assertEquals(
            setOf(LogLevel.WARN, LogLevel.ERROR, LogLevel.FATAL),
            LogLevel.entries.filter(policy::allows).toSet(),
        )
    }

    @Test
    fun `info mode excludes debug while debug mode accepts every level`() {
        val policy = DiagnosticsLogRecordingPolicy().apply {
            update(DiagnosticsLogRecordingMode.INFO_AND_ABOVE)
        }
        assertEquals(setOf(LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.FATAL), LogLevel.entries.filter(policy::allows).toSet())

        policy.update(DiagnosticsLogRecordingMode.DEBUG_AND_ABOVE)
        assertEquals(LogLevel.entries.toSet(), LogLevel.entries.filter(policy::allows).toSet())
    }

    @Test
    fun `store filters normal and emergency writes and keeps storage operations delegated`() {
        val backing = RecordingStore()
        val policy = DiagnosticsLogRecordingPolicy()
        val store = PolicyLogRecordStore(backing, policy)
        val info = record(LogLevel.INFO)
        val fatal = record(LogLevel.FATAL)

        store.emit(info)
        assertFalse(store.crashEmergencyWrite(fatal, 200L))
        assertTrue(backing.records.isEmpty())
        assertEquals(0, backing.emergencyWrites)

        policy.update(DiagnosticsLogRecordingMode.WARN_AND_ABOVE)
        store.emit(info)
        store.emit(fatal)
        assertTrue(store.crashEmergencyWrite(fatal, 200L))
        assertEquals(listOf(fatal, fatal), backing.records)
        assertEquals(1, backing.emergencyWrites)
        store.clear()
        assertEquals(1, backing.clearCalls)
    }

    private fun record(level: LogLevel) = SanitizedLogRecord(
        eventId = EventId("event_12345678"),
        timestampMs = 1L,
        elapsedRealtimeMs = 1L,
        level = level,
        category = LogCategory.APPLICATION,
        event = EventKey("application.started"),
        processSessionId = ProcessSessionId("process_12345678"),
        secureSessionId = null,
        trace = TraceSnapshot(TraceId("trace_12345678"), SpanId("span_12345678"), null),
        fields = LogFields.Empty,
        error = null,
    )

    private class RecordingStore : LogRecordStore {
        val records = mutableListOf<SanitizedLogRecord>()
        var emergencyWrites = 0
        var clearCalls = 0

        override fun write(record: SanitizedLogRecord) {
            records += record
        }

        override fun readPage(cursor: LogRecordCursor?, limit: Int) = LogRecordPage(emptyList(), 0, null)
        override fun readBetween(startTimestampMs: Long, endTimestampMs: Long, limit: Int) = emptyList<SanitizedLogRecord>()
        override fun flush(timeoutMs: Long) = true
        override fun storageBytes() = 0L
        override fun drainDroppedCounts() = LogDropCounts()
        override fun clear(): LogClearResult {
            clearCalls++
            return LogClearResult.Cleared
        }
        override fun crashEmergencyWrite(record: SanitizedLogRecord, timeoutMs: Long): Boolean {
            emergencyWrites++
            records += record
            return true
        }
        override fun close() = Unit
    }
}
