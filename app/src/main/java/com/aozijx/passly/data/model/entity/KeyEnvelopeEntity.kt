package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseConfig

@Entity(
    tableName = DatabaseConfig.TABLE_KEY_ENVELOPES,
    indices = [
        Index(value = ["type"])
    ]
)
data class KeyEnvelopeEntity(

    @PrimaryKey
    val envelopeId: String = "",

    /**
     * 认证类型
     */
    val type: Int,

    /**
     * KDF 算法
     */
    val algorithm: Int,

    /**
     * 加密后的 DEK
     */
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val ciphertext: ByteArray,

    /**
     * KDF 参数（Argon2id salt 等）
     */
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val kdfSalt: ByteArray? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)