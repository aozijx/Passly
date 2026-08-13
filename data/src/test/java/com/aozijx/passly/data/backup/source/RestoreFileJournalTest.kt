package com.aozijx.passly.data.backup.source

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class RestoreFileJournalTest {

    @Test
    fun rollback_restoresPreviousFileAndRemovesNewFile() {
        val root = Files.createTempDirectory("passly-restore-journal").toFile()
        try {
            val existing = root.resolve("existing.enc").apply {
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
            val target = root.resolve("attachment.enc").apply {
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
}
