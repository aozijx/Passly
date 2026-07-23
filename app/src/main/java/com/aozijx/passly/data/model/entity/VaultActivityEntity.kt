package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.domain.model.activity.ActivityType
import com.github.f4b6a3.uuid.UuidCreator

@Entity(
    tableName = DatabaseSchema.TABLE_ACTIVITY,
    foreignKeys = [
        ForeignKey(
            entity = VaultMetadataEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entryId", "createdAt"]),
        Index(value = ["createdAt"]),
        Index(value = ["activityType", "entryId", "createdAt"])
    ]
)
data class VaultActivityEntity(
    // 活动唯一标识
    @PrimaryKey
    val activityId: String = UuidCreator.getTimeOrderedEpoch().toString(),

    // 关联条目 ID
    val entryId: String,

    // 活动类型
    @ColumnInfo(name = "activityType")
    val activityType: ActivityType,

    // 行为来源（例如：Chrome, Settings, Export, Sync）
    val source: String? = null,

    // 创建时间
    val createdAt: Long = System.currentTimeMillis()
)