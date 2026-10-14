package com.aozijx.passly.app.observability

import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviousProcessExitReaderTest {
    @Test
    fun `previous exit exposes only stable non identifying fields`() {
        val cursor = InMemoryExitCursor()
        val reader = PreviousProcessExitReader(
            gateway = PreviousProcessExitGateway {
                listOf(
                    PlatformPreviousExit(
                        timestampMs = 1234,
                        reason = PreviousExitReason.ANR,
                        status = 6,
                        importance = PreviousExitImportance.FOREGROUND,
                        pssKb = 100,
                        rssKb = 200,
                    ),
                )
            },
            cursor = cursor,
        )

        val exit = reader.readUnrecorded()!!
        val propertyNames = exit::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
            .map { it.name }
            .toSet()

        assertEquals(
            setOf("timestampMs", "reason", "status", "importance", "pssKb", "rssKb"),
            propertyNames,
        )
        listOf("description", "processName", "uid", "pid", "trace", "packageName").forEach {
            assertFalse(exit.toString().contains(it, ignoreCase = true))
        }
    }

    @Test
    fun `same previous exit timestamp is emitted once`() {
        val cursor = InMemoryExitCursor()
        val gateway = PreviousProcessExitGateway {
            listOf(
                PlatformPreviousExit(
                    timestampMs = 5678,
                    reason = PreviousExitReason.CRASH,
                    status = 1,
                    importance = PreviousExitImportance.CACHED,
                    pssKb = 10,
                    rssKb = 20,
                ),
            )
        }
        val reader = PreviousProcessExitReader(gateway, cursor)

        assertEquals(5678L, reader.readUnrecorded()?.timestampMs)
        assertNull(reader.readUnrecorded())
        assertEquals(5678L, cursor.timestampMs)
    }

    @Test
    fun `invalid samples are ignored without advancing cursor`() {
        val cursor = InMemoryExitCursor()
        val reader = PreviousProcessExitReader(
            PreviousProcessExitGateway {
                listOf(
                    PlatformPreviousExit(
                        timestampMs = -1,
                        reason = PreviousExitReason.UNKNOWN,
                        status = 0,
                        importance = PreviousExitImportance.UNKNOWN,
                        pssKb = -1,
                        rssKb = -1,
                    ),
                )
            },
            cursor,
        )

        assertNull(reader.readUnrecorded())
        assertEquals(0, cursor.timestampMs)
    }

    private class InMemoryExitCursor : PreviousExitCursor {
        var timestampMs = 0L
        override fun readTimestampMs(): Long = timestampMs
        override fun writeTimestampMs(timestampMs: Long) {
            this.timestampMs = timestampMs
        }
    }
}
