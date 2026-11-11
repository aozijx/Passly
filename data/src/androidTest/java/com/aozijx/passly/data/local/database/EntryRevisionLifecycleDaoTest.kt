package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.domain.entry.model.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryRevisionLifecycleDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            TestDatabaseFactory
                .inMemoryDatabaseBuilder(context)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun perEntryRetention_keepsNewestFiftyVersions() =
        runBlocking {
            insertEntry("entry-a")
            (1..51).forEach { version ->
                database.entryRevisionCommandDao().insertStrict(
                    revision("entry-a", version, createdAt = version.toLong()),
                )
            }

            assertEquals(
                1,
                database.entryRevisionCommandDao().deleteOldVersions("entry-a", keepCount = 50),
            )
            assertEquals(
                (51 downTo 2).toList(),
                database
                    .entryRevisionQueryDao()
                    .observeByEntryId("entry-a")
                    .first()
                    .map { it.version },
            )
        }

    @Test
    fun globalRetention_prunesOldestAcrossEntriesWithStableTieBreak() =
        runBlocking {
            insertEntry("entry-a")
            insertEntry("entry-b")
            val revisions =
                listOf(
                    revision("entry-a", version = 1, createdAt = 10L, revisionId = "revision-1"),
                    revision("entry-a", version = 2, createdAt = 20L, revisionId = "revision-3"),
                    revision("entry-b", version = 1, createdAt = 20L, revisionId = "revision-2"),
                    revision("entry-b", version = 2, createdAt = 30L, revisionId = "revision-4"),
                )
            revisions.forEach { database.entryRevisionCommandDao().insertStrict(it) }

            assertEquals(
                2,
                database.entryRevisionCommandDao().deleteOldestBeyondGlobalLimit(keepCount = 2),
            )
            assertEquals(
                2,
                database
                    .entryRevisionQueryDao()
                    .observeByEntryId("entry-a")
                    .first()
                    .size +
                    database
                        .entryRevisionQueryDao()
                        .observeByEntryId("entry-b")
                        .first()
                        .size,
            )
            assertEquals(
                listOf("revision-3"),
                database
                    .entryRevisionQueryDao()
                    .observeByEntryId("entry-a")
                    .first()
                    .map { it.revisionId },
            )
            assertEquals(
                listOf("revision-4"),
                database
                    .entryRevisionQueryDao()
                    .observeByEntryId("entry-b")
                    .first()
                    .map { it.revisionId },
            )
        }

    @Test
    fun softDeleteRetainsHistory_butPermanentDeleteRemovesItInSameTransaction() =
        runBlocking {
            insertEntry("entry-a")
            database.entryRevisionCommandDao().insertStrict(revision("entry-a", version = 1))

            assertEquals(
                1,
                database.entryCommandDao().optimisticSoftDelete(
                    entryId = "entry-a",
                    expectedVersion = 1,
                    deletedAt = 10L,
                    updatedAt = 10L,
                ),
            )
            assertEquals(
                1,
                database
                    .entryRevisionQueryDao()
                    .observeByEntryId("entry-a")
                    .first()
                    .size,
            )

            database.withTransaction {
                assertEquals(1, database.entryRevisionCommandDao().deleteByEntryId("entry-a"))
                assertEquals(
                    1,
                    database.entryCommandDao().deleteDeletedOptimistic(
                        entryId = "entry-a",
                        expectedVersion = 2,
                    ),
                )
            }

            assertEquals(
                0,
                database
                    .entryRevisionQueryDao()
                    .observeByEntryId("entry-a")
                    .first()
                    .size,
            )
        }

    @Test
    fun emptyTrashHistoryDelete_keepsActiveEntryHistory() =
        runBlocking {
            insertEntry("active")
            insertEntry("deleted")
            listOf(revision("active", 1), revision("deleted", 1)).forEach {
                database.entryRevisionCommandDao().insertStrict(it)
            }
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

            assertEquals("active", database.entryQueryDao().getById("active")?.entryId)
            assertNull(database.entryQueryDao().getById("deleted"))
            assertEquals(
                listOf("active"),
                database
                    .entryRevisionQueryDao()
                    .observeByEntryId("active")
                    .first()
                    .map { it.entryId },
            )
            assertEquals(
                1,
                database
                    .entryRevisionQueryDao()
                    .observeByEntryId("active")
                    .first()
                    .size +
                    database
                        .entryRevisionQueryDao()
                        .observeByEntryId("deleted")
                        .first()
                        .size,
            )
        }

    private suspend fun insertEntry(id: String) {
        database.entryCommandDao().insertStrict(
            EntryEntity(
                entryId = id,
                entryType = EntryType.LOGIN,
                title = "entry",
            ),
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
