package com.aozijx.passly.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.DatabaseConfig
import java.io.Serializable

/**
 * 历史表：全维度变更记录
 */
@Entity(
    tableName = DatabaseConfig.TABLE_HISTORY,
    foreignKeys = [
        ForeignKey(
            entity = VaultEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entryId"]),
        Index(value = ["changedAt"])
    ]
)
data class VaultHistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Int = 0,
    val entryId: Int,
    val fieldName: String,
    val oldValue: String?,
    val newValue: String?,
    val changeType: Int = 0,
    val deviceName: String? = null,
    val changedAt: Long = System.currentTimeMillis()
) : Serializable
