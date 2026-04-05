package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

class SearchEntrySummariesUseCase(private val repository: VaultRepository) {
    operator fun invoke(query: String): Flow<List<VaultSummary>> = repository.searchEntrySummaries(query)
}
