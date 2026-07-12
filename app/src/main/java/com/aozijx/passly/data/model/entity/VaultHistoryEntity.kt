package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.DatabaseConfig

@Entity(
    tableName = DatabaseConfig.TABLE_HISTORY,
    foreignKeys = [
        ForeignKey(
            entity = VaultMetadataEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entryId"]),
        Index(value = ["createdAt"])
    ]
)
data class VaultHistoryEntity(

    @PrimaryKey
    val historyId: String = "",

    val entryId: String,

    // 加密后的完整快照（Metadata + Credential）
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val snapshotBlob: ByteArray,

    // 从 1 开始递增
    val version: Int,

    val createdAt: Long = System.currentTimeMillis()

)