package com.aozijx.passly.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_links",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["firstEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["secondEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["secondEntryId"]),
        Index(
            value = ["firstEntryId", "secondEntryId"],
            unique = true,
        ),
    ],
)
data class EntryLinkEntity(
    @PrimaryKey
    val linkId: String,
    val firstEntryId: String,
    val secondEntryId: String,
    val createdAt: Long,
    val updatedAt: Long,
)
