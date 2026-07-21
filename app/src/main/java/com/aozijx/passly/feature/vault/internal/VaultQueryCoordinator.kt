package com.aozijx.passly.feature.vault.internal

import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.lookup.LookupRepository
import com.aozijx.passly.domain.usecase.vault.VaultQueryUseCases
import com.aozijx.passly.feature.vault.model.VaultTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

internal class VaultQueryCoordinator(
    private val vaultQueryUseCases: VaultQueryUseCases
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeItems(
        debouncedSearchQuery: Flow<String>,
        normalizedSelectedCategory: Flow<String?>,
        distinctSelectedTab: Flow<VaultTab>
    ): Flow<List<VaultEntry>> = combine(
        debouncedSearchQuery, normalizedSelectedCategory, distinctSelectedTab
    ) { query, category, tab ->
        QueryParams(query = query, category = category, tab = tab)
    }.distinctUntilChanged().flatMapLatest { params ->
        vaultQueryUseCases.observe(
            query = params.query,
            category = params.category,
            filter = LookupRepository.EntryFilter.ALL
        )
    }

    private data class QueryParams(
        val query: String,
        val category: String?,
        val tab: VaultTab
    )
}