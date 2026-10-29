package com.aozijx.passly.domain.autofill.model

sealed interface AutofillInboxEnqueueOutcome {
    data object Queued : AutofillInboxEnqueueOutcome
    data object NotProvisioned : AutofillInboxEnqueueOutcome
    data object Full : AutofillInboxEnqueueOutcome
    data object Failed : AutofillInboxEnqueueOutcome
}
