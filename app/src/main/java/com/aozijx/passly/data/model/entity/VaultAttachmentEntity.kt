package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseSchema

@Entity(
    tableName = DatabaseSchema.TABLE_ATTACHMENT,
    foreignKeys = [
        ForeignKey(
            entity = VaultMetadataEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entryId"])]
)
data class VaultAttachmentEntity(
    // 附件唯一标识
    @PrimaryKey
    val attachmentId: String,

    // 关联条目 ID
    val entryId: String,

    // 加密后的附件元数据（文件名、MIME、加密路径等）
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val encryptedBlob: ByteArray,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)