package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.domain.entry.model.EntryType
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
    fun endpointPairIsUnique_andDeletingEndpointCascadesLink() =
        runBlocking {
            database.entryCommandDao().insertStrict(entry("login", EntryType.LOGIN))
            database.entryCommandDao().insertStrict(entry("account", EntryType.ACCOUNT))
            database.entryLinkCommandDao().upsert(link("link-1"))

            val duplicateFailure =
                runCatching {
                    database.entryLinkCommandDao().upsert(link("link-2"))
                }.exceptionOrNull()
            assertTrue(
                "expected the endpoint pair unique index to reject the duplicate, but was $duplicateFailure",
                generateSequence(duplicateFailure) { it.cause }.any {
                    it.message?.contains(
                        "UNIQUE constraint failed: entry_links.firstEntryId, " +
                            "entry_links.secondEntryId",
                    ) == true
                },
            )
            assertEquals(
                "link-1",
                database
                    .entryLinkQueryDao()
                    .getAll()
                    .single()
                    .linkId,
            )
            database.entryLinkCommandDao().upsert(link("link-1").copy(updatedAt = 2L))
            assertEquals(
                2L,
                database
                    .entryLinkQueryDao()
                    .getAll()
                    .single()
                    .updatedAt,
            )

            assertEquals(1, database.entryCommandDao().deleteById("account"))
            assertTrue(database.entryLinkQueryDao().getAll().isEmpty())
        }

    private fun entry(
        id: String,
        type: EntryType,
    ) = EntryEntity(
        entryId = id,
        entryType = type,
        title = id,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun link(id: String) =
        EntryLinkEntity(
            linkId = id,
            firstEntryId = "account",
            secondEntryId = "login",
            createdAt = 1L,
            updatedAt = 1L,
        )
}
