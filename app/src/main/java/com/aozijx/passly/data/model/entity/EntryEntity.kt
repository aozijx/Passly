package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.domain.entry.model.EntryType

@Entity(
    tableName = "vault_entries",
    indices = [
        Index(value = ["deletedAt", "updatedAt"]),
        Index(value = ["entryType", "deletedAt", "updatedAt"]),
        Index(value = ["deletedAt", "createdAt"])
    ]
)
data class EntryEntity(
    @PrimaryKey
    val entryId: String,
    val vaultId: String = "default",
    val entryType: EntryType = EntryType.LOGIN,
    val version: Int = 1,
    val capabilityFlags: Int = 0,
    /** 条目有 OTP 时的类型（TOTP/HOTP/STEAM），null 表示无 OTP */
    val otpType: String? = null,
    val searchIndexVersion: Int = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val summaryBlob: ByteArray,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
