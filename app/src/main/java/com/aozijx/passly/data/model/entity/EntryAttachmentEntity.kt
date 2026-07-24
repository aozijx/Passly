package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.domain.model.attachment.AttachmentStatus

@Entity(
    tableName = "entry_attachments",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entryId"]), Index(value = ["status"])]
)
data class EntryAttachmentEntity(
    @PrimaryKey
    val attachmentId: String,
    val entryId: String,
    /** 文件名，明文列，无需解密 BLOB 即可查询 */
    val fileName: String = "",
    /** 文件大小（字节），明文列 */
    val fileSize: Long = 0L,
    /** MIME 类型，明文列 */
    val mimeType: String? = null,
    /** 附件状态：PENDING（草稿/未提交）或 COMMITTED（已提交） */
    val status: String = AttachmentStatus.PENDING.name,
    /** 所有者标识：COMMITTED 时为条目 ID，PENDING 时为草稿 ID */
    val owner: String = "",
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val encryptedBlob: ByteArray = byteArrayOf(),
    val createdAt: Long = System.currentTimeMillis()
)
