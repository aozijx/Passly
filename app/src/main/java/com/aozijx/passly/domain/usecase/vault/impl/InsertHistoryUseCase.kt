package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.core.VaultHistory
import com.aozijx.passly.domain.repository.vault.HistoryRepository

class InsertHistoryUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(history: VaultHistory) = repository.insertHistory(history)
}