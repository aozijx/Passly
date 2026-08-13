package com.aozijx.passly.feature.vault.list

import com.aozijx.passly.domain.entry.model.query.EntryFilter
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
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
            refreshTrigger = flowOf(1L)
        ).first()

        assertEquals(EntryFilter.ALL, repository.lastFilter)
        assertEquals("query", repository.lastQuery)
    }

    private class RecordingRepository : EntryListQueryRepository {
        override val deletedEntries: Flow<List<EntryListItem>> = emptyFlow()
        var lastQuery: String? = null
        var lastFilter: EntryFilter? = null

        override fun observe(
            query: String,
            filter: EntryFilter
        ): Flow<List<EntryListItem>> {
            lastQuery = query
            lastFilter = filter
            return flowOf(emptyList())
        }
    }
}
