package com.aozijx.passly.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.domain.entry.model.attachment.AttachmentStatus

/** Semantic metadata referencing immutable content. Pending refs survive process death. */
@Entity(
    tableName = "attachment_refs",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AttachmentResourceEntity::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["entryId"]),
        Index(value = ["resourceId"]),
        Index(value = ["status"]),
        Index(value = ["stagingOwnerId"]),
    ],
)
data class AttachmentRefEntity(
    @PrimaryKey
    val attachmentId: String,
    val resourceId: String,
    val entryId: String?,
    val stagingOwnerId: String?,
    val fileName: String,
    val mimeType: String?,
    val displayOrder: Int = 0,
    val status: String = AttachmentStatus.PENDING.name,
    val createdAt: Long = System.currentTimeMillis(),
)
