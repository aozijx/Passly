package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.github.f4b6a3.uuid.UuidCreator

@Entity(
    tableName = DatabaseSchema.TABLE_HISTORY,
    foreignKeys = [
        ForeignKey(
            entity = VaultMetadataEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entryId", "version"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class VaultSnapshotEntity(
    // 历史记录唯一标识
    @PrimaryKey
    val historyId: String = UuidCreator.getTimeOrderedEpoch().toString(),

    // 变更版本号，从 1 开始递增
    val version: Int,

    // 关联条目 ID
    val entryId: String,

    // 加密后的完整快照（Metadata + Credential）
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val snapshotBlob: ByteArray,

    // 变更类型：value_changed, version_restored
    val changeType: String,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)