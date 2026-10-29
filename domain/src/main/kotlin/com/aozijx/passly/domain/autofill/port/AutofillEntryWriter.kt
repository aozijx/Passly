package com.aozijx.passly.domain.autofill.port

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId

/** Narrow persistence capability available only to an authorized Autofill session. */
fun interface AutofillEntryWriter {
    suspend fun create(entry: Entry): AppResult<EntryId>

    suspend fun createFromInbox(
        entry: Entry,
        draftId: AutofillInboxDraftId,
    ): AppResult<EntryId> = create(entry)
}
