package com.aozijx.passly.domain.autofill.port

import com.aozijx.passly.domain.autofill.model.AutofillCandidateQuery
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary

interface AutofillCandidateRepository {
    suspend fun search(query: AutofillCandidateQuery): List<AutofillCandidateSummary>
}
