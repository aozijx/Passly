package com.aozijx.passly.app.database.backup

import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord
import com.aozijx.passly.feature.backup.internal.archive.snapshot.RestoreFileJournal
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RoomBackupFaviconResourceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun webpRoundTripPreservesMetadataBytesAndRewritesLocalPath() {
        val sourceRoot = temporaryFolder.newFolder("source-icons")
        val restoredRoot = temporaryFolder.newFolder("restored-icons")
        val sourceBytes = byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x01, 0x02, 0x03)
        val source = File(sourceRoot, "private-icon.webp").apply {
            writeBytes(sourceBytes)
        }

        val exported = requireNotNull(
            RoomBackupFaviconResource.export(
                entryId = "entry-1",
                iconPath = source.absolutePath,
                iconRoot = sourceRoot,
            )
        )

        assertEquals(BackupResourceKind.ICON, exported.record.kind)
        assertEquals("image/webp", exported.record.mimeType)
        assertEquals("private-icon.webp", exported.record.fileName)
        assertFalse(exported.record.toString().contains(sourceRoot.absolutePath))
        assertArrayEquals(sourceBytes, exported.content)

        val journal = RestoreFileJournal()
        val restored = RoomBackupFaviconResource.restore(
            record = exported.record,
            content = exported.content,
            iconRoot = restoredRoot,
            fileJournal = journal,
        )
        journal.commit()

        assertEquals("webp", restored.extension)
        assertEquals(restoredRoot.canonicalFile, restored.canonicalFile.parentFile)
        assertTrue(restored.canonicalPath != source.canonicalPath)
        assertArrayEquals(sourceBytes, restored.readBytes())
    }

    @Test
    fun nonWebpIconsAreRejectedByTheStorageContract() {
        val sourceRoot = temporaryFolder.newFolder("source-png-icons")
        val source = File(sourceRoot, "legacy-icon.png").apply { writeBytes(byteArrayOf(1)) }

        assertThrows(IllegalArgumentException::class.java) {
            RoomBackupFaviconResource.export(
                entryId = "entry-1",
                iconPath = source.absolutePath,
                iconRoot = sourceRoot,
            )
        }

        val record = BackupResourceRecord(
            id = "icon_entry-1",
            entryId = "entry-1",
            kind = BackupResourceKind.ICON,
            fileName = "legacy-icon.png",
            mimeType = "image/png",
            size = 1,
            sha256 = "00",
        )
        assertThrows(IllegalArgumentException::class.java) {
            RoomBackupFaviconResource.restore(
                record = record,
                content = byteArrayOf(1),
                iconRoot = temporaryFolder.newFolder("restored-png-icons"),
                fileJournal = RestoreFileJournal(),
            )
        }
    }
}
