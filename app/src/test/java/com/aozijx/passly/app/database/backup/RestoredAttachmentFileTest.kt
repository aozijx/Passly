package com.aozijx.passly.app.database.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RestoredAttachmentFileTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun existingAttachmentIsRetainedWithoutBeingRewritten() {
        val target = temporaryFolder.newFile("existing.attachment").apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }
        val retainedPaths = mutableSetOf<String>()
        var writeCalled = false

        retainRestoredAttachmentFile(target, retainedPaths) {
            writeCalled = true
        }

        assertFalse(writeCalled)
        assertEquals(setOf(target.canonicalPath), retainedPaths)
    }
}
