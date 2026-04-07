package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.repository.vault.VaultRepository

class UpdateEntryUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(entry: VaultEntry) = repository.update(entry)
}
