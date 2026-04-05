package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

class GetEntrySummariesByCategoryUseCase(private val repository: VaultRepository) {
    operator fun invoke(category: String): Flow<List<VaultSummary>> = repository.getEntrySummariesByCategory(category)
}
