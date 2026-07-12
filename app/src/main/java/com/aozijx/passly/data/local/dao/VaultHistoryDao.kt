package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.DatabaseConfig
import com.aozijx.passly.data.model.entity.VaultHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultHistoryDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId ORDER BY version DESC")
    fun observeByEntryId(entryId: String): Flow<List<VaultHistoryEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId ORDER BY version DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, VaultHistoryEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_HISTORY} WHERE historyId = :historyId LIMIT 1")
    suspend fun getById(historyId: String): VaultHistoryEntity?

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId AND version = :version LIMIT 1")
    suspend fun getByVersion(entryId: String, version: Int): VaultHistoryEntity?

    @Query("SELECT COALESCE(MAX(version), 0) + 1 FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun getNextVersion(entryId: String): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    // ---- insert ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(history: VaultHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(histories: List<VaultHistoryEntity>)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseConfig.TABLE_HISTORY} WHERE historyId = :historyId")
    suspend fun deleteById(historyId: String)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId AND historyId NOT IN (SELECT historyId FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId ORDER BY version DESC LIMIT :keepCount)")
    suspend fun deleteOldVersions(entryId: String, keepCount: Int)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_HISTORY}")
    suspend fun clear()
}