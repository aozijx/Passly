package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import kotlinx.coroutines.flow.Flow

class GetEntriesByCategoryUseCase(private val repository: VaultSearchRepository) {
    operator fun invoke(category: String): Flow<List<VaultEntry>> = repository.getEntriesByCategory(category)
}
