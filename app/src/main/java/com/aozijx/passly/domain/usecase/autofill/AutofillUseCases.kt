package com.aozijx.passly.domain.usecase.autofill

import com.aozijx.passly.domain.repository.service.AutofillServiceRepository
import com.aozijx.passly.domain.usecase.autofill.impl.FindMatchingCandidatesUseCase
import com.aozijx.passly.domain.usecase.autofill.impl.GetEntriesByIdsUseCase
import com.aozijx.passly.domain.usecase.autofill.impl.GetEntryByIdUseCase
import com.aozijx.passly.domain.usecase.autofill.impl.SaveOrUpdateEntryUseCase
import com.aozijx.passly.domain.usecase.autofill.impl.UpdateUsageStatsUseCase

class AutofillUseCases(repository: AutofillServiceRepository) {
    val updateUsageStats = UpdateUsageStatsUseCase(repository)
    val getEntryById = GetEntryByIdUseCase(repository)
    val getEntriesByIds = GetEntriesByIdsUseCase(repository)
    val findMatchingCandidates = FindMatchingCandidatesUseCase(repository)
    val saveOrUpdateEntry = SaveOrUpdateEntryUseCase(repository)
}
