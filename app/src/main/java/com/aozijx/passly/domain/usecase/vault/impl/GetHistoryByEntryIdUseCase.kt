package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import kotlinx.coroutines.flow.Flow

class GetHistoryByEntryIdUseCase(private val repository: HistoryRepository) {
    operator fun invoke(entryId: Int): Flow<List<VaultHistory>> = repository.getHistoryByEntryId(entryId)
}
