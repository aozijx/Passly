package com.aozijx.passly.domain.usecase.autofill.impl

import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.repository.service.AutofillServiceRepository

class UpdateUsageStatsUseCase(private val repository: AutofillServiceRepository) {
    suspend operator fun invoke(entry: VaultEntry) = repository.updateUsageStats(entry)
}
