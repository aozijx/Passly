package com.aozijx.passly.feature.backup.internal.archive.format.text

import com.aozijx.passly.feature.backup.internal.archive.model.BackupCustomFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextVaultRendererTest {
    @Test
    fun `text export omits favorite metadata`() {
        val entry =
            BackupEntryRecord(
                id = "entry-1",
                type = "login",
                revision = 1,
                createdAt = 1L,
                updatedAt = 2L,
                fields =
                    listOf(
                        BackupFieldRecord("title", BackupFieldValue.text("Example")),
                        BackupFieldRecord("favorite", BackupFieldValue.boolean(true)),
                        BackupFieldRecord("username", BackupFieldValue.text("user")),
                    ),
            )

        val output = StringBuilder().also { TextVaultRenderer().write(listOf(entry), it) }.toString()

        assertTrue(output.contains("标题: Example"))
        assertTrue(output.contains("用户名: user"))
        assertFalse(output.contains("favorite"))
    }

    @Test
    fun `technical metadata and attachment details require explicit option`() {
        val entry = BackupEntryRecord("entry-1", "login", revision = 3, createdAt = 1L, updatedAt = 2L)
        val resource =
            BackupResourceRecord(
                id = "resource-1",
                entryId = entry.id,
                kind = BackupResourceKind.ATTACHMENT,
                fileName = "proof.pdf",
                mimeType = "application/pdf",
                size = 1_024L,
                sha256 = "hash",
            )
        val renderer = TextVaultRenderer()
        val defaultOutput = StringBuilder()
        renderer.write(listOf(entry), defaultOutput, resources = listOf(resource))

        assertFalse(defaultOutput.contains("条目 ID: entry-1"))
        assertFalse(defaultOutput.contains("附件:"))

        val technicalOutput = StringBuilder()
        renderer.write(
            listOf(entry),
            technicalOutput,
            resources = listOf(resource),
            options = TextExportOptions(includeTechnicalInfo = true),
        )

        assertTrue(technicalOutput.contains("条目 ID: entry-1"))
        assertTrue(technicalOutput.contains("版本: 3"))
        assertTrue(technicalOutput.contains("附件:"))
        assertTrue(technicalOutput.contains("proof.pdf (application/pdf, 1 KB)"))
    }

    @Test
    fun `renderer bounds notes and renders typed and custom field values`() {
        val entry =
            BackupEntryRecord(
                id = "entry-1",
                type = "login",
                revision = 1,
                createdAt = 1L,
                updatedAt = 2L,
                fields =
                    listOf(
                        BackupFieldRecord("notes", BackupFieldValue.text("first\nsecond\nthird")),
                        BackupFieldRecord("tags", BackupFieldValue.texts(listOf("work", "email"))),
                        BackupFieldRecord("enabled", BackupFieldValue.boolean(true)),
                        BackupFieldRecord("count", BackupFieldValue.integer(4)),
                        BackupFieldRecord("last_seen", BackupFieldValue.long(123L)),
                        BackupFieldRecord(
                            "custom_fields",
                            BackupFieldValue.customFields(
                                listOf(BackupCustomFieldValue("environment", "production", BackupCustomFieldValue.KIND_TEXT)),
                            ),
                        ),
                    ),
            )
        val output = StringBuilder()

        TextVaultRenderer().write(
            listOf(entry),
            output,
            options = TextExportOptions(maxNotesLines = 2),
        )

        assertTrue(output.contains("备注: first\n      second\n      ...（共 3 行）"))
        assertTrue(output.contains("标签: work, email"))
        assertTrue(output.contains("enabled: true"))
        assertTrue(output.contains("count: 4"))
        assertTrue(output.contains("last_seen: 123"))
        assertTrue(output.contains("自定义字段.environment: production"))
    }
}
