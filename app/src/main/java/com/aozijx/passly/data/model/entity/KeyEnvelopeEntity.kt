package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseConfig

enum class EnvelopeType {
    BIOMETRIC,
    DEVICE_CREDENTIAL,
    APP_PASSWORD,
    RECOVERY,
    PASSKEY,
    YUBIKEY
}

enum class KdfAlgorithm {
    NONE,
    ARGON2ID,
    PBKDF2_SHA256
}

/**
 * 密钥信封 —— 一个 DEK 可通过多种认证方式加解密。
 *
 * 支持：
 * - Biometric（生物认证）
 * - DeviceCredential（设备锁屏）
 * - AppPassword（应用密码）
 * - Recovery（恢复码）
 * - Passkey（未来）
 * - YubiKey（未来）
 *
 * 每种认证方式对应一条 Envelope 记录，
 * 新增认证方式只需追加 enum 值，无需修改表结构。
 */
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
