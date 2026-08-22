package com.aozijx.passly.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.domain.entry.model.EntryType

@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["deletedAt", "updatedAt"]),
        Index(value = ["entryType", "deletedAt", "updatedAt"]),
        Index(value = ["deletedAt", "createdAt"]),
        Index(value = ["deletedAt", "favorite", "updatedAt"]),
        Index(value = ["deletedAt", "title"]),
        Index(value = ["primaryUrl"]),
    ]
)
data class EntryEntity(
    @PrimaryKey
    val entryId: String,
    val entryType: EntryType = EntryType.LOGIN,
    val version: Int = 1,
    val capabilityFlags: Int = 0,
    /** 条目有 OTP 时的类型（TOTP/HOTP/STEAM），null 表示无 OTP */
    val otpType: String? = null,
    val searchIndexVersion: Int = 0,
    val title: String,
    val username: String = "",
    val primaryUrl: String? = null,
    val domains: Set<String> = emptySet(),
    val applicationIds: Set<String> = emptySet(),
    val iconName: String? = null,
    val iconCustomReference: String? = null,
    val favorite: Boolean = false,
    val tags: Set<String> = emptySet(),
    val iconColor: String? = null,
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
