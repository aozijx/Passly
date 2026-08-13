package com.aozijx.passly.domain.entry.model.attachment

data class EntryAttachment(
    val attachmentId: String,
    val entryId: String?,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String?,
    val status: AttachmentStatus,
    val stagingOwnerId: String? = null,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AttachmentStatus {
    PENDING,
    COMMITTED
}
