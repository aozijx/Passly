package com.aozijx.passly.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Semantic metadata for immutable content referenced by a current entry. */
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
    ],
)
internal data class AttachmentRefEntity(
    @PrimaryKey
    val attachmentId: String,
    val resourceId: String,
    val entryId: String,
    val fileName: String,
    val mimeType: String?,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
