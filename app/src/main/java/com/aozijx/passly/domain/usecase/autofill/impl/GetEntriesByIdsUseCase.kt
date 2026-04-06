package com.aozijx.passly.domain.usecase.autofill.impl

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.repository.service.AutofillServiceRepository

class GetEntriesByIdsUseCase(private val repository: AutofillServiceRepository) {
    suspend operator fun invoke(entryIds: List<Int>): List<VaultEntry> = repository.getEntriesByIds(entryIds)
}
