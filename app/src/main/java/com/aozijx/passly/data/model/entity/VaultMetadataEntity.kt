package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseConfig

/**
 * 非敏感元数据存储 —— 列表/搜索/自动填充只需解密此 Blob。
 */
@Entity(
    tableName = DatabaseConfig.TABLE_METADATA,
    indices = [
        Index(value = ["entryType"]),
        Index(value = ["deletedAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class VaultMetadataEntity(

    @PrimaryKey
    val entryId: String = "",

    /**
     * 所属 Vault。
     * 预留多 Vault（Personal / Work / Shared）能力。
     */
    val vaultId: String = "default",

    /**
     * 条目版本，用于 History / Sync / Conflict。
     */
    val entryVersion: Int = 1,

    /**
     * 条目类型（Login、Card、Identity、SSH...）
     */
    val entryType: Int = 0,

    /**
     * MetadataPayload（AES-256-GCM 加密）
     */
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val metadataBlob: ByteArray,

    /**
     * 创建时间（UTC Epoch Millis）
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * 最后更新时间（UTC Epoch Millis）
     */
    val updatedAt: Long = System.currentTimeMillis(),

    /**
     * 回收站时间。
     * null 表示正常条目。
     */
    val deletedAt: Long? = null
)