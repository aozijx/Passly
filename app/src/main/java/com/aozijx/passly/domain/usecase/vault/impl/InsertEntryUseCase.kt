package com.aozijx.passly.domain.usecase.vault.impl

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.repository.vault.VaultRepository

class InsertEntryUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(entry: VaultEntry): Long = repository.insert(entry)
}
