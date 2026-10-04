package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyedBackupWireModelTest {
    @Test
    fun documentUsesNewFormatVersionOneAndKeyedFields() {
        val document = BackupDocument(
            format = BackupDocument.FORMAT,
            version = BackupDocument.CURRENT_VERSION,
            exportedAt = 10L,
            entries = listOf(
                BackupEntryRecord(
                    id = "entry-1",
                    type = "login",
                    revision = 3,
                    createdAt = 1L,
                    updatedAt = 2L,
                    fields = listOf(
                        BackupFieldRecord("title", BackupFieldValue.text("Example")),
                        BackupFieldRecord("favorite", BackupFieldValue.boolean(true)),
                    ),
                ),
            ),
        )

        val json = BackupJson.encodeToString(document)

        assertTrue(json.contains("\"format\": \"passly-field-archive\""))
        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("\"revision\": 3"))
        assertTrue(json.contains("\"kind\": \"text\""))
        assertTrue(json.contains("\"text\": \"Example\""))
        assertTrue(json.contains("\"boolean\": true"))
        assertFalse(json.contains("\"summary\""))
        assertFalse(json.contains("\"secret\""))
        assertFalse(json.contains("\"sensitiveFields\""))
    }
}
