package com.aozijx.passly.data.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.database.DatabaseConfig
import com.aozijx.passly.domain.model.activity.ActivityType

@Entity(
    tableName = DatabaseConfig.TABLE_ACTIVITY,
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
        Index(value = ["createdAt"])
    ]
)
data class VaultActivityEntity(
    @PrimaryKey
    val activityId: String = "",
    val entryId: String,
    @ColumnInfo(name = "activityType")
    val activityType: ActivityType,
    /**
     * 行为来源，例如：
     * - com.android.chrome
     * - com.android.settings
     * - Export
     * - Sync
     */
    val source: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)