package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.aozijx.passly.data.local.database.DatabaseConfig

enum class LookupField {
    TITLE,
    USERNAME,
    EMAIL,
    URL,
    NOTES
}

@Entity(
    tableName = DatabaseConfig.TABLE_LOOKUP_INDEX,
    primaryKeys = ["entryId", "field", "keywordHash"],
    foreignKeys = [
        ForeignKey(
            entity = VaultMetadataEntity::class,
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
data class LookupIndexEntity(

    val entryId: String,

    val field: LookupField,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val keywordHash: ByteArray,

    val weight: Int = 1
)