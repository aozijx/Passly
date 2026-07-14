package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.domain.model.lookup.LookupField

@Entity(
    tableName = DatabaseSchema.TABLE_LOOKUP_INDEX,
    primaryKeys = ["entryId", "field", "keywordHash", "gramLength"],
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
    // 关联条目 ID
    val entryId: String,

    // 索引字段类型
    val field: LookupField,

    // 关键词哈希值
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val keywordHash: ByteArray,

    // 分词长度
    val gramLength: Int = 2,

    // 搜索权重
    val weight: Int = 1
)