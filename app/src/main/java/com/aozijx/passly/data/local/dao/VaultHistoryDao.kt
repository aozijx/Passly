package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.data.local.config.DatabaseConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultHistoryDao {
    @Query("SELECT * FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId ORDER BY changedAt DESC")
    fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistoryEntity>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId ORDER BY changedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getHistoryPaged(entryId: Int, limit: Int, offset: Int): List<VaultHistoryEntity>

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(history: VaultHistoryEntity)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun clearHistoryByEntryId(entryId: Int)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_HISTORY}")
    suspend fun clearAll()
}
