package com.aozijx.passly.domain.repository.draft

import com.aozijx.passly.domain.model.draft.EntryDraft

interface EntryDraftRepository {
    suspend fun getLatestByEntryId(entryId: String): EntryDraft?
    suspend fun saveDraft(draft: EntryDraft)
    suspend fun deleteDraft(draftId: String)
}
