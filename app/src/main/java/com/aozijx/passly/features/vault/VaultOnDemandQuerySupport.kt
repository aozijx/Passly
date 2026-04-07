package com.aozijx.passly.features.vault

import com.aozijx.passly.core.designsystem.model.VaultTab
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.domain.usecase.vault.VaultUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

internal class VaultOnDemandQuerySupport(
    private val vaultUseCases: VaultUseCases
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeVaultItems(
        debouncedSearchQuery: Flow<String>,
        normalizedSelectedCategory: Flow<String?>,
        distinctSelectedTab: Flow<VaultTab>
    ): Flow<List<VaultSummary>> = combine(
        debouncedSearchQuery, normalizedSelectedCategory, distinctSelectedTab
    ) { query, category, tab ->
        VaultQueryParams(
            query = query, category = category, filter = tab.toEntryFilter()
        )
    }.distinctUntilChanged().flatMapLatest { params ->
            vaultUseCases.observeEntrySummariesByDemand(
                query = params.query, category = params.category, filter = params.filter
            )
        }

    private fun VaultTab.toEntryFilter(): VaultSearchRepository.EntryFilter = when (this) {
        VaultTab.ALL -> VaultSearchRepository.EntryFilter.ALL
        VaultTab.PASSWORDS -> VaultSearchRepository.EntryFilter.PASSWORD_ONLY
        VaultTab.TOTP -> VaultSearchRepository.EntryFilter.TOTP_ONLY
    }

    private data class VaultQueryParams(
        val query: String, val category: String?, val filter: VaultSearchRepository.EntryFilter
    )
}
