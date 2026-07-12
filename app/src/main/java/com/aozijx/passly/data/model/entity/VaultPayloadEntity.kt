package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseConfig

/**
 * 敏感凭据存储 —— 仅在查看/编辑条目时解密。
 */
@Entity(
    tableName = DatabaseConfig.TABLE_CREDENTIALS,
    foreignKeys = [
        ForeignKey(
            entity = VaultMetadataEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VaultPayloadEntity(

    @PrimaryKey
    val entryId: String = "",

    // CredentialPayload（AES-256-GCM 加密）
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val credentialBlob: ByteArray
)