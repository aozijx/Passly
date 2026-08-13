package com.aozijx.passly.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_secrets",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EntrySecretEntity(
    @PrimaryKey
    val entryId: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val secretBlob: ByteArray
)
