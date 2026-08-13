package com.aozijx.passly.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Durable second phase of attachment deletion. */
@Entity(
    tableName = "attachment_gc_queue",
    foreignKeys = [
        ForeignKey(
            entity = AttachmentResourceEntity::class,
            parentColumns = ["resourceId"],
            childColumns = ["resourceId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["enqueuedAt"])],
)
data class AttachmentGcQueueEntity(
    @PrimaryKey val resourceId: String,
    val enqueuedAt: Long,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
)
