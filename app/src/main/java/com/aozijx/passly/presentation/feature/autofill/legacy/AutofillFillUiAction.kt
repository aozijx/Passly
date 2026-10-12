package com.aozijx.passly.presentation.feature.autofill.legacy

import com.aozijx.passly.domain.entry.model.EntryId

sealed interface AutofillFillUiAction {
    data class Initialize(val request: AutofillFillRequest) : AutofillFillUiAction
    data class CandidateSelected(val entryId: EntryId) : AutofillFillUiAction
    data object Cancel : AutofillFillUiAction
}
