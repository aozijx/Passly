package com.aozijx.passly.domain.repository.attachment

import com.aozijx.passly.domain.model.attachment.EntryAttachment

interface AttachmentRepository {
    suspend fun getAttachments(entryId: String): List<EntryAttachment>
    suspend fun saveAttachment(entryId: String, attachment: EntryAttachment)
    suspend fun deleteAttachment(attachmentId: String)
}
