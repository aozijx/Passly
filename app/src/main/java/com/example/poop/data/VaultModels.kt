package com.example.poop.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val username: String, // 存储加密后的用户名
    val password: String, // 存储加密后的密码
    val category: String, // 改为 String 以支持自定义分类
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE category = :category ORDER BY timestamp DESC")
    fun getItemsByCategory(category: String): Flow<List<VaultItem>>

    @Query("SELECT DISTINCT category FROM vault_items ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM vault_items WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchItems(query: String): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VaultItem>)

    @Update
    suspend fun update(item: VaultItem)

    @Delete
    suspend fun delete(item: VaultItem)

    @Query("DELETE FROM vault_items")
    suspend fun deleteAll()
}
