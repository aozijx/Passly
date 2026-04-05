package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

class SearchEntriesUseCase(private val repository: VaultRepository) {
    operator fun invoke(query: String): Flow<List<VaultEntry>> = repository.searchEntries(query)
}
