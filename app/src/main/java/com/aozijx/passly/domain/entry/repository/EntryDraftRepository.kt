package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.draft.EntryDraft

interface EntryDraftRepository {
    suspend fun getLatestByEntryId(entryId: String): EntryDraft?
    suspend fun saveDraft(draft: EntryDraft)
    suspend fun deleteDraft(draftId: String)
}
