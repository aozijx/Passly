package com.aozijx.passly.app.database.backup

import com.aozijx.passly.domain.backup.model.VaultBackupResource
import com.aozijx.passly.domain.backup.model.VaultBackupResourceKind
import com.aozijx.passly.domain.backup.model.VaultBackupSnapshot
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupSnapshotArchiveMapperTest {
    @Test
    fun mappingFailureWipesAllDomainResourceBuffers() {
        val entry = account("entry")
        val firstBytes = byteArrayOf(1, 2, 3)
        val secondBytes = byteArrayOf(4, 5, 6)
        val snapshot =
            VaultBackupSnapshot(
                entries = listOf(entry),
                links = emptyList(),
                resources =
                    listOf(
                        icon(entry.id, "first", firstBytes),
                        icon(entry.id, "second", secondBytes),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            BackupSnapshotArchiveMapper.toBundle(
                snapshot = snapshot,
                appVersion = { "1.0" },
                exportedAt = 10L,
            )
        }

        assertArrayEquals(byteArrayOf(0, 0, 0), firstBytes)
        assertArrayEquals(byteArrayOf(0, 0, 0), secondBytes)
    }

    private fun icon(
        entryId: EntryId,
        id: String,
        content: ByteArray,
    ) = VaultBackupResource(
        id = id,
        entryId = entryId,
        kind = VaultBackupResourceKind.ICON,
        fileName = "$id.webp",
        mimeType = "image/webp",
        createdAt = null,
        content = content,
    )

    private fun account(id: String) =
        Entry(
            identity =
                EntryIdentity(
                    id = EntryId(id),
                    type = EntryType.ACCOUNT,
                    version = EntryVersion.INITIAL,
                    timestamps = EntryTimestamps(createdAtMs = 1L, updatedAtMs = 2L),
                ),
            profile = EntryProfile("Account"),
        )
}
