package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.repository.VaultRepository

class UpdateEntryUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(entry: VaultEntry) = repository.update(entry)
}
