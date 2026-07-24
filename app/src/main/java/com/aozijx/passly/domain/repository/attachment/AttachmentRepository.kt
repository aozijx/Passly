package com.aozijx.passly.domain.repository.attachment

import com.aozijx.passly.domain.model.attachment.EntryAttachment

interface AttachmentRepository {
    /** 获取条目下已提交的附件（status = COMMITTED） */
    suspend fun getAttachments(entryId: String): List<EntryAttachment>

    /** 获取草稿下未提交的附件（status = PENDING, owner = draftId） */
    suspend fun getPendingAttachments(draftId: String): List<EntryAttachment>

    /**
     * 保存附件元数据及加密文件内容。
     *
     * @param entryId  所属条目 ID
     * @param attachment 附件元数据
     * @param content   原始文件内容（由本方法负责加密落盘并计算 SHA-256）
     */
    suspend fun saveAttachment(entryId: String, attachment: EntryAttachment, content: ByteArray)

    /** 删除附件（同时删除磁盘加密文件） */
    suspend fun deleteAttachment(attachmentId: String)
}
