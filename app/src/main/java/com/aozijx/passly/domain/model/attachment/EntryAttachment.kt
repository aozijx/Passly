package com.aozijx.passly.domain.model.attachment

data class EntryAttachment(
    val attachmentId: String,
    val entryId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String?,
    val status: AttachmentStatus,
    val owner: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class AttachmentStatus {
    PENDING,
    COMMITTED
}
