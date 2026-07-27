package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_drafts",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entryId"]),
        Index(value = ["status"]),
        Index(value = ["updatedAt"])
    ]
)
data class EntryDraftEntity(
    @PrimaryKey
    val draftId: String,
    val entryId: String? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val draftBlob: ByteArray,
    val status: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
