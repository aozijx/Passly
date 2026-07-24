package com.aozijx.passly.data.repository.attachment

import android.content.Context
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.crypto.AttachmentCipher
import com.aozijx.passly.data.mapper.attachment.AttachmentMapper
import com.aozijx.passly.data.model.payload.backup.AttachmentPayload
import com.aozijx.passly.domain.model.attachment.EntryAttachment
import com.aozijx.passly.domain.repository.attachment.AttachmentRepository
import com.aozijx.passly.security.crypto.FieldEncryptor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 附件 Repository 实现。
 *
 * 元数据（文件名、MIME 等）作为明文列存储，无需解密即可列表查询；
 * 加密元数据（加密文件路径 + SHA-256）在 [AttachmentPayload] 中序列化后
 * 经 [AttachmentCipher] 加密写入 [EntryAttachmentEntity.encryptedBlob]。
 * 文件内容经 [FieldEncryptor] 加密后落盘。
 *
 * ## 文件存储目录
 * `{filesDir}/attachments/{entryId}/{attachmentId}.enc`
 *
 * ## 哈希验证
 * 保存时计算原始内容的 SHA-256，读取时解密后重算比对。
 */
@Singleton
class FileBackedAttachmentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: UnifiedSessionManager,
    private val fieldEncryptor: FieldEncryptor
) : AttachmentRepository {

    /** 附件文件根目录 */
    private val attachmentsDir: File
        get() = File(context.filesDir, "attachments")

    override suspend fun getAttachments(entryId: String): List<EntryAttachment> =
        sessionManager.query {
            entryAttachmentQueryDao().getByEntryId(entryId)
                .map { AttachmentMapper.toDomain(it) }
        }

    override suspend fun getPendingAttachments(draftId: String): List<EntryAttachment> =
        sessionManager.query {
            entryAttachmentQueryDao().getPendingByOwner(draftId)
                .map { AttachmentMapper.toDomain(it) }
        }

    override suspend fun saveAttachment(
        entryId: String,
        attachment: EntryAttachment,
        content: ByteArray
    ) = sessionManager.transaction {
        val now = System.currentTimeMillis()
        val attachmentId = attachment.attachmentId

        // 1. 加密文件内容并落盘
        val sha256Hex = sha256Hex(content)
        val encryptedContent = fieldEncryptor.encrypt(
            Base64.getEncoder().encodeToString(content),
            null
        )
        val file = resolveFile(entryId, attachmentId)
        file.parentFile?.mkdirs()
        file.writeBytes(encryptedContent)

        // 2. 构建加密元数据（AttachmentPayload → encryptedBlob）
        val payload = AttachmentPayload(
            attachmentId = attachmentId,
            fileName = attachment.fileName,
            mimeType = attachment.mimeType ?: "",
            fileSize = attachment.fileSize,
            encryptedPath = "${entryId}/${attachmentId}.enc",
            sha256 = sha256Hex,
            createdAt = now
        )
        val encryptedBlob = AttachmentCipher.encrypt(payload, entryId, attachmentId, fieldEncryptor)

        // 3. 写入 DB
        val entity = AttachmentMapper.toEntity(
            attachment.copy(createdAt = now),
            encryptedBlob
        )
        entryAttachmentCommandDao().insertStrict(entity)
    }

    override suspend fun deleteAttachment(attachmentId: String) =
        sessionManager.transaction {
            // 先查实体获取路径
            val entity = entryAttachmentQueryDao().getById(attachmentId) ?: return@transaction
            entryAttachmentCommandDao().deleteById(attachmentId)

            // 删除对应磁盘文件
            val file = resolveFile(entity.entryId, attachmentId)
            if (file.exists()) file.delete()
        }

    // ======== 内部工具 ========

    private fun resolveFile(entryId: String, attachmentId: String): File =
        File(attachmentsDir, "${entryId}/${attachmentId}.enc")

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }
}
