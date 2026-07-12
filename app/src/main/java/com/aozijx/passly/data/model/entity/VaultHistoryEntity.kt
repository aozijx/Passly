package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseConfig
import com.github.f4b6a3.uuid.UuidCreator

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
        // 查询某个条目的版本历史（ORDER BY version DESC）
        Index(value = ["entryId", "version"]),

        // 清理旧历史
        Index(value = ["createdAt"])
    ]
)
data class VaultHistoryEntity(

    /**
     * Snapshot 唯一标识。
     * 使用 UUIDv7，保证时间有序。
     * version 由 DAO 自动维护，不参与主键。
     */
    @PrimaryKey
    val historyId: String = UuidCreator.getTimeOrderedEpoch().toString(),

    val entryId: String,

    // 从 1 开始递增，由 DAO.getNextVersion() 统一管理
    val version: Int,

    // 加密后的完整快照（Metadata + Credential）
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val snapshotBlob: ByteArray,

    val createdAt: Long = System.currentTimeMillis()
)