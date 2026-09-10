package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.domain.entry.model.EntryType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutofillEntryQueryDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun autofillCandidates_matchStructuredFieldsAndExcludeIneligibleEntries() = runBlocking {
        insert(entry("application", applicationIds = setOf("COM.Example.App")))
        insert(entry("domain", domains = setOf("example.com")))
        insert(entry("url", primaryUrl = "https://www.example.com/login"))
        insert(entry("deleted", domains = setOf("example.com"), deletedAt = 2L))
        insert(entry("note", type = EntryType.NOTE, domains = setOf("example.com")))

        assertEquals(
            listOf("application"),
            database.entryQueryDao()
                .getAutofillCandidatesByApplicationId("com.example.app", 10)
                .map { it.entryId },
        )
        assertEquals(
            listOf("url", "domain"),
            database.entryQueryDao()
                .getAutofillCandidatesByDomain("example.com", 10)
                .map { it.entryId },
        )
    }

    private suspend fun insert(entry: EntryEntity) =
        database.entryCommandDao().insertStrict(entry)

    private fun entry(
        id: String,
        type: EntryType = EntryType.LOGIN,
        primaryUrl: String? = null,
        domains: Set<String> = emptySet(),
        applicationIds: Set<String> = emptySet(),
        deletedAt: Long? = null,
    ) = EntryEntity(
        entryId = id,
        entryType = type,
        title = id,
        primaryUrl = primaryUrl,
        domains = domains,
        applicationIds = applicationIds,
        createdAt = 1L,
        updatedAt = if (id == "url") 3L else 1L,
        deletedAt = deletedAt,
    )
}
