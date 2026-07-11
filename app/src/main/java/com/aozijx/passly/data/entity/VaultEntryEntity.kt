package com.aozijx.passly.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aozijx.passly.data.local.DatabaseConfig
import java.io.Serializable

@Entity(
    tableName = DatabaseConfig.TABLE_ENTRIES,
    indices = [Index(value = ["entryType"])]
)
data class VaultEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entryType: Int = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val encryptedBlob: ByteArray,
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable
