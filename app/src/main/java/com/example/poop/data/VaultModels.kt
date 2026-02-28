package com.example.poop.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class VaultCategory(val displayName: String) {
    SOCIAL("社交账号"),
    BANK("银行卡"),
    WORK("工作"),
    OTHER("其他")
}

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val username: String,
    val password: String,
    val category: VaultCategory,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<VaultItem>>

    @Insert
    suspend fun insert(item: VaultItem)

    @Update
    suspend fun update(item: VaultItem)

    @Delete
    suspend fun delete(item: VaultItem)
}
