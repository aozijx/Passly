package com.aozijx.passly.feature.autofill.framework

import com.aozijx.passly.core.autofill.model.ResolvedCandidate

sealed interface AutofillFillIntent {
    data class Initialize(val request: AutofillFillRequest) : AutofillFillIntent
    data class CandidateSelected(val candidate: ResolvedCandidate) : AutofillFillIntent
}
