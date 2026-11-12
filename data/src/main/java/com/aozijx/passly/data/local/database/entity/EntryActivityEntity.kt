package com.aozijx.passly.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType

@Entity(
    tableName = "entry_activities",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["entryId", "createdAt"]),
        Index(value = ["createdAt"]),
        Index(value = ["activityType", "entryId", "createdAt"]),
    ],
)
internal data class EntryActivityEntity(
    @PrimaryKey
    val activityId: String,
    val entryId: String,
    @ColumnInfo(name = "activityType")
    val activityType: ActivityType,
    val revisionVersion: Int? = null,
    @ColumnInfo(defaultValue = "'[]'")
    val affectedFieldKeys: Set<String> = emptySet(),
    val createdAt: Long = System.currentTimeMillis(),
)
