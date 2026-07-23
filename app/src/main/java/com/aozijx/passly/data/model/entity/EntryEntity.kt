package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.domain.model.entry.EntryType

@Entity(
    tableName = "vault_entries",
    indices = [
        Index(value = ["entryType"]),
        Index(value = ["deletedAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class EntryEntity(
    @PrimaryKey
    val entryId: String,
    val vaultId: String = "default",
    val entryType: EntryType = EntryType.LOGIN,
    val version: Int = 1,
    val capabilityFlags: Int = 0,
    val searchIndexVersion: Int = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val summaryBlob: ByteArray,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
