package com.aozijx.passly.feature.vault.list

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.lookup.EntryFilter
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.repository.EntryListQueryRepository
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultListCoordinatorTest {

    @Test
    fun `category filter is applied after decrypted list items are loaded`() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val repository = StaticRepository(
            items = listOf(
                item(id = "1", title = "Mail", tags = listOf("Work", "Finance")),
                item(id = "2", title = "Game", tags = listOf("Personal"))
            )
        )
        val searchFilter = SearchFilterState(scope = scope)
        val coordinator = VaultListCoordinator(
            scope = scope,
            queryCoordinator = VaultQueryCoordinator(repository),
            searchFilter = searchFilter,
            refreshTrigger = flowOf(0L)
        )

        try {
            val initial = coordinator.state.filter {
                it.categories == listOf("Finance", "Personal", "Work") &&
                        it.itemsByQuickFilter.getValue(LibraryQuickFilter.ALL).size == 2
            }.first()
            assertEquals(2, initial.itemsByQuickFilter.getValue(LibraryQuickFilter.ALL).size)

            searchFilter.updateSelectedCategory("work")

            val filtered = coordinator.state
                .filter { it.itemsByQuickFilter.getValue(LibraryQuickFilter.ALL).size == 1 }
                .first()
            assertEquals(
                "Mail",
                filtered.itemsByQuickFilter.getValue(LibraryQuickFilter.ALL).single().title
            )
        } finally {
            scope.cancel()
        }
    }

    private class StaticRepository(items: List<EntryListItem>) : EntryListQueryRepository {
        private val itemsFlow = MutableStateFlow(items)
        override val deletedEntries: Flow<List<EntryListItem>> = emptyFlow()

        override fun observe(
            query: String,
            filter: EntryFilter
        ): Flow<List<EntryListItem>> {
            return itemsFlow
        }
    }

    private fun item(
        id: String,
        title: String,
        tags: List<String>
    ): EntryListItem = EntryListItem(
        id = id,
        entryType = EntryType.LOGIN,
        title = title,
        username = "",
        icon = null,
        iconCustomPath = null,
        website = null,
        favorite = false,
        tags = tags,
        color = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = null,
        expiresAt = null,
        lastUsedAt = null,
        usageCount = 0,
        capabilityFlags = 0,
        entryVersion = 1
    )
}
