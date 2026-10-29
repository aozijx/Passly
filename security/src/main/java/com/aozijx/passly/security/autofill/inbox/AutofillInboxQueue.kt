package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.domain.autofill.model.AutofillInboxEnqueueOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInboxSource
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft

fun interface AutofillInboxQueue {
    suspend fun enqueue(
        source: AutofillInboxSource,
        draft: AutofillSaveDraft,
    ): AutofillInboxEnqueueOutcome
}
