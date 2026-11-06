package com.aozijx.passly.domain.autofill.port

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryId

/** Creates a credential through the Autofill-authorized session and optional Inbox retry identity. */
interface AutofillEntryCreation {
    suspend fun createFromAutofill(
        draft: EntryDraft,
        inboxDraftId: AutofillInboxDraftId? = null,
    ): AppResult<EntryId>
}
