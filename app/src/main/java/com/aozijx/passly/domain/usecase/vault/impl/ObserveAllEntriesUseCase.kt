package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.repository.vault.VaultRepository
import kotlinx.coroutines.flow.Flow

class ObserveAllEntriesUseCase(private val repository: VaultRepository) {
    operator fun invoke(): Flow<List<VaultEntry>> = repository.allEntries
}
