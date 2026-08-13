package com.aozijx.passly.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** A historical revision's reference to immutable attachment content. */
@Entity(
    tableName = "revision_attachment_refs",
    primaryKeys = ["revisionId", "attachmentId"],
    foreignKeys = [
        ForeignKey(
            entity = EntryRevisionEntity::class,
            parentColumns = ["revisionId"],
            childColumns = ["revisionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AttachmentResourceEntity::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["resourceId"])],
)
data class RevisionAttachmentRefEntity(
    val revisionId: String,
    val attachmentId: String,
    val resourceId: String,
    val fileName: String,
    val mimeType: String?,
    val displayOrder: Int,
    val createdAt: Long,
)
