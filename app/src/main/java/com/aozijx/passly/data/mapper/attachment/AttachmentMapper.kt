package com.aozijx.passly.data.mapper.attachment

import com.aozijx.passly.data.model.payload.backup.AttachmentPayload
import com.aozijx.passly.domain.model.attachment.AttachmentStatus
import com.aozijx.passly.domain.model.attachment.EntryAttachment

object AttachmentMapper {

    fun toDomain(payload: AttachmentPayload, entryId: String): EntryAttachment = EntryAttachment(
        attachmentId = payload.attachmentId,
        entryId = entryId,
        fileName = payload.fileName,
        fileSize = payload.fileSize,
        mimeType = payload.mimeType,
        status = AttachmentStatus.COMMITTED,
        createdAt = payload.createdAt
    )

    fun toPayload(attachment: EntryAttachment): AttachmentPayload = AttachmentPayload(
        attachmentId = attachment.attachmentId,
        fileName = attachment.fileName,
        mimeType = attachment.mimeType ?: "",
        fileSize = attachment.fileSize,
        encryptedPath = "",
        sha256 = null,
        createdAt = attachment.createdAt
    )
}
