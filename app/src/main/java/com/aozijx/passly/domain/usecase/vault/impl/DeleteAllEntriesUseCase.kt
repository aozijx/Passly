package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.repository.vault.VaultRepository

class DeleteAllEntriesUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke() = repository.deleteAll()
}
