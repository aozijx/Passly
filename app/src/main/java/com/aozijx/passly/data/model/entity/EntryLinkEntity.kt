package com.aozijx.passly.data.model.entity

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
            childColumns = ["sourceEntryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["targetEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceEntryId"]),
        Index(value = ["targetEntryId"]),
        Index(
            value = ["sourceEntryId", "targetEntryId", "relationType"],
            unique = true
        )
    ]
)
data class EntryLinkEntity(
    @PrimaryKey
    val linkId: String,
    val sourceEntryId: String,
    val targetEntryId: String,
    val relationType: String,
    val createdAt: Long,
    val updatedAt: Long
)
