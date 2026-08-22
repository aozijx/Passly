package com.aozijx.passly.feature.vault.list

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.feature.vault.contract.VaultUiState
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
    fun `search filters the decrypted summary snapshot without querying again`() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val repository = StaticRepository(
            items = listOf(
                item(id = "1", title = "Mail", tags = listOf("Work")),
                item(id = "2", title = "Game", tags = listOf("Personal")),
            )
        )
        val uiState = MutableStateFlow(VaultUiState())
        val coordinator = VaultListCoordinator(
            scope = scope,
            entryListQueryRepository = repository,
            uiState = uiState,
            refreshTrigger = flowOf(0L),
        )

        try {
            coordinator.state.filter { it.items.size == 2 }.first()
            uiState.value = uiState.value.copy(searchQuery = "work")

            val result = coordinator.state.filter { it.items.size == 1 }.first()
            assertEquals("Mail", result.items.single().title)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `category filter is applied after decrypted list items are loaded`() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val repository = StaticRepository(
            items = listOf(
                item(id = "1", title = "Mail", tags = listOf("Work", "Finance")),
                item(id = "2", title = "Game", tags = listOf("Personal"))
            )
        )
        val uiState = MutableStateFlow(VaultUiState())
        val coordinator = VaultListCoordinator(
            scope = scope,
            entryListQueryRepository = repository,
            uiState = uiState,
            refreshTrigger = flowOf(0L)
        )

        try {
            val initial = coordinator.state.filter {
                it.categories == listOf("Finance", "Personal", "Work") &&
                        it.items.size == 2
            }.first()
            assertEquals(2, initial.items.size)

            uiState.value = uiState.value.copy(selectedCategory = "work")

            val filtered = coordinator.state
                .filter { it.items.size == 1 }
                .first()
            assertEquals(
                "Mail",
                filtered.items.single().title
            )
        } finally {
            scope.cancel()
        }
    }

    private class StaticRepository(items: List<EntryListItem>) : EntryListQueryRepository {
        private val itemsFlow = MutableStateFlow(items)
        override val activeSummaries: Flow<List<EntryListItem>> = itemsFlow
        override val deletedEntries: Flow<List<EntryListItem>> = emptyFlow()
    }

    private fun item(
        id: String,
        title: String,
        tags: List<String>
    ): EntryListItem = EntryListItem(
        identity = EntryIdentity(
            id = EntryId(id),
            type = EntryType.LOGIN,
            timestamps = EntryTimestamps(0L),
        ),
        profile = EntryProfile(title = title, tags = tags.toSet()),
    )
}
