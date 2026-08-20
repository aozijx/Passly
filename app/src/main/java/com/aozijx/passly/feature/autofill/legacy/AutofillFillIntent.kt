package com.aozijx.passly.feature.autofill.legacy

import com.aozijx.passly.domain.autofill.model.ResolvedCandidate

sealed interface AutofillFillIntent {
    data class Initialize(val request: AutofillFillRequest) : AutofillFillIntent
    data class CandidateSelected(val candidate: ResolvedCandidate) : AutofillFillIntent
}
