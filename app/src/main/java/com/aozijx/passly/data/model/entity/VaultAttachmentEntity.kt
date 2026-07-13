package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseConfig

/**
 * 极简附件表：仅存储加密后的 Metadata Blob。
 *
 * 所有业务字段（filename, mimeType, encryptedPath, thumbnail, dimensions...）
 * 均以 JSON 序列化后 AES-GCM 加密存入 [encryptedBlob]。
 */
@Entity(
    tableName = DatabaseConfig.TABLE_ATTACHMENTS,
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
    @PrimaryKey val attachmentId: String,
    val entryId: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val encryptedBlob: ByteArray,
    val createdAt: Long = System.currentTimeMillis()
)