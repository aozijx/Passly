package com.example.passly.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aozijx.passly.data.local.DatabaseConfig
import com.aozijx.passly.data.model.VaultEntry
import com.example.passly.data.model.VaultHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} ORDER BY favorite DESC, usageCount DESC, createdAt DESC")
    fun getAllEntries(): Flow<List<VaultEntry>>

    @Query("SELECT DISTINCT category FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE category IS NOT NULL AND category != ''")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE category = :category ORDER BY createdAt DESC")
    fun getEntriesByCategory(category: String): Flow<List<VaultEntry>>

    @Query("""
        SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} 
        WHERE title LIKE '%' || :query || '%' 
        OR category LIKE '%' || :query || '%' 
        OR tags LIKE '%' || :query || '%'
    """)
    fun searchEntries(query: String): Flow<List<VaultEntry>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId ORDER BY changedAt DESC")
    fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>>

    @Transaction
    suspend fun updateWithHistory(entry: VaultEntry, history: VaultHistory) {
        update(entry.copy(updatedAt = System.currentTimeMillis()))
        insertHistory(history)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInternal(entry: VaultEntry)

    suspend fun insert(entry: VaultEntry) {
        val now = System.currentTimeMillis()
        insertInternal(entry.copy(createdAt = entry.createdAt ?: now, updatedAt = now))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<VaultEntry>)

    @Update
    suspend fun update(entry: VaultEntry)

    @Delete
    suspend fun delete(entry: VaultEntry)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_ENTRIES}")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(history: VaultHistory)
    
    @Query("DELETE FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun clearHistoryByEntryId(entryId: Int)
}
