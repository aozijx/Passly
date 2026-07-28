package com.aozijx.passly.feature.vault.list

import com.aozijx.passly.domain.entry.model.lookup.EntryFilter
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.repository.EntryListQueryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultQueryCoordinatorTest {

    @Test
    fun pagerQueryAlwaysLoadsAllEntriesForAdjacentTabs() = runBlocking {
        val repository = RecordingRepository()
        val coordinator = VaultQueryCoordinator(repository)

        coordinator.observeItems(
            debouncedSearchQuery = flowOf("query"),
            normalizedSelectedCategory = flowOf("login"),
            refreshTrigger = flowOf(1L)
        ).first()

        assertEquals(EntryFilter.ALL, repository.lastFilter)
        assertEquals("query", repository.lastQuery)
        assertEquals("login", repository.lastCategory)
    }

    private class RecordingRepository : EntryListQueryRepository {
        override val allCategories: Flow<List<String>> = emptyFlow()
        override val deletedEntries: Flow<List<EntryListItem>> = emptyFlow()
        var lastQuery: String? = null
        var lastCategory: String? = null
        var lastFilter: EntryFilter? = null

        override fun observe(
            query: String,
            category: String?,
            filter: EntryFilter
        ): Flow<List<EntryListItem>> {
            lastQuery = query
            lastCategory = category
            lastFilter = filter
            return flowOf(emptyList())
        }

        override fun observeCategories(filter: EntryFilter): Flow<List<String>> =
            emptyFlow()
    }
}
