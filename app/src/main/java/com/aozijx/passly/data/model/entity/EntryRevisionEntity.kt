package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_revisions",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entryId", "version"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class EntryRevisionEntity(
    @PrimaryKey
    val revisionId: String,
    val entryId: String,
    val version: Int,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val snapshotBlob: ByteArray,
    val changeType: String,
    val createdAt: Long = System.currentTimeMillis()
)
