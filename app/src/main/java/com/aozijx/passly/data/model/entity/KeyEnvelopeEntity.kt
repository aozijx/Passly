package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseSchema

@Entity(
    tableName = DatabaseSchema.TABLE_KEY_ENVELOPE,
    indices = [
        Index(value = ["type"])
    ]
)
data class KeyEnvelopeEntity(
    /* 信封唯一标识 */
    @PrimaryKey
    val envelopeId: String = "",

    // 算法版本，用于未来加密算法迁移
    val algorithmVersion: Int = 1,

    // 认证类型
    val type: String,

    // KDF 算法
    val algorithm: String,

    // 加密后的 DEK
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val ciphertext: ByteArray,

    // KDF 参数（Argon2id salt 等）
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val kdfSalt: ByteArray? = null,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),

    // 更新时间
    val updatedAt: Long = System.currentTimeMillis()
)