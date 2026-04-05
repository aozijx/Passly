package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

class ObserveAllEntrySummariesUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<List<VaultSummary>> = repository.allEntrySummaries
}
