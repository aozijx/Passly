package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.local.database.entity.EntryActivityEntity
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.model.EntryPagingRow
import com.aozijx.passly.data.local.database.query.buildEntryPagingQuery
import com.aozijx.passly.data.mapper.entry.databaseFlag
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.query.EntryFilter
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryPagingDaoTest {
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
    fun pagingQuery_filtersStructuredSummary_andReturnsUsageAndAccount() = runBlocking {
        insert(entry("account", EntryType.ACCOUNT, "Example account"))
        insert(
            entry(
                id = "mail",
                type = EntryType.LOGIN,
                title = "Mail",
                tags = setOf("Work"),
                domains = setOf("example.com"),
                capabilityFlags = databaseFlag(EntryCapability.PASSWORD),
            )
        )
        database.entryActivityCommandDao().insertAllIdempotent(
            listOf(
                activity("view", "mail", ActivityType.VIEW, 100L),
                activity("copy", "mail", ActivityType.COPY_PASSWORD, 200L),
                activity("update", "mail", ActivityType.UPDATE, 300L),
            )
        )
        database.entryLinkCommandDao().upsert(
            EntryLinkEntity(
                linkId = "member",
                sourceEntryId = "mail",
                targetEntryId = "account",
                relationType = EntryRelationType.MEMBER_OF_ACCOUNT,
                createdAt = 10L,
                updatedAt = 10L,
            )
        )

        val rows = load(
            EntryListQuery(
                searchText = "example",
                filter = EntryFilter.PASSWORD_ONLY,
                category = "work",
            )
        )

        assertEquals(listOf("mail"), rows.map { it.entry.entryId })
        assertEquals(2, rows.single().usageCount)
        assertEquals(200L, rows.single().lastUsedAt)
        assertEquals("account", rows.single().accountId)
    }

    @Test
    fun pagingQuery_keepsNullLastUsedLastInBothDirections() = runBlocking {
        insert(entry("unused", EntryType.LOGIN, "Unused"))
        insert(entry("older", EntryType.LOGIN, "Older"))
        insert(entry("newer", EntryType.LOGIN, "Newer"))
        database.entryActivityCommandDao().insertAllIdempotent(
            listOf(
                activity("older-view", "older", ActivityType.VIEW, 100L),
                activity("newer-view", "newer", ActivityType.VIEW, 200L),
            )
        )

        fun query(direction: SortDirection) = EntryListQuery(
            sort = EntrySort(
                field = EntrySortField.LAST_USED_AT,
                direction = direction,
                pinFavorites = false,
            )
        )

        assertEquals(
            listOf("older", "newer", "unused"),
            load(query(SortDirection.ASC)).map { it.entry.entryId },
        )
        assertEquals(
            listOf("newer", "older", "unused"),
            load(query(SortDirection.DESC)).map { it.entry.entryId },
        )
    }

    @Test
    fun pagingQuery_sortsEntryTypeByDomainDeclarationOrder() = runBlocking {
        insert(entry("note", EntryType.NOTE, "Note"))
        insert(entry("login", EntryType.LOGIN, "Login"))
        insert(entry("account", EntryType.ACCOUNT, "Account"))

        val rows = load(
            EntryListQuery(
                sort = EntrySort(
                    field = EntrySortField.ENTRY_TYPE,
                    direction = SortDirection.ASC,
                    pinFavorites = false,
                )
            )
        )

        assertEquals(
            listOf("account", "login", "note"),
            rows.map { it.entry.entryId },
        )
    }

    private suspend fun insert(entry: EntryEntity) =
        database.entryCommandDao().insertStrict(entry)

    private suspend fun load(query: EntryListQuery): List<EntryPagingRow> {
        val result = database.entryQueryDao()
            .paging(buildEntryPagingQuery(query))
            .load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 50,
                    placeholdersEnabled = false,
                )
            )
        return (result as PagingSource.LoadResult.Page).data
    }

    private fun entry(
        id: String,
        type: EntryType,
        title: String,
        tags: Set<String> = emptySet(),
        domains: Set<String> = emptySet(),
        capabilityFlags: Int = 0,
    ) = EntryEntity(
        entryId = id,
        entryType = type,
        title = title,
        tags = tags,
        domains = domains,
        capabilityFlags = capabilityFlags,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun activity(
        id: String,
        entryId: String,
        type: ActivityType,
        createdAt: Long,
    ) = EntryActivityEntity(
        activityId = id,
        entryId = entryId,
        activityType = type,
        createdAt = createdAt,
    )
}
