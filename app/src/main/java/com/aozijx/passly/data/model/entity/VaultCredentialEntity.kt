package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.github.f4b6a3.uuid.UuidCreator

@Entity(
    tableName = DatabaseSchema.TABLE_CREDENTIALS,
    foreignKeys = [
        ForeignKey(
            entity = VaultMetadataEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VaultCredentialEntity(
    // 条目唯一标识
    @PrimaryKey
    val entryId: String = UuidCreator.getTimeOrderedEpoch().toString(),

    // VaultCredential（AES-256-GCM 加密）
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val credentialBlob: ByteArray
)