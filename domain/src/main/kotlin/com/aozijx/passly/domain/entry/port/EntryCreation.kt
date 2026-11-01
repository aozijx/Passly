package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryId

/** Application-owned entry creation capability shared by Vault and Autofill workflows. */
interface EntryCreation {
    suspend operator fun invoke(draft: EntryDraft): AppResult<EntryId>

    suspend fun createFromAutofill(
        draft: EntryDraft,
        inboxDraftId: AutofillInboxDraftId? = null,
    ): AppResult<EntryId>
}
