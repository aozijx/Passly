package com.aozijx.passly.data.repository.entry.command

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryIconReclaimTransactionTest {
    private lateinit var database: AppDatabase
    private lateinit var root: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.cacheDir, "icon-reclaim-test").apply { mkdirs() }
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
        root.deleteRecursively()
    }

    @Test
    fun currentReferencePreventsDeletingAnAlreadyCommittedCandidate() = runBlocking {
        val file = File(root, "current.webp").apply { writeText("fixture") }
        database.entryCommandDao().insertStrict(EntryEntity(entryId = "entry", title = "Entry", iconCustomReference = file.absolutePath))
        assertEquals(IconResourceReclaimOutcome.REFERENCED, reclaim(file.absolutePath))
        assertTrue(file.exists())
    }

    @Test
    fun historicalReferenceIsPreservedUntilTheRevisionIsRemoved() = runBlocking {
        val file = File(root, "history.webp").apply { writeText("fixture") }
        database.entryCommandDao().insertStrict(EntryEntity(entryId = "entry", title = "Entry"))
        database.entryRevisionCommandDao().insertStrict(EntryRevisionEntity(
            revisionId = "revision", entryId = "entry", version = 1,
            entryContentCipher = byteArrayOf(), sensitiveFieldCipherSet = byteArrayOf(),
            changeType = "value_changed", iconCustomReference = file.absolutePath,
        ))
        assertEquals(IconResourceReclaimOutcome.REFERENCED, reclaim(file.absolutePath))
        assertTrue(file.exists())
        database.entryRevisionCommandDao().deleteByEntryId("entry")
        assertEquals(IconResourceReclaimOutcome.COMPLETE, reclaim(file.absolutePath))
        assertFalse(file.exists())
        assertEquals(IconResourceReclaimOutcome.COMPLETE, reclaim(file.absolutePath))
    }

    @Test
    fun pruningSelectorsMatchTheRevisionsActuallyRemoved() = runBlocking {
        database.entryCommandDao().insertStrict(EntryEntity(entryId = "entry", title = "Entry"))
        for (version in 1..4) {
            database.entryRevisionCommandDao().insertStrict(EntryRevisionEntity(
                revisionId = "revision-$version", entryId = "entry", version = version,
                entryContentCipher = byteArrayOf(), sensitiveFieldCipherSet = byteArrayOf(),
                changeType = "value_changed", createdAt = 1L,
                iconCustomReference = "icon-$version",
            ))
        }
        val query = database.entryRevisionQueryDao()
        assertEquals(setOf("icon-1", "icon-2"), query.getIconReferencesBeyondEntryLimit("entry", 2).toSet())
        database.entryRevisionCommandDao().deleteOldVersions("entry", 2)
        assertEquals(setOf("icon-3", "icon-4"), query.getIconReferences("entry").toSet())
        assertEquals(listOf("icon-3"), query.getIconReferencesBeyondGlobalLimit(1))
        database.entryRevisionCommandDao().deleteOldestBeyondGlobalLimit(1)
        assertEquals(listOf("icon-4"), query.getAllIconReferences())
    }

    @Test
    fun unreferencedFilesAreDeletedButOutsidePathsAreIgnored() = runBlocking {
        val owned = File(root, "orphan.webp").apply { writeText("fixture") }
        assertEquals(IconResourceReclaimOutcome.COMPLETE, reclaim(owned.absolutePath))
        assertFalse(owned.exists())
        val outside = File(root.parentFile, "icon-reclaim-outside.webp").apply { writeText("keep") }
        try {
            assertEquals(IconResourceReclaimOutcome.COMPLETE, reclaim(File(root, "../${outside.name}").path))
            assertTrue(outside.exists())
        } finally { outside.delete() }
    }

    private suspend fun reclaim(path: String) = database.withTransaction {
        EntryIconReclaimTransaction.reclaim(database, root, path)
    }
}
