package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.local.config.DatabaseConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultEntryDao {

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES}")
    fun observeAll(): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES}")
    suspend fun getAll(): List<VaultEntryEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE id = :entryId LIMIT 1")
    suspend fun getEntryById(entryId: Int): VaultEntryEntity?

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE id IN (:entryIds)")
    suspend fun getEntriesByIds(entryIds: List<Int>): List<VaultEntryEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE entryType = :entryType")
    fun observeByType(entryType: Int): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE entryType = :entryType")
    suspend fun getByType(entryType: Int): List<VaultEntryEntity>

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_ENTRIES}")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE entryType = :entryType")
    suspend fun countByType(entryType: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<VaultEntryEntity>)

    @Update
    suspend fun update(entry: VaultEntryEntity)

    @Delete
    suspend fun delete(entry: VaultEntryEntity)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_ENTRIES}")
    suspend fun deleteAll()
}
