package com.example.poop.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.io.Serializable

/**
 * 历史表：全维度变更记录
 * 与 VaultEntry 深度绑定，追踪字段级变化
 */
@Entity(
    tableName = DatabaseConfig.TABLE_HISTORY,
    foreignKeys = [
        ForeignKey(
            entity = VaultEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE // 主表删除，历史同步清理
        )
    ],
    indices = [
        Index(value = ["entryId"]),
        Index(value = ["changedAt"])
    ]
)
data class VaultHistory(
    @PrimaryKey(autoGenerate = true) val historyId: Int = 0,
    val entryId: Int,
    
    // 变更详情
    val fieldName: String,      // 被修改的字段名，如 "password", "notes", "cardCvv"
    val oldValue: String?,      // 修改前的加密密文
    val newValue: String?,      // 修改后的加密密文
    
    // 操作上下文
    val changeType: Int = 0,    // 0: 手动修改, 1: 批量导入, 2: 自动生成, 3: 恢复记录
    val deviceName: String? = null, // 操作设备名称
    
    val changedAt: Long = System.currentTimeMillis()
) : Serializable

@Dao
interface VaultDao {
    // --- 基础查询 ---
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

    // --- 进阶历史记录配合逻辑 ---
    
    @Query("SELECT * FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId ORDER BY changedAt DESC")
    fun getHistoryByEntryId(entryId: Int): Flow<List<VaultHistory>>

    /**
     * 事务操作：更新主表并自动记录历史
     */
    @Transaction
    suspend fun updateWithHistory(entry: VaultEntry, history: VaultHistory) {
        update(entry.copy(updatedAt = System.currentTimeMillis()))
        insertHistory(history)
    }

    // --- 内部基础操作 ---
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

    @Update
    suspend fun updateInternal(entry: VaultEntry)

    @Delete
    suspend fun delete(entry: VaultEntry)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_ENTRIES}")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(history: VaultHistory)
    
    @Query("DELETE FROM ${DatabaseConfig.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun clearHistoryByEntryId(entryId: Int)
}
