package com.aozijx.passly.feature.autofill.legacy

import com.aozijx.passly.domain.autofill.model.ResolvedCandidate

sealed interface AutofillFillUiAction {
    data class Initialize(val request: AutofillFillRequest) : AutofillFillUiAction
    data class CandidateSelected(val candidate: ResolvedCandidate) : AutofillFillUiAction
}
