package com.aozijx.passly.data.mapper.attachment

import com.aozijx.passly.data.model.entity.EntryAttachmentEntity
import com.aozijx.passly.data.model.payload.backup.AttachmentPayload
import com.aozijx.passly.domain.model.attachment.AttachmentStatus
import com.aozijx.passly.domain.model.attachment.EntryAttachment

object AttachmentMapper {

    // ======== Entity ↔ Domain ========

    fun toDomain(entity: EntryAttachmentEntity): EntryAttachment = EntryAttachment(
        attachmentId = entity.attachmentId,
        entryId = entity.entryId,
        fileName = entity.fileName,
        fileSize = entity.fileSize,
        mimeType = entity.mimeType,
        status = AttachmentStatus.valueOf(entity.status),
        owner = entity.owner,
        createdAt = entity.createdAt
    )

    fun toEntity(domain: EntryAttachment, encryptedBlob: ByteArray): EntryAttachmentEntity =
        EntryAttachmentEntity(
            attachmentId = domain.attachmentId,
            entryId = domain.entryId,
            fileName = domain.fileName,
            fileSize = domain.fileSize,
            mimeType = domain.mimeType,
            status = domain.status.name,
            owner = domain.owner,
            encryptedBlob = encryptedBlob,
            createdAt = domain.createdAt
        )

    // ======== Payload ↔ Domain (备份用) ========

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
