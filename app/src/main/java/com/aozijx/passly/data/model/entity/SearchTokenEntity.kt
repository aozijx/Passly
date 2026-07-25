package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.aozijx.passly.domain.entry.model.lookup.LookupField

@Entity(
    tableName = "entry_search_tokens",
    primaryKeys = ["entryId", "field", "keywordHash", "gramLength"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entryId", "field"]),
        Index(value = ["keywordHash"]),
    ]
)
data class SearchTokenEntity(
    val entryId: String,
    val field: LookupField,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val keywordHash: ByteArray,
    val gramLength: Int = 2,
    val weight: Int = 1
)
