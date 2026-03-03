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
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.io.Serializable

/**
 * 主表：保险库条目
 * 实现 Serializable 以支持在 Intent 中传递
 */
@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val username: String, // 加密后的用户名
    val password: String, // 加密后的密码
    val category: String, // 分类
    val notes: String? = null, // 备注/笔记
    
    // 图标相关
    val iconName: String? = null, // 预设图标标识
    val iconCustomPath: String? = null, // 本地自定义图片路径
    
    // TOTP 相关
    val totpSecret: String? = null, // 加密后的密钥
    val totpPeriod: Int = 30, // 步长
    val totpDigits: Int = 6, // 位数
    val totpAlgorithm: String = "SHA1",
    
    // Autofill & 匹配增强 (核心)
    val associatedAppPackage: String? = null, // 关联 App 包名
    val associatedDomain: String? = null, // 关联网站域名
    val uriList: String? = null, // 多 URI 列表 (JSON)
    val matchType: Int = 0, // 匹配规则: 0-等值, 1-主机名, 2-基础域名, 3-正则
    val customFieldsJson: String? = null, // 自定义字段 (JSON: key-value)
    val autoSubmit: Boolean = false,
    
    // 安全与统计
    val strengthScore: Float? = null, // 强度评分
    val lastUsedAt: Long? = null, // 最后使用时间
    val usageCount: Int = 0, // 使用次数
    
    // 元数据
    val favorite: Boolean = false,
    val tags: String? = null, // 标签
    val createdAt: Long? = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val expiresAt: Long? = null
) : Serializable

/**
 * 历史表：密码变更记录
 */
@Entity(
    tableName = "password_history",
    foreignKeys = [
        ForeignKey(
            entity = VaultItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["itemId", "changedAt"], unique = true)
    ]
)
data class PasswordHistory(
    @PrimaryKey(autoGenerate = true) val historyId: Int = 0,
    val itemId: Int,
    val oldPassword: String,
    val changedAt: Long = System.currentTimeMillis()
) : Serializable

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY favorite DESC, usageCount DESC, createdAt DESC")
    fun getAllItems(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE category = :category ORDER BY createdAt DESC")
    fun getItemsByCategory(category: String): Flow<List<VaultItem>>

    @Query("SELECT DISTINCT category FROM vault_items ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("""
        SELECT * FROM vault_items 
        WHERE title LIKE '%' || :query || '%' 
        OR category LIKE '%' || :query || '%' 
        OR tags LIKE '%' || :query || '%'
        OR associatedDomain LIKE '%' || :query || '%'
        OR associatedAppPackage LIKE '%' || :query || '%'
    """)
    fun searchItems(query: String): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInternal(item: VaultItem)

    suspend fun insert(item: VaultItem) {
        val now = System.currentTimeMillis()
        val toInsert = item.copy(
            createdAt = item.createdAt ?: now,
            updatedAt = now
        )
        insertInternal(toInsert)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VaultItem>)

    @Update
    suspend fun updateInternal(item: VaultItem)

    suspend fun update(item: VaultItem) {
        updateInternal(item.copy(updatedAt = System.currentTimeMillis()))
    }

    @Delete
    suspend fun delete(item: VaultItem)

    @Query("DELETE FROM vault_items")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(history: PasswordHistory)

    @Query("SELECT * FROM password_history WHERE itemId = :itemId ORDER BY changedAt DESC")
    fun getHistoryByItemId(itemId: Int): Flow<List<PasswordHistory>>
}
