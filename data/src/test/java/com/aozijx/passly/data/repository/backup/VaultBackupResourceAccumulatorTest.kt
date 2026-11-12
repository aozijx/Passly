package com.aozijx.passly.data.repository.backup

import com.aozijx.passly.domain.backup.model.VaultBackupReadOptions
import com.aozijx.passly.domain.backup.model.VaultBackupResource
import com.aozijx.passly.domain.backup.model.VaultBackupResourceKind
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class VaultBackupResourceAccumulatorTest {
    @Test
    fun snapshotTransferKeepsBuffersAndReleasesAccumulatorOwnership() {
        val content = byteArrayOf(2, 4, 6)
        val accumulator = VaultBackupResourceAccumulator(options(maxTotalBytes = 8L))
        accumulator.add(resource("resource", content))

        val snapshot = accumulator.toSnapshot(emptyList(), emptyList())

        assertSame(content, snapshot.resources.single().content)
        assertArrayEquals(byteArrayOf(2, 4, 6), content)
        assertEquals(0L, accumulator.accumulatedBytes)
    }

    @Test
    fun exceedingTotalLimitWipesRejectedAndAccumulatedBuffersOnCleanup() {
        val firstBytes = byteArrayOf(1, 2, 3)
        val secondBytes = byteArrayOf(4, 5, 6, 7)
        val accumulator = VaultBackupResourceAccumulator(options(maxTotalBytes = 6L))
        accumulator.add(resource("first", firstBytes))

        assertThrows(IllegalArgumentException::class.java) {
            accumulator.add(resource("second", secondBytes))
        }

        assertArrayEquals(byteArrayOf(0, 0, 0, 0), secondBytes)
        val failure = IllegalArgumentException("Later resource failed")
        val propagated = assertThrows(IllegalArgumentException::class.java) { accumulator.fail(failure) }

        assertSame(failure, propagated)
        assertArrayEquals(byteArrayOf(0, 0, 0), firstBytes)
        assertEquals(0L, accumulator.accumulatedBytes)
    }

    private fun options(maxTotalBytes: Long) =
        VaultBackupReadOptions(
            includeDeleted = true,
            includedEntryTypes = setOf(EntryType.LOGIN),
            includeIcons = true,
            includeAttachments = true,
            maxResourceBytes = 8L,
            maxTotalResourceBytes = maxTotalBytes,
        )

    private fun resource(
        id: String,
        content: ByteArray,
    ) = VaultBackupResource(
        id = id,
        entryId = EntryId("entry"),
        kind = VaultBackupResourceKind.ATTACHMENT,
        fileName = "$id.txt",
        mimeType = "text/plain",
        createdAt = null,
        content = content,
    )
}
