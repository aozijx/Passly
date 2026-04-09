package com.aozijx.passly.domain.usecase.autofill.impl

import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.repository.service.AutofillServiceRepository

class GetEntryByIdUseCase(private val repository: AutofillServiceRepository) {
    suspend operator fun invoke(entryId: Int): VaultEntry? = repository.getEntryById(entryId)
}
