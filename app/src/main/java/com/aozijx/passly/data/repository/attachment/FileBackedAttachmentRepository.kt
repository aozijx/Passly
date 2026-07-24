package com.aozijx.passly.data.repository.attachment

import com.aozijx.passly.domain.model.attachment.EntryAttachment
import com.aozijx.passly.domain.repository.attachment.AttachmentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileBackedAttachmentRepository @Inject constructor(
) : AttachmentRepository {

    override suspend fun getAttachments(entryId: String): List<EntryAttachment> {
        // TODO: 附件功能尚未对接 DAO
        return emptyList()
    }

    override suspend fun getPendingAttachments(draftId: String): List<EntryAttachment> {
        // TODO: 附件功能尚未对接 DAO
        return emptyList()
    }

    override suspend fun saveAttachment(entryId: String, attachment: EntryAttachment) {
        // TODO: 附件功能尚未对接 DAO
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        // TODO: 附件功能尚未对接 DAO
    }
}
