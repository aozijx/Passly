package com.aozijx.passly.domain.autofill.port

import com.aozijx.passly.domain.autofill.model.AutofillInboxPendingDraft

fun interface AutofillInboxDraftImporter {
    suspend fun import(draft: AutofillInboxPendingDraft): AutofillInboxImportOutcome
}

enum class AutofillInboxImportOutcome {
    Imported,
    Invalid,
    Retry,
}
