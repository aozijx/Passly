package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import com.aozijx.passly.core.observability.sink.LogClearResult
import com.aozijx.passly.core.observability.sink.LogDropCounts
import com.aozijx.passly.core.observability.sink.LogRecordCursor
import com.aozijx.passly.core.observability.sink.LogRecordPage
import com.aozijx.passly.core.observability.sink.LogRecordStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DiagnosticsLogStoreAdapterTest {
    @Test
    fun `read lines requests bounded pages and maps persisted fields`() =
        runTest {
            val records = (0 until 501).map(::record)
            val store = RecordingLogRecordStore(records)
            val adapter = DiagnosticsLogStoreAdapter(store, TsvRecordEncoder())

            val lines = adapter.readLines(limit = 501)

            assertEquals(listOf(500, 1), store.readLimits)
            assertEquals(501, lines.size)
            assertEquals(records.first().trace.traceId.value, lines.first().traceId)
            assertEquals(records.first().event.value, lines.first().event)
            assertEquals(LogLevel.INFO, lines.first().level)
            assertEquals(8, lines.first().text.split('\t').size)
            assertTrue(lines.first().text.contains(records.first().event.value))
            assertTrue(lines.first().text.contains("trace_id=${records.first().trace.traceId.value}"))
        }

    @Test
    fun `clear and storage size use the shared record store`() =
        runTest {
            val store = RecordingLogRecordStore(emptyList(), bytes = 4096L, clearResult = LogClearResult.Cleared)
            val adapter = DiagnosticsLogStoreAdapter(store, TsvRecordEncoder())

            assertEquals(LogClearResult.Cleared, adapter.clear())
            assertEquals(4096L, adapter.storageBytes())
            assertEquals(1, store.clearCalls)
            assertEquals(1, store.storageBytesCalls)
        }

    @Test
    fun `read lines rejects non positive limit`() =
        runTest {
            val adapter = DiagnosticsLogStoreAdapter(RecordingLogRecordStore(emptyList()), TsvRecordEncoder())

            try {
                adapter.readLines(0)
                fail("Expected non-positive limit to be rejected")
            } catch (_: IllegalArgumentException) {
                // Expected validation failure.
            }
        }

    private fun record(index: Int) =
        SanitizedLogRecord(
            eventId = EventId("event_$index".padEnd(12, '0')),
            timestampMs = index.toLong(),
            elapsedRealtimeMs = index.toLong(),
            level = LogLevel.INFO,
            category = LogCategory.AUTOFILL,
            event = Events.Autofill.ResultDelivered,
            processSessionId = ProcessSessionId("process_12345678"),
            secureSessionId = null,
            trace = TraceSnapshot(TraceId("trace_12345678"), SpanId("span_12345678"), null),
            fields = LogFields.Empty,
            error = null,
        )

    private class RecordingLogRecordStore(
        private val records: List<SanitizedLogRecord>,
        private val bytes: Long = 0,
        private val clearResult: LogClearResult = LogClearResult.Failed,
    ) : LogRecordStore {
        val readLimits = mutableListOf<Int>()
        var clearCalls = 0
        var storageBytesCalls = 0

        override fun write(record: SanitizedLogRecord) = Unit

        override fun readPage(
            cursor: LogRecordCursor?,
            limit: Int,
        ): LogRecordPage {
            readLimits += limit
            val start = cursor?.nextSequence?.toInt() ?: 0
            val pageRecords = records.drop(start).take(limit)
            val next = (start + pageRecords.size).takeIf { it < records.size }?.let {
                LogRecordCursor(0, "log_file", it.toLong(), it.toLong())
            }
            return LogRecordPage(pageRecords, records.size, next)
        }

        override fun readBetween(startTimestampMs: Long, endTimestampMs: Long, limit: Int) = emptyList<SanitizedLogRecord>()

        override fun flush(timeoutMs: Long) = true

        override fun storageBytes(): Long {
            storageBytesCalls++
            return bytes
        }

        override fun drainDroppedCounts() = LogDropCounts()

        override fun clear(): LogClearResult {
            clearCalls++
            return clearResult
        }

        override fun crashEmergencyWrite(record: SanitizedLogRecord, timeoutMs: Long) = true

        override fun close() = Unit
    }
}
