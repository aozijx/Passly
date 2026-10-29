package com.aozijx.passly.app.observability

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiagnosticsExportFileRetentionTest {
    private lateinit var cacheDirectory: File

    @Before
    fun setUp() {
        cacheDirectory = Files.createTempDirectory("passly-diagnostics-retention").toFile()
    }

    @After
    fun tearDown() {
        cacheDirectory.deleteRecursively()
    }

    @Test
    fun `only diagnostic exports older than the sharing window are stale`() {
        val nowMs = 1_700_000_000_000L
        val stale = file("passly_logs_old.jsonl", modifiedAt = nowMs - DiagnosticsExportFileRetention.MAX_AGE_MS - 1)
        val active = file("passly_logs_active.zip", modifiedAt = nowMs - DiagnosticsExportFileRetention.MAX_AGE_MS + 1)
        val unrelated = file("other_export.jsonl", modifiedAt = nowMs - DiagnosticsExportFileRetention.MAX_AGE_MS - 1)

        val staleFiles = DiagnosticsExportFileRetention.staleFiles(cacheDirectory.listFiles(), nowMs)

        assertEquals(listOf(stale), staleFiles)
        assertTrue(active.exists())
        assertTrue(unrelated.exists())
    }

    @Test
    fun `missing cache listing has no stale exports`() {
        assertTrue(DiagnosticsExportFileRetention.staleFiles(null, nowMs = 1_700_000_000_000L).isEmpty())
    }

    private fun file(name: String, modifiedAt: Long): File = File(cacheDirectory, name).apply {
        writeText("export")
        check(setLastModified(modifiedAt))
    }
}
