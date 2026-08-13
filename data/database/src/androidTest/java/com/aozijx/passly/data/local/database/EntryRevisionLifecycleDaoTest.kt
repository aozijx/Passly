package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import com.aozijx.passly.domain.entry.model.EntryType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryRevisionLifecycleDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun perEntryRetention_keepsNewestFiftyVersions() = runBlocking {
        insertEntry("entry-a")
        (1..51).forEach { version ->
            database.entryRevisionCommandDao().insertStrict(
                revision("entry-a", version, createdAt = version.toLong())
            )
        }

        assertEquals(
            1,
            database.entryRevisionCommandDao().deleteOldVersions("entry-a", keepCount = 50)
        )
        assertEquals(
            (51 downTo 2).toList(),
            database.entryRevisionQueryDao().getByEntryId("entry-a").map { it.version }
        )
    }

    @Test
    fun globalRetention_prunesOldestAcrossEntriesWithStableTieBreak() = runBlocking {
        insertEntry("entry-a")
        insertEntry("entry-b")
        val revisions = listOf(
            revision("entry-a", version = 1, createdAt = 10L, revisionId = "revision-1"),
            revision("entry-b", version = 1, createdAt = 20L, revisionId = "revision-2"),
            revision("entry-a", version = 2, createdAt = 30L, revisionId = "revision-3"),
            revision("entry-b", version = 2, createdAt = 30L, revisionId = "revision-4"),
        )
        database.entryRevisionCommandDao().insertAllStrict(revisions)

        assertEquals(
            2,
            database.entryRevisionCommandDao().deleteOldestBeyondGlobalLimit(keepCount = 2)
        )
        assertEquals(2, database.entryRevisionQueryDao().countAll())
        assertEquals(
            listOf("revision-3"),
            database.entryRevisionQueryDao().getByEntryId("entry-a").map { it.revisionId }
        )
        assertEquals(
            listOf("revision-4"),
            database.entryRevisionQueryDao().getByEntryId("entry-b").map { it.revisionId }
        )
    }

    @Test
    fun softDeleteRetainsHistory_butPermanentDeleteRemovesItInSameTransaction() = runBlocking {
        insertEntry("entry-a")
        database.entryRevisionCommandDao().insertStrict(revision("entry-a", version = 1))

        assertEquals(
            1,
            database.entryCommandDao().optimisticSoftDelete(
                entryId = "entry-a",
                expectedVersion = 1,
                deletedAt = 10L,
                updatedAt = 10L,
            )
        )
        assertEquals(1, database.entryRevisionQueryDao().getByEntryId("entry-a").size)

        database.withTransaction {
            assertEquals(1, database.entryRevisionCommandDao().deleteByEntryId("entry-a"))
            assertEquals(
                1,
                database.entryCommandDao().deleteDeletedOptimistic(
                    entryId = "entry-a",
                    expectedVersion = 2,
                )
            )
        }

        assertEquals(0, database.entryRevisionQueryDao().countAll())
    }

    @Test
    fun emptyTrashHistoryDelete_keepsActiveEntryHistory() = runBlocking {
        insertEntry("active")
        insertEntry("deleted")
        database.entryRevisionCommandDao().insertAllStrict(
            listOf(revision("active", 1), revision("deleted", 1))
        )
        database.entryCommandDao().optimisticSoftDelete(
            entryId = "deleted",
            expectedVersion = 1,
            deletedAt = 10L,
            updatedAt = 10L,
        )

        database.withTransaction {
            assertEquals(1, database.entryRevisionCommandDao().deleteForDeletedEntries())
            assertEquals(1, database.entryCommandDao().deleteAllDeleted())
        }

        assertEquals(
            listOf("active"),
            database.entryRevisionQueryDao().getByEntryId("active").map { it.entryId }
        )
        assertEquals(1, database.entryRevisionQueryDao().countAll())
    }

    private suspend fun insertEntry(id: String) {
        database.entryCommandDao().insertStrict(
            EntryEntity(
                entryId = id,
                entryType = EntryType.LOGIN,
                summaryBlob = byteArrayOf(1),
            )
        )
    }

    private fun revision(
        entryId: String,
        version: Int,
        createdAt: Long = version.toLong(),
        revisionId: String = "$entryId-revision-$version",
    ) = EntryRevisionEntity(
        revisionId = revisionId,
        entryId = entryId,
        version = version,
        entryContentCipher = byteArrayOf(version.toByte()),
        sensitiveFieldCipherSet = byteArrayOf(),
        changeType = "value_changed",
        createdAt = createdAt,
    )
}
