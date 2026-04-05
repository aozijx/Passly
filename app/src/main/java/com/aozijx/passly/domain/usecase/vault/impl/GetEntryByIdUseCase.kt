package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.repository.VaultRepository

class GetEntryByIdUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(entryId: Int): VaultEntry? = repository.getEntryById(entryId)
}
