package com.aozijx.passly.data.model.payload.backup

import kotlinx.serialization.Serializable

/**
 * 附件备份 Payload。
 *
 * 附件不属于 Credential，属于 Snapshot 顶层的独立资源。
 * 可关联任意条目类型（Login / Note / Card / Identity / Secure File）。
 *
 * 只携带展示和恢复所需元数据。实际加密文件内容作为二进制文件存储在
 * 备份归档中，不入 JSON。
 *
 * @param encryptedPath  加密文件的本地路径或相对引用
 * @param sha256         文件内容 SHA-256 哈希（hex），用于恢复后完整性校验
 */
@Serializable
data class AttachmentPayload(
    val attachmentId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val encryptedPath: String,
    val sha256: String? = null,
    val createdAt: Long = 0L
)