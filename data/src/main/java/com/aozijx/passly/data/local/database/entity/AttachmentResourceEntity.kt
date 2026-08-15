package com.aozijx.passly.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Immutable encrypted content addressed by a keyed content ID.
 *
 * `lifecycleState == PENDING_GC` doubles as the durable deletion queue entry:
 * [enqueuedAt], [attemptCount] and [lastAttemptAt] track the file-deletion
 * retry bookkeeping that used to live in a separate `attachment_gc_queue` table.
 */
@Entity(tableName = "attachment_resources")
data class AttachmentResourceEntity(
    @PrimaryKey
    val resourceId: String,
    val fileSize: Long,
    val lifecycleState: String = AttachmentResourceState.ACTIVE,
    val createdAt: Long,
    val enqueuedAt: Long? = null,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
)

object AttachmentResourceState {
    const val ACTIVE = "ACTIVE"
    const val PENDING_GC = "PENDING_GC"
}
