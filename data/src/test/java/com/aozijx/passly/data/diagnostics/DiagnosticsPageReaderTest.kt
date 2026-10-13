package com.aozijx.passly.data.diagnostics

import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticsPageReaderTest {

    @Test
    fun `reads a large export through bounded pages`() {
        val requestedPageSizes = mutableListOf<Int>()
        var nextEvent = 0

        val events = readMatchingEventsByPage(
            limit = 1_200,
            maxPageSize = 500,
            include = { true },
        ) { _, pageSize ->
            requestedPageSizes += pageSize
            val pageEvents = List(pageSize) {
                TelemetryEvent(
                    timestampMs = nextEvent++.toLong(),
                    level = EventLevel.INFO,
                    category = EventCategory.APPLICATION,
                    name = "event",
                )
            }
            DiagnosticsPage(
                events = pageEvents,
                totalRecords = 1_200,
                nextCursor = if (nextEvent < 1_200) cursor(nextEvent) else null,
            )
        }

        assertEquals(listOf(500, 500, 200), requestedPageSizes)
        assertEquals(1_200, events.size)
        assertEquals(0L, events.first().timestampMs)
        assertEquals(1_199L, events.last().timestampMs)
    }

    @Test
    fun `keeps reading raw pages until the requested day is collected`() {
        var pageIndex = 0

        val events = readMatchingEventsByPage(
            limit = 600,
            maxPageSize = 500,
            include = { event -> event.timestampMs >= 1_000L },
        ) { _, pageSize ->
            val pageStart = pageIndex++ * 500
            val pageEvents = List(pageSize) { offset ->
                TelemetryEvent(
                    timestampMs = (pageStart + offset).toLong(),
                    level = EventLevel.INFO,
                    category = EventCategory.APPLICATION,
                    name = "event",
                )
            }
            DiagnosticsPage(
                events = pageEvents,
                totalRecords = 1_600,
                nextCursor = if (pageStart + pageSize < 1_600) cursor(pageStart + pageSize) else null,
            )
        }

        assertEquals(600, events.size)
        assertEquals(1_000L, events.first().timestampMs)
        assertEquals(1_599L, events.last().timestampMs)
    }

    private fun cursor(offset: Int) = LogCursor(
        fileIndex = 0,
        fileId = "file",
        nextRecordOffset = offset.toLong(),
        nextSequence = offset.toLong() + 1,
    )
}
