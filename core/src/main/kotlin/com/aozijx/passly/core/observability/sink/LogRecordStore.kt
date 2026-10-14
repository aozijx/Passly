package com.aozijx.passly.core.observability.sink

import com.aozijx.passly.core.observability.SanitizedLogRecord

data class LogRecordCursor(
    val fileIndex: Int,
    val fileId: String,
    val nextRecordOffset: Long,
    val nextSequence: Long,
)

data class LogRecordPage(
    val records: List<SanitizedLogRecord>,
    val totalRecords: Int,
    val nextCursor: LogRecordCursor?,
)

interface LogRecordStore : LogSink, AutoCloseable {
    fun write(record: SanitizedLogRecord)
    override fun emit(record: SanitizedLogRecord) = write(record)
    fun readPage(cursor: LogRecordCursor?, limit: Int): LogRecordPage
    fun readBetween(startTimestampMs: Long, endTimestampMs: Long, limit: Int): List<SanitizedLogRecord>
    fun flush(timeoutMs: Long): Boolean
    fun clear()
    fun crashEmergencyWrite(record: SanitizedLogRecord, timeoutMs: Long): Boolean
}

fun interface LogRecordStoreFactory {
    fun create(): LogRecordStore
}
