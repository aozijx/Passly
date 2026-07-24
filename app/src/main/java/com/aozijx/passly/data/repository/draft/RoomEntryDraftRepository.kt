package com.aozijx.passly.data.repository.draft

import com.aozijx.passly.domain.entry.model.draft.EntryDraft
import com.aozijx.passly.domain.entry.repository.EntryDraftRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEntryDraftRepository @Inject constructor(
) : EntryDraftRepository {

    override suspend fun getLatestByEntryId(entryId: String): EntryDraft? {
        // TODO: 草稿功能尚未对接 DAO
        return null
    }

    override suspend fun saveDraft(draft: EntryDraft) {
        // TODO: 草稿功能尚未对接 DAO
    }

    override suspend fun deleteDraft(draftId: String) {
        // TODO: 草稿功能尚未对接 DAO
    }
}
