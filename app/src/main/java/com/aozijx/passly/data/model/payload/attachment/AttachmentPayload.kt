package com.aozijx.passly.data.model.payload.attachment

import kotlinx.serialization.Serializable

/**
 * 附件备份 Payload。
 *
 * 属于 Attachment 子系统，用于附件数据的加密序列化。
 * 不直接暴露给备份系统使用。
 */
@Serializable
data class AttachmentPayload(
    val attachmentId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val encryptedPath: String = "",
    val sha256: String? = null,
    val createdAt: Long = 0L
)
