package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.repository.vault.VaultRepository

class RecordUsageUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(entryId: Int) = repository.recordUsage(entryId)
}
