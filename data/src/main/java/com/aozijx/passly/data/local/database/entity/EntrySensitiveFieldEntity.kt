package com.aozijx.passly.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "entry_sensitive_fields",
    primaryKeys = ["entryId", "fieldKey"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entryId"])]
)
data class EntrySensitiveFieldEntity(
    val entryId: String,
    val fieldKey: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val valueCipher: ByteArray,
    val keyVersion: Int,
    val updatedAt: Long
)
