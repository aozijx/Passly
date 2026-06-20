package com.aozijx.passly.ui.features.vault.internal

import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.domain.usecase.vault.VaultUseCases
import com.aozijx.passly.ui.features.vault.model.VaultTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

internal class VaultQueryCoordinator(
    private val vaultUseCases: VaultUseCases
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeItems(
        debouncedSearchQuery: Flow<String>,
        normalizedSelectedCategory: Flow<String?>,
        distinctSelectedTab: Flow<VaultTab>
    ): Flow<List<VaultSummary>> = combine(
        debouncedSearchQuery, normalizedSelectedCategory, distinctSelectedTab
    ) { query, category, tab ->
        QueryParams(query = query, category = category, tab = tab)
    }.distinctUntilChanged().flatMapLatest { params ->
        vaultUseCases.observeEntrySummaries(
            query = params.query,
            category = params.category,
            filter = VaultSearchRepository.EntryFilter.ALL
        )
    }

    private data class QueryParams(
        val query: String,
        val category: String?,
        val tab: VaultTab
    )
}