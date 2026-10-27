package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VaultQueryStateTest {
    @Test
    fun `query state normalizes text and applies hierarchy only to all entries`() {
        val state = VaultQueryState.create(
            searchText = "  Mail  ",
            category = "  Work  ",
            sort = createdAtDescendingSort,
            entryTypes = emptySet(),
            hierarchyMode = EntryHierarchyDisplayMode.EXPANDED,
            reloadVersion = 7L,
        )

        val allQuery = state.toEntryListQuery()
        val passwordQuery = state.copy(entryTypes = setOf(EntryType.LOGIN)).toEntryListQuery()

        assertEquals("Mail", allQuery.searchText)
        assertEquals("Work", allQuery.category)
        assertEquals(emptySet<EntryType>(), allQuery.entryTypes)
        assertEquals(EntryHierarchyDisplayMode.EXPANDED, allQuery.hierarchyMode)
        assertEquals(setOf(EntryType.LOGIN), passwordQuery.entryTypes)
        assertNull(passwordQuery.hierarchyMode)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `reload version switches generation while equal state is deduplicated`() = runTest {
        val initial = VaultQueryState.create(
            searchText = "mail",
            category = null,
            sort = createdAtDescendingSort,
            entryTypes = emptySet(),
            hierarchyMode = EntryHierarchyDisplayMode.COLLAPSED,
            reloadVersion = 0L,
        )
        val states = MutableSharedFlow<VaultQueryState>(replay = 1, extraBufferCapacity = 2)
        states.tryEmit(initial)
        val loadedQueries = mutableListOf<String>()
        val outputs = mutableListOf<Int>()

        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            states.switchQueryGenerations { query ->
                loadedQueries += query.searchText
                flowOf(loadedQueries.size)
            }.take(2).toList(outputs)
        }
        runCurrent()

        states.emit(initial.copy())
        runCurrent()
        assertEquals(listOf("mail"), loadedQueries)

        states.emit(initial.copy(reloadVersion = 1L))
        runCurrent()
        collection.join()

        assertEquals(listOf("mail", "mail"), loadedQueries)
        assertEquals(listOf(1, 2), outputs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `hierarchy changes restart unfiltered query but not type filtered query`() = runTest {
        val initial = VaultQueryState.create(
            searchText = "",
            category = null,
            sort = createdAtDescendingSort,
            entryTypes = emptySet(),
            hierarchyMode = EntryHierarchyDisplayMode.COLLAPSED,
            reloadVersion = 0L,
        )
        val states = MutableSharedFlow<VaultQueryState>(replay = 1)
        states.emit(initial)
        val allLoads = mutableListOf<EntryHierarchyDisplayMode?>()
        val passwordLoads = mutableListOf<EntryHierarchyDisplayMode?>()

        val allCollection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            states.switchQueryGenerations { query ->
                allLoads += query.hierarchyMode
                flowOf(Unit)
            }.collect {}
        }
        val passwordCollection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            states.map { it.copy(entryTypes = setOf(EntryType.LOGIN)) }.switchQueryGenerations { query ->
                passwordLoads += query.hierarchyMode
                flowOf(Unit)
            }.collect {}
        }
        runCurrent()

        states.emit(initial.copy(hierarchyMode = EntryHierarchyDisplayMode.EXPANDED))
        runCurrent()

        assertEquals(
            listOf(EntryHierarchyDisplayMode.COLLAPSED, EntryHierarchyDisplayMode.EXPANDED),
            allLoads,
        )
        assertEquals(listOf(null), passwordLoads)
        allCollection.cancel()
        passwordCollection.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `changed query cancels the active generation before starting the next`() = runTest {
        val initial = VaultQueryState.create(
            searchText = "mail",
            category = null,
            sort = createdAtDescendingSort,
            entryTypes = emptySet(),
            hierarchyMode = EntryHierarchyDisplayMode.COLLAPSED,
            reloadVersion = 0L,
        )
        val states = MutableSharedFlow<VaultQueryState>(replay = 1)
        states.emit(initial)
        val started = mutableListOf<String>()
        val cancelled = mutableListOf<String>()

        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            states.switchQueryGenerations { query ->
                flow<Unit> {
                    started += query.searchText
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled += query.searchText
                    }
                }
            }.collect {}
        }
        runCurrent()

        states.emit(initial.copy(searchText = "work"))
        runCurrent()

        assertEquals(listOf("mail", "work"), started)
        assertEquals(listOf("mail"), cancelled)
        collection.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `unrelated ui updates cannot postpone the initial search deadline`() = runTest {
        val states = MutableStateFlow(VaultUiState(searchQuery = "mail", selectedSort = createdAtDescendingSort))
        val observed = mutableListOf<VaultQueryState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            buildVaultQueryStates(states, flowOf(EntryHierarchyDisplayMode.COLLAPSED), flowOf(0L))
                .collect(observed::add)
        }
        runCurrent()
        advanceTimeBy(100L)
        states.value = states.value.copy(showTOTPCode = !states.value.showTOTPCode)
        runCurrent()
        advanceTimeBy(100L)
        states.value = states.value.copy(availableCategories = listOf("Work"), selectedCategory = "Work")
        runCurrent()
        advanceTimeBy(50L)
        runCurrent()
        assertEquals(listOf("mail"), observed.map(VaultQueryState::searchText))
        assertEquals("Work", observed.last().category)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `equivalent search text keeps its deadline while category changes immediately`() = runTest {
        val states = MutableStateFlow(VaultUiState(searchQuery = "mail", selectedSort = createdAtDescendingSort))
        val observed = mutableListOf<VaultQueryState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            buildVaultQueryStates(states, flowOf(EntryHierarchyDisplayMode.COLLAPSED), flowOf(0L))
                .collect(observed::add)
        }
        runCurrent()
        advanceTimeBy(250L)
        runCurrent()
        states.value = states.value.copy(searchQuery = "vault")
        runCurrent()
        advanceTimeBy(100L)
        states.value = states.value.copy(searchQuery = " vault ", selectedCategory = "Work")
        runCurrent()
        assertEquals("mail", observed.last().searchText)
        assertEquals("Work", observed.last().category)
        advanceTimeBy(150L)
        runCurrent()
        assertEquals("vault", observed.last().searchText)
        assertEquals("Work", observed.last().category)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `search is debounced while other query inputs propagate immediately`() = runTest {
        val uiStates = MutableStateFlow(
            VaultUiState(searchQuery = "mail", selectedSort = createdAtDescendingSort),
        )
        val hierarchyModes = MutableStateFlow(EntryHierarchyDisplayMode.COLLAPSED)
        val reloadVersions = MutableStateFlow(0L)
        val observed = mutableListOf<VaultQueryState>()

        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            buildVaultQueryStates(uiStates, hierarchyModes, reloadVersions).collect(observed::add)
        }
        runCurrent()
        assertEquals(emptyList<VaultQueryState>(), observed)

        advanceTimeBy(249L)
        runCurrent()
        assertEquals(emptyList<VaultQueryState>(), observed)
        advanceTimeBy(1L)
        runCurrent()
        assertEquals(listOf("mail"), observed.map(VaultQueryState::searchText))

        uiStates.value = uiStates.value.copy(selectedCategory = "Work")
        runCurrent()
        assertEquals("Work", observed.last().category)

        uiStates.value = uiStates.value.copy(selectedSort = createdAtDescendingSort.toggled())
        runCurrent()
        assertEquals(createdAtDescendingSort.toggled(), observed.last().sort)

        hierarchyModes.value = EntryHierarchyDisplayMode.EXPANDED
        runCurrent()
        assertEquals(EntryHierarchyDisplayMode.EXPANDED, observed.last().hierarchyMode)

        val countBeforeSearch = observed.size
        uiStates.value = uiStates.value.copy(searchQuery = "vault")
        runCurrent()
        assertEquals(countBeforeSearch, observed.size)
        advanceTimeBy(250L)
        runCurrent()
        assertEquals("vault", observed.last().searchText)
        collection.cancel()
    }
}

private val createdAtDescendingSort = EntrySort(
    field = EntrySortField.CREATED_AT,
    direction = SortDirection.DESC,
)
