package com.example.poop.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.poop.data.local.DatabaseConfig
import java.io.Serializable

/**
 * 主表：保险库条目 (v5 深度扩展版)
 */
@Entity(tableName = DatabaseConfig.TABLE_ENTRIES)
data class VaultEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val username: String,
    val password: String,
    val category: String,
    val notes: String? = null,
    val iconName: String? = null,
    val iconCustomPath: String? = null,
    val totpSecret: String? = null,
    val totpPeriod: Int = 30,
    val totpDigits: Int = 6,
    val totpAlgorithm: String = "SHA1",
    val passkeyDataJson: String? = null,
    val recoveryCodes: String? = null,
    val hardwareKeyInfo: String? = null,
    val wifiEncryptionType: String? = "WPA",
    val wifiIsHidden: Boolean = false,
    val cardCvv: String? = null,
    val cardExpiration: String? = null,
    val idNumber: String? = null,
    val paymentPin: String? = null,
    val sshPrivateKey: String? = null,
    val cryptoSeedPhrase: String? = null,
    val entryType: Int = 0,
    val associatedAppPackage: String? = null,
    val associatedDomain: String? = null,
    val uriList: List<String>? = null,
    val matchType: Int = 0,
    val customFieldsJson: String? = null,
    val autoSubmit: Boolean = false,
    val encryptedImageData: ByteArray? = null,
    val strengthScore: Float? = null,
    val lastUsedAt: Long? = null,
    val usageCount: Int = 0,
    val favorite: Boolean = false,
    val tags: List<String>? = null,
    val createdAt: Long? = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val expiresAt: Long? = null
) : Serializable
