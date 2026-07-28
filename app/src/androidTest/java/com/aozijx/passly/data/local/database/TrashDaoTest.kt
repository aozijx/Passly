package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.model.entity.EntryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrashDaoTest {
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
    fun trash_restore_andPermanentDelete_requireCurrentVersion() = runBlocking {
        val queryDao = database.entryQueryDao()
        val commandDao = database.entryCommandDao()
        commandDao.insertStrict(entry("entry-1"))

        assertEquals(
            1,
            commandDao.optimisticSoftDelete(
                entryId = "entry-1",
                expectedVersion = 1,
                deletedAt = 10L,
                updatedAt = 10L
            )
        )
        assertEquals(listOf("entry-1"), queryDao.observeDeleted().first().map { it.entryId })
        assertEquals(0, commandDao.deleteDeletedOptimistic("entry-1", expectedVersion = 1))
        assertEquals(1, commandDao.restoreOptimistic("entry-1", expectedVersion = 2, now = 20L))
        assertEquals(emptyList<EntryEntity>(), queryDao.observeDeleted().first())

        assertEquals(
            1,
            commandDao.optimisticSoftDelete(
                entryId = "entry-1",
                expectedVersion = 3,
                deletedAt = 30L,
                updatedAt = 30L
            )
        )
        assertEquals(1, commandDao.deleteDeletedOptimistic("entry-1", expectedVersion = 4))
        assertNull(queryDao.getById("entry-1"))
    }

    @Test
    fun emptyTrash_keepsActiveEntries() = runBlocking {
        val queryDao = database.entryQueryDao()
        val commandDao = database.entryCommandDao()
        commandDao.insertStrict(entry("active"))
        commandDao.insertStrict(entry("deleted"))
        commandDao.optimisticSoftDelete(
            entryId = "deleted",
            expectedVersion = 1,
            deletedAt = 10L,
            updatedAt = 10L
        )

        assertEquals(1, commandDao.deleteAllDeleted())
        assertEquals(listOf("active"), queryDao.getAll().map { it.entryId })
    }

    private fun entry(id: String) = EntryEntity(
        entryId = id,
        summaryBlob = byteArrayOf(1, 2, 3)
    )
}
