package com.aozijx.passly.data.repository.draft

import com.aozijx.passly.domain.entry.model.draft.EntryDraft
import com.aozijx.passly.domain.entry.repository.EntryDraftRepository

/**
 * 草稿功能的占位实现。
 *
 * 草稿功能尚未对接 DAO，所有操作均为空实现。
 * 不标记为 @Inject，防止被 Hilt 作为生产实现注入。
 * 待草稿功能完成 DAO 对接后，再注册为可注入的实现。
 */
class RoomEntryDraftRepository : EntryDraftRepository {

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
