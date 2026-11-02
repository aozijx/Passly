package com.aozijx.passly.app.database.backup

import com.aozijx.passly.core.crypto.AttachmentContentProtector
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RestoredAttachmentFileTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun validExistingAttachmentIsRetainedWithoutBeingRewritten() =
        runTest {
            val content = "archive content".toByteArray()
            val target =
                temporaryFolder.newFile("existing.attachment").apply {
                    writeBytes(TestAttachmentProtector.encrypt(content))
                }
            val existingCiphertext = target.readBytes()
            val retainedPaths = mutableSetOf<String>()
            val journal = RestoreFileJournal()

            retainRestoredAttachmentFile(
                target = target,
                content = content,
                resourceId = "resource-id",
                attachmentContentProtector = TestAttachmentProtector(content),
                fileJournal = journal,
                retainedCanonicalPaths = retainedPaths,
            )

            assertArrayEquals(existingCiphertext, target.readBytes())
            assertEquals(listOf("existing.attachment"), temporaryFolder.root.listFiles()?.map { it.name })
            assertEquals(setOf(target.canonicalPath), retainedPaths)
        }

    @Test
    fun invalidExistingAttachmentIsReplacedAndRollbackRestoresOriginalFile() =
        runTest {
            val content = "archive content".toByteArray()
            val corruptCiphertext = byteArrayOf(1, 2, 3)
            val target =
                temporaryFolder.newFile("corrupt.attachment").apply {
                    writeBytes(corruptCiphertext)
                }
            val retainedPaths = mutableSetOf<String>()
            val journal = RestoreFileJournal()

            retainRestoredAttachmentFile(
                target = target,
                content = content,
                resourceId = "resource-id",
                attachmentContentProtector = TestAttachmentProtector(content),
                fileJournal = journal,
                retainedCanonicalPaths = retainedPaths,
            )

            assertArrayEquals(byteArrayOf(0x66) + content, target.readBytes())
            assertEquals(setOf(target.canonicalPath), retainedPaths)

            journal.rollback()

            assertArrayEquals(corruptCiphertext, target.readBytes())
        }

    @Test
    fun missingAttachmentIsWrittenAndRetained() =
        runTest {
            val content = "archive content".toByteArray()
            val target = File(temporaryFolder.root, "missing.attachment")
            val retainedPaths = mutableSetOf<String>()

            retainRestoredAttachmentFile(
                target = target,
                content = content,
                resourceId = "resource-id",
                attachmentContentProtector = TestAttachmentProtector(content),
                fileJournal = RestoreFileJournal(),
                retainedCanonicalPaths = retainedPaths,
            )

            assertArrayEquals(byteArrayOf(0x66) + content, target.readBytes())
            assertEquals(setOf(target.canonicalPath), retainedPaths)
        }

    @Test
    fun existingPlaintextWithInvalidContentIdIsReplaced() =
        runTest {
            val content = "archive content".toByteArray()
            val oldCiphertext = TestAttachmentProtector.encrypt(content)
            val target =
                temporaryFolder.newFile("invalid-id.attachment").apply {
                    writeBytes(oldCiphertext)
                }

            retainRestoredAttachmentFile(
                target = target,
                content = content,
                resourceId = "resource-id",
                attachmentContentProtector = TestAttachmentProtector(content, validContentId = false),
                fileJournal = RestoreFileJournal(),
                retainedCanonicalPaths = mutableSetOf(),
            )

            assertFalse(target.readBytes().contentEquals(oldCiphertext))
            assertArrayEquals(byteArrayOf(0x66) + content, target.readBytes())
        }

    private class TestAttachmentProtector(
        private val expectedContent: ByteArray,
        private val validContentId: Boolean = true,
    ) : AttachmentContentProtector {
        override suspend fun contentId(content: ByteArray): String = "resource-id"

        override suspend fun encrypt(
            content: ByteArray,
            resourceId: String,
        ): ByteArray = byteArrayOf(0x66) + content

        override suspend fun decrypt(
            encrypted: ByteArray,
            resourceId: String,
        ): ByteArray {
            require(encrypted.isNotEmpty() && encrypted[0] in byteArrayOf(0x55, 0x66)) { "Corrupt ciphertext" }
            return encrypted.copyOfRange(1, encrypted.size)
        }

        override suspend fun verifyContentId(
            content: ByteArray,
            resourceId: String,
        ): Boolean = validContentId && resourceId == "resource-id" && content.contentEquals(expectedContent)

        companion object {
            fun encrypt(content: ByteArray): ByteArray = byteArrayOf(0x55) + content
        }
    }
}
