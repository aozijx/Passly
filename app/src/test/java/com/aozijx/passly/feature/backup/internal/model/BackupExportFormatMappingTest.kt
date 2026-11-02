package com.aozijx.passly.feature.backup.internal.model

import com.aozijx.passly.feature.backup.contract.BackupExportFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupExportFormatMappingTest {
    @Test
    fun `every presentation export choice maps to its stable archive format`() {
        assertEquals(BackupFormats.PASSLY_ENCRYPTED, BackupExportFormat.ENCRYPTED.toFormatId())
        assertEquals(BackupFormats.PASSLY_JSON, BackupExportFormat.JSON.toFormatId())
        assertEquals(BackupFormats.READABLE_TEXT, BackupExportFormat.TEXT.toFormatId())
        assertEquals(
            BackupExportFormat.entries.toSet(),
            setOf(
                BackupExportFormat.ENCRYPTED,
                BackupExportFormat.JSON,
                BackupExportFormat.TEXT,
            ),
        )
    }
}
