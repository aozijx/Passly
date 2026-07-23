package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_attachments",
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
data class EntryAttachmentEntity(
    @PrimaryKey
    val attachmentId: String,
    val entryId: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val encryptedBlob: ByteArray,
    val createdAt: Long = System.currentTimeMillis()
)
