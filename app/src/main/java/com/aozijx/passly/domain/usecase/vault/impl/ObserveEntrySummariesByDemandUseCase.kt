package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import kotlinx.coroutines.flow.Flow

class ObserveEntrySummariesByDemandUseCase(
    private val repository: VaultSearchRepository
) {
    operator fun invoke(
        query: String, category: String?, filter: VaultSearchRepository.EntryFilter
    ): Flow<List<VaultSummary>> = repository.observeEntrySummariesByDemand(query, category, filter)
}
