package com.aozijx.passly.feature.vault.list

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.policy.EntryListSorter
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.domain.settings.model.LibrarySortSpec
import com.aozijx.passly.domain.entry.model.query.EntryFilter
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.feature.vault.contract.VaultUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

data class VaultListState(
    val isLoading: Boolean = true,
    val categories: List<String> = emptyList(),
    val itemsByQuickFilter: Map<LibraryQuickFilter, List<EntryListItem>> = emptyMap()
)

internal class VaultListCoordinator(
    private val scope: CoroutineScope,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val uiState: StateFlow<VaultUiState>,
    private val refreshTrigger: Flow<Long>
) {
    private val _isLoading = MutableStateFlow(true)

    private val searchQuery = uiState
        .map { it.searchQuery }
        .distinctUntilChanged()
    private val selectedSort = uiState
        .map { it.selectedSort }
        .distinctUntilChanged()
    private val selectedCategory = uiState
        .map { it.selectedCategory }
        .distinctUntilChanged()

    private val loadingTrigger = searchQuery
        .map(String::trim)
        .distinctUntilChanged()

    init {
        scope.launch {
            loadingTrigger.drop(1).collect { _isLoading.value = true }
        }
    }

    @OptIn(FlowPreview::class)
    private val rawItems: StateFlow<List<EntryListItem>> = observeItems(
        debouncedSearchQuery = searchQuery
            .map(String::trim)
            .debounce(250)
            .distinctUntilChanged(),
        refreshTrigger = refreshTrigger
    ).onEach { _ ->
        _isLoading.value = false
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeItems(
        debouncedSearchQuery: Flow<String>,
        refreshTrigger: Flow<Long>,
    ): Flow<List<EntryListItem>> = combine(
        debouncedSearchQuery,
        refreshTrigger,
    ) { query, refreshId -> QueryParams(query, refreshId) }
        .distinctUntilChanged()
        .flatMapLatest { params ->
            entryListQueryRepository.observe(query = params.query, filter = EntryFilter.ALL)
        }

    private data class QueryParams(val query: String, val refreshId: Long)

    private val categories: StateFlow<List<String>> = rawItems
        .map { items ->
            items.flatMap { item -> item.tags }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val sortedItems: StateFlow<List<EntryListItem>> = combine(
        rawItems,
        selectedSort,
        selectedCategory.map { category -> category?.trim()?.takeIf(String::isNotEmpty) }
    ) { items, sort, category ->
        val filteredItems = category?.let {
            items.filter { item ->
                item.tags.any { tag -> tag.equals(category, ignoreCase = true) }
            }
        } ?: items
        EntryListSorter.sort(filteredItems, sort.toDomain())
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<VaultListState> = combine(
        _isLoading,
        categories,
        sortedItems
    ) { loading, cats, items ->
        VaultListState(
            isLoading = loading,
            categories = cats,
            itemsByQuickFilter = LibraryQuickFilter.entries.associateWith { quickFilter ->
                when (quickFilter) {
                    LibraryQuickFilter.ALL -> items
                    LibraryQuickFilter.PASSWORDS -> items.filter(EntryListItem::hasPassword)
                    LibraryQuickFilter.TOTP -> items.filter(EntryListItem::hasOtp)
                }
            }
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), VaultListState())
}

private fun LibrarySortSpec.toDomain() = EntrySort(
    field = EntrySortField.valueOf(field.name),
    direction = SortDirection.valueOf(direction.name),
    pinFavorites = pinFavorites,
    tieBreaker = EntrySortField.valueOf(tieBreaker.name),
)
