package com.aozijx.passly.data.mapper.attachment

import com.aozijx.passly.data.model.entity.AttachmentRefEntity
import com.aozijx.passly.domain.entry.model.attachment.AttachmentStatus
import com.aozijx.passly.domain.entry.model.attachment.EntryAttachment

object AttachmentRefMapper {
    fun toDomain(entity: AttachmentRefEntity, fileSize: Long): EntryAttachment = EntryAttachment(
        attachmentId = entity.attachmentId,
        entryId = entity.entryId,
        fileName = entity.fileName,
        fileSize = fileSize,
        mimeType = entity.mimeType,
        status = AttachmentStatus.valueOf(entity.status),
        stagingOwnerId = entity.stagingOwnerId,
        displayOrder = entity.displayOrder,
        createdAt = entity.createdAt,
    )

    fun toEntity(domain: EntryAttachment, resourceId: String): AttachmentRefEntity =
        AttachmentRefEntity(
            attachmentId = domain.attachmentId,
            resourceId = resourceId,
            entryId = domain.entryId,
            stagingOwnerId = domain.stagingOwnerId,
            fileName = domain.fileName,
            mimeType = domain.mimeType,
            displayOrder = domain.displayOrder,
            status = domain.status.name,
            createdAt = domain.createdAt,
        )
}
