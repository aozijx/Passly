package com.aozijx.passly.feature.backup.internal.archive.format.text

import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextVaultRendererTest {
    @Test
    fun `text export omits favorite metadata`() {
        val entry = BackupEntryRecord(
            id = "entry-1",
            type = "login",
            revision = 1,
            createdAt = 1L,
            updatedAt = 2L,
            fields = listOf(
                BackupFieldRecord("title", BackupFieldValue.text("Example")),
                BackupFieldRecord("favorite", BackupFieldValue.boolean(true)),
                BackupFieldRecord("username", BackupFieldValue.text("user")),
            ),
        )

        val output = TextVaultRenderer().render(listOf(entry))

        assertTrue(output.contains("标题: Example"))
        assertTrue(output.contains("用户名: user"))
        assertFalse(output.contains("favorite"))
    }
}
