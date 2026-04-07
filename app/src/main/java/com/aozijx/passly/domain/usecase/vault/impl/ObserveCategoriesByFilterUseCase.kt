package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import kotlinx.coroutines.flow.Flow

class ObserveCategoriesByFilterUseCase(
    private val repository: VaultSearchRepository
) {
    operator fun invoke(filter: VaultSearchRepository.EntryFilter): Flow<List<String>> =
        repository.getCategoriesByFilter(filter)
}
