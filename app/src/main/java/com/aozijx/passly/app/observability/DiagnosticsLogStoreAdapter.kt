package com.aozijx.passly.app.observability

import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogClearResult
import com.aozijx.passly.core.observability.sink.LogRecordCursor
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
internal class DiagnosticsLogStoreAdapter
    @Inject
    constructor(
        private val store: LogRecordStore,
        private val recordEncoder: TsvRecordEncoder,
    ) : DiagnosticsLogStore {
        override suspend fun readLines(limit: Int): List<DiagnosticsLogLine> =
            withContext(Dispatchers.IO) {
                require(limit > 0) { "Line limit must be positive" }
                readRecords(limit).map { record ->
                    DiagnosticsLogLine(
                        level = record.level,
                        text = recordEncoder.encode(record),
                        traceId = record.trace.traceId.value,
                        event = record.event.value,
                        errorCode = record.error?.errorCode?.value,
                        exceptionKind = record.error?.exception?.kind?.name,
                        exceptionType = record.error?.exception?.type?.value,
                    )
                }
            }

        override suspend fun storageBytes(): Long = withContext(Dispatchers.IO) { store.storageBytes() }

        override suspend fun clear(): LogClearResult = withContext(Dispatchers.IO) { store.clear() }

        private fun readRecords(limit: Int): List<SanitizedLogRecord> {
            val result = ArrayList<SanitizedLogRecord>(limit)
            var cursor: LogRecordCursor? = null
            do {
                val page = store.readPage(cursor, minOf(PAGE_SIZE, limit - result.size))
                result += page.records.take(limit - result.size)
                cursor = page.nextCursor
            } while (cursor != null && result.size < limit)
            return result
        }

        private companion object {
            const val PAGE_SIZE = 500
        }
    }
