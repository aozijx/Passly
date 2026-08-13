package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.data.model.entity.EntryLinkEntity
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.link.EntryRelationType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryLinkConstraintDaoTest {
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
    fun endpointTripleIsUnique_andDeletingEndpointCascadesLink() = runBlocking {
        database.entryCommandDao().insertStrict(entry("login", EntryType.LOGIN))
        database.entryCommandDao().insertStrict(entry("account", EntryType.ACCOUNT))
        database.entryLinkCommandDao().upsert(link("link-1"))

        val duplicate = runCatching {
            database.entryLinkCommandDao().upsert(link("link-2"))
        }
        assertTrue(duplicate.isFailure)

        assertEquals(1, database.entryCommandDao().deleteById("account"))
        assertTrue(database.entryLinkQueryDao().getAll().isEmpty())
    }

    private fun entry(id: String, type: EntryType) = EntryEntity(
        entryId = id,
        entryType = type,
        summaryBlob = byteArrayOf(),
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun link(id: String) = EntryLinkEntity(
        linkId = id,
        sourceEntryId = "login",
        targetEntryId = "account",
        relationType = EntryRelationType.MEMBER_OF_ACCOUNT,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
