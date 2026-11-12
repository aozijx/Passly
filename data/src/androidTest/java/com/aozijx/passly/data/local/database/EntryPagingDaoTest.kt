package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.paging.PagingSource
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.codec.toStorageKey
import com.aozijx.passly.data.local.database.entity.EntryActivityEntity
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.model.EntryPagingRow
import com.aozijx.passly.data.local.database.query.buildEntryCategoryQuery
import com.aozijx.passly.data.local.database.query.buildEntryPagingQuery
import com.aozijx.passly.data.mapper.entry.databaseFlag
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import kotlinx.coroutines.flow.first
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
        database =
            TestDatabaseFactory
                .inMemoryDatabaseBuilder(context)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun pagingQuery_filtersStructuredSummary_andReturnsUsageAndAccount() =
        runBlocking {
            insert(entry("account", EntryType.ACCOUNT, "Example account"))
            insert(
                entry(
                    id = "mail",
                    type = EntryType.LOGIN,
                    title = "Mail",
                    tags = setOf("Work"),
                    domains = setOf("example.com"),
                    capabilityFlags = databaseFlag(EntryCapability.PASSWORD),
                ),
            )
            val activities =
                listOf(
                    activity("view", "mail", ActivityType.VIEW, 100L),
                    activity("copy", "mail", ActivityType.COPY_FIELD, 200L, setOf(FieldKey.PASSWORD)),
                    activity("update", "mail", ActivityType.UPDATE, 300L),
                )
            for (activity in activities) {
                database.entryActivityCommandDao().insertIdempotent(activity)
            }
            database.entryLinkCommandDao().upsert(
                EntryLinkEntity(
                    linkId = "member",
                    firstEntryId = "account",
                    secondEntryId = "mail",
                    createdAt = 10L,
                    updatedAt = 10L,
                ),
            )

            val rows =
                load(
                    EntryListQuery(
                        searchText = "example",
                        entryTypes = setOf(EntryType.LOGIN),
                        category = "work",
                        sort =
                            EntrySort(
                                field = EntrySortField.TITLE,
                                direction = SortDirection.ASC,
                                pinFavorites = false,
                            ),
                    ),
                )

            assertEquals(listOf("mail"), rows.map { it.entry.entryId })
            assertEquals(2, rows.single().usageCount)
            assertEquals(200L, rows.single().lastUsedAt)
            assertEquals("account", rows.single().accountId)
        }

    @Test
    fun pagingQuery_matchesAnySelectedEntryType() =
        runBlocking {
            insert(entry("login", EntryType.LOGIN, "Login"))
            insert(entry("otp", EntryType.OTP, "OTP"))
            insert(entry("wifi", EntryType.WIFI, "Wi-Fi"))

            val rows =
                load(
                    EntryListQuery(
                        entryTypes = setOf(EntryType.LOGIN, EntryType.OTP),
                        sort =
                            EntrySort(
                                field = EntrySortField.TITLE,
                                direction = SortDirection.ASC,
                                pinFavorites = false,
                            ),
                    ),
                )

            assertEquals(setOf("login", "otp"), rows.map { it.entry.entryId }.toSet())
        }

    @Test
    fun pagingQuery_searchesEveryStructuredEntryField() =
        runBlocking {
            val cases =
                listOf(
                    "title" to entry("title", EntryType.LOGIN, "TITLE-MATCH"),
                    "username" to
                        entry("username", EntryType.LOGIN, "Neutral", username = "USER-MATCH"),
                    "url" to
                        entry("url", EntryType.LOGIN, "Neutral", primaryUrl = "https://URL-MATCH.example"),
                    "tag" to entry("tag", EntryType.LOGIN, "Neutral", tags = setOf("TAG-MATCH")),
                    "domain" to
                        entry("domain", EntryType.LOGIN, "Neutral", domains = setOf("DOMAIN-MATCH.example")),
                    "application" to
                        entry("application", EntryType.LOGIN, "Neutral", applicationIds = setOf("APP-MATCH.ID")),
                )
            cases.forEach { (_, entry) -> insert(entry) }

            cases.forEach { (search, expectedEntry) ->
                val rows = load(EntryListQuery(searchText = search.uppercase(), sort = titleAscending()))

                assertEquals(listOf(expectedEntry.entryId), rows.map { it.entry.entryId })
            }
        }

    @Test
    fun pagingQuery_resolvesOtpAndRecoveryGroupingFromProtectedEntry() =
        runBlocking {
            insert(entry("account", EntryType.ACCOUNT, "Account"))
            insert(entry("login", EntryType.LOGIN, "Login"))
            insert(entry("otp", EntryType.OTP, "OTP"))
            insert(entry("recovery", EntryType.RECOVERY_CODE, "Recovery"))
            database.entryLinkCommandDao().upsert(
                EntryLinkEntity(
                    linkId = "login-account",
                    firstEntryId = "account",
                    secondEntryId = "login",
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            )
            database.entryLinkCommandDao().upsert(
                EntryLinkEntity(
                    linkId = "otp-login",
                    firstEntryId = "login",
                    secondEntryId = "otp",
                    createdAt = 2L,
                    updatedAt = 2L,
                ),
            )
            database.entryLinkCommandDao().upsert(
                EntryLinkEntity(
                    linkId = "recovery-login",
                    firstEntryId = "login",
                    secondEntryId = "recovery",
                    createdAt = 3L,
                    updatedAt = 3L,
                ),
            )

            val rows =
                load(
                    EntryListQuery(
                        sort =
                            EntrySort(
                                EntrySortField.ID,
                                SortDirection.ASC,
                                pinFavorites = false,
                                tieBreaker = EntrySortField.CREATED_AT,
                            ),
                    ),
                )

            assertEquals(
                mapOf("account" to null, "login" to "account", "otp" to "account", "recovery" to "login"),
                rows.associate { it.entry.entryId to it.accountId },
            )
        }

    @Test
    fun pagingQuery_treatsLikeMetacharactersAsLiteralSearchText() =
        runBlocking {
            insert(entry("percent-literal", EntryType.LOGIN, "literal%needle"))
            insert(entry("percent-wildcard", EntryType.LOGIN, "literalXneedle"))
            insert(entry("underscore-literal", EntryType.LOGIN, "under_score"))
            insert(entry("underscore-wildcard", EntryType.LOGIN, "underXscore"))
            insert(entry("backslash-literal", EntryType.LOGIN, "back\\slash"))
            insert(entry("backslash-absent", EntryType.LOGIN, "backslash"))

            assertEquals(
                listOf("percent-literal"),
                load(EntryListQuery(searchText = "literal%needle", sort = titleAscending())).map { it.entry.entryId },
            )
            assertEquals(
                listOf("underscore-literal"),
                load(EntryListQuery(searchText = "under_score", sort = titleAscending())).map { it.entry.entryId },
            )
            assertEquals(
                listOf("backslash-literal"),
                load(EntryListQuery(searchText = "back\\slash", sort = titleAscending())).map { it.entry.entryId },
            )
        }

    @Test
    fun pagingQuery_sortsByTimestampsAndUsageAndPinsFavorites() =
        runBlocking {
            insert(entry("a", EntryType.LOGIN, "A", createdAt = 10L, updatedAt = 30L))
            insert(entry("b", EntryType.LOGIN, "B", createdAt = 20L, updatedAt = 10L))
            insert(entry("c", EntryType.LOGIN, "C", createdAt = 5L, updatedAt = 20L, favorite = true))
            listOf(
                activity("a-view", "a", ActivityType.VIEW, 100L),
                activity("a-update-1", "a", ActivityType.UPDATE, 200L),
                activity("a-update-2", "a", ActivityType.UPDATE, 300L),
                activity("a-update-3", "a", ActivityType.UPDATE, 400L),
                activity("b-view-1", "b", ActivityType.VIEW, 500L),
                activity("b-view-2", "b", ActivityType.COPY_FIELD, 600L, setOf(FieldKey.PASSWORD)),
            ).forEach { activity -> database.entryActivityCommandDao().insertIdempotent(activity) }
            database.entryActivityCommandDao().insertIdempotent(
                activity(
                    "b-copy-password",
                    "b",
                    ActivityType.COPY_FIELD,
                    700L,
                    setOf(FieldKey.PASSWORD),
                ),
            )

            suspend fun ids(
                field: EntrySortField,
                direction: SortDirection,
                pinFavorites: Boolean = false,
            ) = load(
                EntryListQuery(
                    sort =
                        EntrySort(
                            field,
                            direction,
                            pinFavorites = pinFavorites,
                            tieBreaker =
                                if (field == EntrySortField.ID) {
                                    EntrySortField.CREATED_AT
                                } else {
                                    EntrySortField.ID
                                },
                        ),
                ),
            ).map { it.entry.entryId }

            assertEquals(listOf("a", "b", "c"), ids(EntrySortField.TITLE, SortDirection.ASC))
            assertEquals(listOf("c", "b", "a"), ids(EntrySortField.TITLE, SortDirection.DESC))
            assertEquals(listOf("c", "a", "b"), ids(EntrySortField.CREATED_AT, SortDirection.ASC))
            assertEquals(listOf("b", "a", "c"), ids(EntrySortField.CREATED_AT, SortDirection.DESC))
            assertEquals(listOf("b", "c", "a"), ids(EntrySortField.UPDATED_AT, SortDirection.ASC))
            assertEquals(listOf("a", "c", "b"), ids(EntrySortField.UPDATED_AT, SortDirection.DESC))
            assertEquals(listOf("c", "a", "b"), ids(EntrySortField.USAGE_FREQUENCY, SortDirection.ASC))
            assertEquals(listOf("b", "a", "c"), ids(EntrySortField.USAGE_FREQUENCY, SortDirection.DESC))
            assertEquals(listOf("c", "a", "b"), ids(EntrySortField.ID, SortDirection.ASC, pinFavorites = true))
        }

    @Test
    fun pagingQuery_usesStableDirectionForTieBreaker() =
        runBlocking {
            insert(entry("a-newer", EntryType.LOGIN, "Same", createdAt = 2L))
            insert(entry("z-older", EntryType.LOGIN, "Same", createdAt = 1L))

            val rows =
                load(
                    EntryListQuery(
                        sort =
                            EntrySort(
                                EntrySortField.TITLE,
                                SortDirection.ASC,
                                pinFavorites = false,
                                tieBreaker = EntrySortField.CREATED_AT,
                            ),
                    ),
                )

            assertEquals(listOf("a-newer", "z-older"), rows.map { it.entry.entryId })
        }

    @Test
    fun pagingQuery_keepsNullLastUsedLastInBothDirections() =
        runBlocking {
            insert(entry("unused", EntryType.LOGIN, "Unused"))
            insert(entry("older", EntryType.LOGIN, "Older"))
            insert(entry("newer", EntryType.LOGIN, "Newer"))
            val activities =
                listOf(
                    activity("older-view", "older", ActivityType.VIEW, 100L),
                    activity("newer-view", "newer", ActivityType.VIEW, 200L),
                )
            for (activity in activities) {
                database.entryActivityCommandDao().insertIdempotent(activity)
            }

            fun query(direction: SortDirection) =
                EntryListQuery(
                    sort =
                        EntrySort(
                            field = EntrySortField.LAST_USED_AT,
                            direction = direction,
                            pinFavorites = false,
                        ),
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
    fun pagingQuery_sortsEntryTypeByDomainDeclarationOrder() =
        runBlocking {
            insert(entry("note", EntryType.NOTE, "Note"))
            insert(entry("login", EntryType.LOGIN, "Login"))
            insert(entry("account", EntryType.ACCOUNT, "Account"))

            val rows =
                load(
                    EntryListQuery(
                        sort =
                            EntrySort(
                                field = EntrySortField.ENTRY_TYPE,
                                direction = SortDirection.ASC,
                                pinFavorites = false,
                            ),
                    ),
                )

            assertEquals(
                listOf("account", "login", "note"),
                rows.map { it.entry.entryId },
            )
            assertEquals(
                listOf("note", "login", "account"),
                load(
                    EntryListQuery(
                        sort =
                            EntrySort(
                                EntrySortField.ENTRY_TYPE,
                                SortDirection.DESC,
                                pinFavorites = false,
                            ),
                    ),
                ).map { it.entry.entryId },
            )
        }

    @Test
    fun pagingQuery_appliesHierarchyGlobally_butKeepsNarrowedMatchesVisible() =
        runBlocking {
            insert(entry("z-account", EntryType.ACCOUNT, "Account"))
            insert(entry("a-member", EntryType.LOGIN, "Member", tags = setOf("Work")))
            insert(
                entry(
                    "m-standalone",
                    EntryType.LOGIN,
                    "Standalone",
                    tags = setOf("work", "Personal"),
                ),
            )
            database.entryLinkCommandDao().upsert(
                EntryLinkEntity(
                    linkId = "member-link",
                    firstEntryId = "a-member",
                    secondEntryId = "z-account",
                    createdAt = 10L,
                    updatedAt = 10L,
                ),
            )

            suspend fun ids(
                mode: EntryHierarchyDisplayMode,
                search: String = "",
            ) = load(
                EntryListQuery(
                    searchText = search,
                    hierarchyMode = mode,
                    sort =
                        EntrySort(
                            field = EntrySortField.ID,
                            direction = SortDirection.ASC,
                            pinFavorites = false,
                            tieBreaker = EntrySortField.CREATED_AT,
                        ),
                ),
            ).map { it.entry.entryId }

            assertEquals(
                listOf("m-standalone", "z-account"),
                ids(EntryHierarchyDisplayMode.COLLAPSED),
            )
            assertEquals(
                listOf("m-standalone", "z-account", "a-member"),
                ids(EntryHierarchyDisplayMode.EXPANDED),
            )
            assertEquals(
                listOf("a-member", "m-standalone"),
                ids(EntryHierarchyDisplayMode.SEPARATE),
            )
            assertEquals(
                listOf("a-member"),
                ids(EntryHierarchyDisplayMode.COLLAPSED, search = "member"),
            )
            assertEquals(
                emptyList<String>(),
                ids(EntryHierarchyDisplayMode.SEPARATE, search = "account"),
            )
            assertEquals(
                listOf("Personal", "Work"),
                database.entryQueryDao().observeCategories(buildEntryCategoryQuery()).first(),
            )

            database.entryCommandDao().optimisticSoftDelete(
                entryId = "z-account",
                expectedVersion = 1,
                deletedAt = 20L,
                updatedAt = 20L,
            )
            assertEquals(
                listOf("a-member", "m-standalone"),
                ids(EntryHierarchyDisplayMode.COLLAPSED),
            )
        }

    private suspend fun insert(entry: EntryEntity) = database.entryCommandDao().insertStrict(entry)

    private suspend fun load(query: EntryListQuery): List<EntryPagingRow> {
        val result =
            database
                .entryQueryDao()
                .paging(buildEntryPagingQuery(query))
                .load(
                    PagingSource.LoadParams.Refresh(
                        key = null,
                        loadSize = 50,
                        placeholdersEnabled = false,
                    ),
                )
        return when (result) {
            is PagingSource.LoadResult.Page -> result.data
            is PagingSource.LoadResult.Error -> throw result.throwable
            is PagingSource.LoadResult.Invalid -> error("Paging source invalidated before loading")
        }
    }

    private fun entry(
        id: String,
        type: EntryType,
        title: String,
        username: String = "",
        primaryUrl: String? = null,
        tags: Set<String> = emptySet(),
        domains: Set<String> = emptySet(),
        applicationIds: Set<String> = emptySet(),
        favorite: Boolean = false,
        capabilityFlags: Int = 0,
        createdAt: Long = 1L,
        updatedAt: Long = 1L,
    ) = EntryEntity(
        entryId = id,
        entryType = type,
        title = title,
        username = username,
        primaryUrl = primaryUrl,
        tags = tags,
        domains = domains,
        applicationIds = applicationIds,
        favorite = favorite,
        capabilityFlags = capabilityFlags,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun activity(
        id: String,
        entryId: String,
        type: ActivityType,
        createdAt: Long,
        affectedFields: Set<FieldKey> = emptySet(),
    ) = EntryActivityEntity(
        activityId = id,
        entryId = entryId,
        activityType = type,
        affectedFieldKeys = affectedFields.map(FieldKey::toStorageKey).toSet(),
        createdAt = createdAt,
    )

    private fun titleAscending() = EntrySort(EntrySortField.TITLE, SortDirection.ASC)
}
