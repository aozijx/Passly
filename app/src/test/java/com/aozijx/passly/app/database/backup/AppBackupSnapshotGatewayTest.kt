package com.aozijx.passly.app.database.backup

import android.content.ContextWrapper
import com.aozijx.passly.domain.backup.model.VaultBackupReadOptions
import com.aozijx.passly.domain.backup.model.VaultBackupResource
import com.aozijx.passly.domain.backup.model.VaultBackupResourceKind
import com.aozijx.passly.domain.backup.model.VaultBackupRestoreMode
import com.aozijx.passly.domain.backup.model.VaultBackupRestoreResult
import com.aozijx.passly.domain.backup.model.VaultBackupSnapshot
import com.aozijx.passly.domain.backup.port.VaultBackupStore
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.feature.backup.contract.ImportMode
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotReadOptions
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class AppBackupSnapshotGatewayTest {
    @Test
    fun restoreMapsStableIconKeyToDomainResourceAndTransfersItsBytes() = runTest {
        val entry = account("entry")
        val content = byteArrayOf(3, 4, 5)
        val bundle =
            BackupSnapshotArchiveMapper.toBundle(
                snapshot =
                    VaultBackupSnapshot(
                        entries = listOf(entry),
                        links = emptyList(),
                        resources = listOf(icon(entry.id, content)),
                    ),
                appVersion = { "1.0" },
                exportedAt = 10L,
            )
        val archiveResource = bundle.document.resources.single()
        assertEquals("icon_entry", archiveResource.id)

        val store = RecordingVaultBackupStore()
        val gateway = AppBackupSnapshotGateway(store, ContextWrapper(null))
        val result = gateway.restore(bundle, ImportMode.APPEND)

        assertEquals(1, result.importedEntryCount)
        assertEquals(0, result.existingEntryCount)
        assertEquals(VaultBackupRestoreMode.APPEND, store.restoredMode)
        val restored = requireNotNull(store.restoredSnapshot)
        assertEquals("icon:entry", restored.resources.single().id)
        assertSame(content, restored.resources.single().content)
        assertArrayEquals(byteArrayOf(3, 4, 5), restored.resources.single().content)
    }

    @Test
    fun overwriteWithCompatibilityFilteredEntriesNeverCallsStore() = runTest {
        val bundle =
            BackupBundle(
                document =
                    BackupDocument(
                        format = BackupDocument.FORMAT,
                        version = BackupDocument.CURRENT_VERSION,
                        exportedAt = 10L,
                        entries = emptyList(),
                    ),
                sourceEntryCount = 1,
            )
        val store = RecordingVaultBackupStore()
        val gateway = AppBackupSnapshotGateway(store, ContextWrapper(null))

        try {
            gateway.restore(bundle, ImportMode.OVERWRITE)
            fail("Expected overwrite protection to reject a filtered nonempty source")
        } catch (_: IllegalArgumentException) {
            // Expected: this guard runs before validation or persistence.
        }

        assertEquals(0, store.restoreCalls)
        assertEquals(null, store.restoredSnapshot)
    }

    private fun icon(
        entryId: EntryId,
        content: ByteArray,
    ) =
        VaultBackupResource(
            id = "icon:${entryId.value}",
            entryId = entryId,
            kind = VaultBackupResourceKind.ICON,
            fileName = "icon.webp",
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

    private class RecordingVaultBackupStore : VaultBackupStore {
        var restoredSnapshot: VaultBackupSnapshot? = null
        var restoredMode: VaultBackupRestoreMode? = null
        var restoreCalls: Int = 0

        override suspend fun read(options: VaultBackupReadOptions): VaultBackupSnapshot =
            error("read is not used by these tests")

        override suspend fun restore(
            snapshot: VaultBackupSnapshot,
            mode: VaultBackupRestoreMode,
        ): VaultBackupRestoreResult {
            restoreCalls++
            restoredSnapshot = snapshot
            restoredMode = mode
            return VaultBackupRestoreResult(importedEntryCount = snapshot.entries.size, existingEntryCount = 0)
        }
    }
}
