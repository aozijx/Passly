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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(history: VaultHistoryEntity)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun clearHistoryByEntryId(entryId: Int)
}
