package com.aozijx.passly.app.database.backup

import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreFileJournalTest {
    @Test
    fun rollback_restoresPreviousFileAndRemovesNewFile() {
        val root = Files.createTempDirectory("passly-restore-journal").toFile()
        try {
            val existing =
                root.resolve("existing.enc").apply {
                    writeBytes("old".toByteArray())
                }
            val created = root.resolve("created.enc")
            val journal = RestoreFileJournal()

            journal.replace(existing, "new".toByteArray())
            journal.replace(created, "created".toByteArray())
            journal.rollback()

            assertArrayEquals("old".toByteArray(), existing.readBytes())
            assertEquals(false, created.exists())
            assertEquals(listOf("existing.enc"), root.listFiles()?.map { it.name })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun commit_keepsReplacementAndDeletesJournalFiles() {
        val root = Files.createTempDirectory("passly-restore-journal").toFile()
        try {
            val target =
                root.resolve("attachment.enc").apply {
                    writeBytes("old".toByteArray())
                }
            val journal = RestoreFileJournal()

            journal.replace(target, "new".toByteArray())
            journal.commit()

            assertArrayEquals("new".toByteArray(), target.readBytes())
            assertEquals(listOf("attachment.enc"), root.listFiles()?.map { it.name })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rollbackFailure_keepsChangeForRetry() {
        val root = Files.createTempDirectory("passly-restore-journal").toFile()
        try {
            val target = root.resolve("attachment.enc").apply { writeBytes("old".toByteArray()) }
            val operations = FailingOperations().apply { failTargetDelete = target }
            val journal = RestoreFileJournal(operations)

            journal.replace(target, "new".toByteArray())

            assertThrows(IOException::class.java) { journal.rollback() }
            assertArrayEquals("new".toByteArray(), target.readBytes())
            assertArrayEquals("old".toByteArray(), root.previousFor(target).readBytes())

            journal.rollback()

            assertArrayEquals("old".toByteArray(), target.readBytes())
            assertEquals(listOf("attachment.enc"), root.listFiles()?.map { it.name })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun commitFailure_keepsPreviousForRetryWithoutChangingTarget() {
        val root = Files.createTempDirectory("passly-restore-journal").toFile()
        try {
            val target = root.resolve("attachment.enc").apply { writeBytes("old".toByteArray()) }
            val operations = FailingOperations().apply { failPreviousDelete = true }
            val journal = RestoreFileJournal(operations)

            journal.replace(target, "new".toByteArray())

            assertThrows(IOException::class.java) { journal.commit() }
            assertArrayEquals("new".toByteArray(), target.readBytes())
            assertArrayEquals("old".toByteArray(), root.previousFor(target).readBytes())

            journal.commit()

            assertArrayEquals("new".toByteArray(), target.readBytes())
            assertEquals(listOf("attachment.enc"), root.listFiles()?.map { it.name })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun replaceFailure_keepsUnrestoredPreviousForRollbackRetry() {
        val root = Files.createTempDirectory("passly-restore-journal").toFile()
        try {
            val target = root.resolve("attachment.enc").apply { writeBytes("old".toByteArray()) }
            val operations =
                FailingOperations().apply {
                    failPromotion = true
                    failPreviousRestore = true
                }
            val journal = RestoreFileJournal(operations)

            val failure =
                assertThrows(IOException::class.java) {
                    journal.replace(target, "new".toByteArray())
                }

            assertTrue(failure.suppressed.isNotEmpty())
            assertFalse(target.exists())
            assertArrayEquals("old".toByteArray(), root.previousFor(target).readBytes())

            journal.rollback()

            assertArrayEquals("old".toByteArray(), target.readBytes())
            assertEquals(listOf("attachment.enc"), root.listFiles()?.map { it.name })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun commitFailure_prefersFatalErrorOverEarlierCleanupException() {
        val root = Files.createTempDirectory("passly-restore-journal").toFile()
        try {
            val first = root.resolve("first.enc").apply { writeBytes("first-old".toByteArray()) }
            val second = root.resolve("second.enc").apply { writeBytes("second-old".toByteArray()) }
            val operations = FailingOperations().apply { failFirstPreviousDeleteThenThrowFatal = true }
            val journal = RestoreFileJournal(operations)

            journal.replace(first, "first-new".toByteArray())
            journal.replace(second, "second-new".toByteArray())

            val failure =
                assertThrows(AssertionError::class.java) { journal.commit() }

            assertTrue(failure.suppressed.any { it is IOException })
            assertArrayEquals("first-new".toByteArray(), first.readBytes())
            assertArrayEquals("second-new".toByteArray(), second.readBytes())

            journal.commit()

            assertEquals(setOf("first.enc", "second.enc"), root.listFiles()?.map { it.name }?.toSet())
        } finally {
            root.deleteRecursively()
        }
    }

    private class FailingOperations : RestoreFileOperations by SystemRestoreFileOperations {
        var failTargetDelete: File? = null
        var failPreviousDelete = false
        var failPromotion = false
        var failPreviousRestore = false
        var failFirstPreviousDeleteThenThrowFatal = false
        private var previousDeleteCount = 0

        override fun delete(file: File): Boolean {
            if (file == failTargetDelete) {
                failTargetDelete = null
                return false
            }
            if (file.name.endsWith(".previous") && failPreviousDelete) {
                failPreviousDelete = false
                return false
            }
            if (file.name.endsWith(".previous") && failFirstPreviousDeleteThenThrowFatal) {
                previousDeleteCount++
                if (previousDeleteCount == 1) return false
                if (previousDeleteCount == 2) throw AssertionError("fatal cleanup failure")
            }
            return SystemRestoreFileOperations.delete(file)
        }

        override fun rename(
            source: File,
            target: File,
        ): Boolean {
            if (source.name.endsWith(".importing") && failPromotion) {
                failPromotion = false
                return false
            }
            if (source.name.endsWith(".previous") && failPreviousRestore) {
                failPreviousRestore = false
                return false
            }
            return SystemRestoreFileOperations.rename(source, target)
        }
    }

    private fun File.previousFor(target: File): File =
        listFiles()!!.single { it.name.startsWith(".${target.name}.") && it.name.endsWith(".previous") }
}
