package com.aozijx.passly.domain.backup.model

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class VaultBackupSnapshotTest {
    @Test
    fun resourceKeepsOpaqueIdMetadataKindAndContentBuffer() {
        val content = byteArrayOf(4, 8, 15)
        val attachment =
            VaultBackupResource(
                id = "attachment-id",
                entryId = EntryId("entry-id"),
                kind = VaultBackupResourceKind.ATTACHMENT,
                fileName = "recovery.txt",
                mimeType = "text/plain",
                createdAt = 123L,
                content = content,
            )

        assertEquals("attachment-id", attachment.id)
        assertEquals(EntryId("entry-id"), attachment.entryId)
        assertEquals(VaultBackupResourceKind.ATTACHMENT, attachment.kind)
        assertEquals("recovery.txt", attachment.fileName)
        assertEquals("text/plain", attachment.mimeType)
        assertEquals(123L, attachment.createdAt)
        assertSame(content, attachment.content)
    }

    @Test
    fun resourceRejectsBlankId() {
        assertThrows(IllegalArgumentException::class.java) {
            VaultBackupResource(
                id = " ",
                entryId = EntryId("entry-id"),
                kind = VaultBackupResourceKind.ICON,
                fileName = null,
                mimeType = null,
                createdAt = null,
                content = byteArrayOf(),
            )
        }
    }

    @Test
    fun readOptionsKeepRequestedEntryTypesAndResourceFilter() {
        val options =
            VaultBackupReadOptions(
                includeDeleted = true,
                includedEntryTypes = setOf(EntryType.LOGIN, EntryType.OTP),
                includeIcons = false,
                includeAttachments = false,
                maxResourceBytes = 1024L,
            )

        assertEquals(true, options.includeDeleted)
        assertEquals(setOf(EntryType.LOGIN, EntryType.OTP), options.includedEntryTypes)
        assertEquals(false, options.includeIcons)
        assertEquals(false, options.includeAttachments)
        assertEquals(1024L, options.maxResourceBytes)
    }

    @Test
    fun restoreResultRejectsNegativeCounts() {
        assertThrows(IllegalArgumentException::class.java) {
            VaultBackupRestoreResult(importedEntryCount = -1, existingEntryCount = 0)
        }
    }
}
