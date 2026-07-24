package com.aozijx.passly.domain.repository.attachment

import com.aozijx.passly.domain.model.attachment.EntryAttachment

interface AttachmentRepository {
    /** 获取条目下已提交的附件（status = COMMITTED） */
    suspend fun getAttachments(entryId: String): List<EntryAttachment>

    /** 获取草稿下未提交的附件（status = PENDING, owner = draftId） */
    suspend fun getPendingAttachments(draftId: String): List<EntryAttachment>

    suspend fun saveAttachment(entryId: String, attachment: EntryAttachment)

    suspend fun deleteAttachment(attachmentId: String)
}
