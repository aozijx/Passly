package com.aozijx.passly.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.config.DatabaseConfig
import java.io.Serializable

@Entity(tableName = DatabaseConfig.TABLE_ENTRIES)
data class VaultEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entryType: Int = 0,
    val encryptedBlob: String,
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable
