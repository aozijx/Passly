package com.aozijx.passly.data.local.autofill

import com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId
import com.aozijx.passly.domain.autofill.model.AutofillInboxLimits
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillInboxFileManagerTest {
    private val root = Files.createTempDirectory("passly-autofill-inbox").toFile()
    private var nowMs = 1_800_000_000_000L
    private val manager = AutofillInboxFileManager(root, { nowMs })

    @After
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun enqueuePersistsOpaqueRecordsAndReturnsThemInQueueOrder() {
        val first = draftId(1)
        val second = draftId(2)
        assertTrue(manager.enqueue(KEY_ID, first, nowMs, byteArrayOf(1, 2, 3)))
        assertTrue(manager.enqueue(KEY_ID, second, nowMs + 1, byteArrayOf(4, 5)))

        val queued = manager.pending(KEY_ID)

        assertEquals(listOf(first, second), queued.map { it.draftId })
        assertEquals(listOf(nowMs, nowMs + 1), queued.map { it.queuedAtMs })
        assertArrayEquals(byteArrayOf(1, 2, 3), queued[0].encodedRecord)
        assertArrayEquals(byteArrayOf(4, 5), queued[1].encodedRecord)
        assertEquals(listOf(first.value, second.value),
            File(root, "${KEY_ID.toHex()}/pending").listFiles().orEmpty().map { it.name }.sorted())
    }

    @Test
    fun queueLimitRejectsNewRecordsWithoutEvictingExistingRecords() {
        repeat(AutofillInboxLimits.MAX_PENDING_RECORDS) { index ->
            assertTrue(manager.enqueue(KEY_ID, draftId(index), nowMs, byteArrayOf(index.toByte())))
        }

        assertFalse(manager.enqueue(KEY_ID, draftId(20), nowMs, byteArrayOf(20)))

        assertEquals(AutofillInboxLimits.MAX_PENDING_RECORDS, manager.pending(KEY_ID).size)
    }

    @Test
    fun activeByteLimitRejectsNewRecordsWithoutEvictingExistingRecords() {
        repeat(4) { index ->
            assertTrue(
                manager.enqueue(
                    KEY_ID,
                    draftId(index),
                    nowMs,
                    ByteArray(AutofillInboxLimits.MAX_RECORD_BYTES),
                ),
            )
        }

        assertFalse(
            manager.enqueue(
                KEY_ID,
                draftId(21),
                nowMs,
                byteArrayOf(1),
            ),
        )
        assertEquals(4, manager.pending(KEY_ID).size)
    }

    @Test
    fun expiredRecordsAreRemovedAndInvalidRecordsCanBeQuarantined() {
        val oldRecord = draftId(3)
        val damagedRecord = draftId(4)
        assertTrue(manager.enqueue(KEY_ID, oldRecord, nowMs, byteArrayOf(1)))
        assertTrue(manager.enqueue(KEY_ID, damagedRecord, nowMs, byteArrayOf(2)))
        manager.quarantine(KEY_ID, damagedRecord)
        nowMs += AutofillInboxLimits.PENDING_RETENTION_MS

        assertTrue(manager.pending(KEY_ID).isEmpty())
        assertTrue(File(root, "${KEY_ID.toHex()}/quarantine/${damagedRecord.value}").isFile)
    }

    @Test
    fun pendingForCurrentKeyRemovesRecordsFromStaleKeyDirectories() {
        val staleKey = ByteArray(KEY_ID.size) { 0x7f }
        val id = draftId(5)
        assertTrue(manager.enqueue(staleKey, id, nowMs, byteArrayOf(1)))
        assertTrue(manager.pending(KEY_ID).isEmpty())

        assertFalse(File(root, "${staleKey.toHex()}").exists())
    }

    @Test(expected = IllegalArgumentException::class)
    fun recordLargerThanProtocolLimitIsRejected() {
        manager.enqueue(
            keyId = KEY_ID,
            draftId = draftId(6),
            queuedAtMs = nowMs,
            encodedRecord = ByteArray(AutofillInboxLimits.MAX_RECORD_BYTES + 1),
        )
    }

    private fun draftId(value: Int): AutofillInboxDraftId =
        AutofillInboxDraftId.fromBytes(ByteArray(16) { index -> (value + index).toByte() })

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private companion object {
        val KEY_ID = ByteArray(16) { it.toByte() }
    }
}
