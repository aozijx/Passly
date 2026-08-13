package com.aozijx.passly.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Immutable encrypted content addressed by a keyed content ID. */
@Entity(tableName = "attachment_resources")
data class AttachmentResourceEntity(
    @PrimaryKey
    val resourceId: String,
    val fileSize: Long,
    val lifecycleState: String = AttachmentResourceState.ACTIVE,
    val createdAt: Long,
)

object AttachmentResourceState {
    const val ACTIVE = "ACTIVE"
    const val PENDING_GC = "PENDING_GC"
}
