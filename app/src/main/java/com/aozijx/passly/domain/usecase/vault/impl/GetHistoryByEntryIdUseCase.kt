package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

class GetHistoryByEntryIdUseCase(private val repository: VaultRepository) {
    operator fun invoke(entryId: Int): Flow<List<VaultHistory>> = repository.getHistoryByEntryId(entryId)
}
