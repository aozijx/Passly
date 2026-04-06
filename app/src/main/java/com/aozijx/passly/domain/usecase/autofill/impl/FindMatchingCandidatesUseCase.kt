package com.aozijx.passly.domain.usecase.autofill.impl

import com.aozijx.passly.domain.model.AutofillCandidate
import com.aozijx.passly.domain.repository.service.AutofillServiceRepository

class FindMatchingCandidatesUseCase(private val repository: AutofillServiceRepository) {
    suspend operator fun invoke(packageName: String?, webDomain: String?): List<AutofillCandidate> =
        repository.findMatchingCandidates(packageName, webDomain)
}
