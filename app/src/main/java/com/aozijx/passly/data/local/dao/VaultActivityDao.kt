package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.model.entity.VaultActivityEntity
import com.aozijx.passly.domain.model.activity.ActivityType
import kotlinx.coroutines.flow.Flow

/**
 * 活动记录按语义分为两类：
 * - **使用记录**：[ActivityType.USAGE_TYPES] — VIEW, COPY_USERNAME, COPY_PASSWORD, AUTOFILL, EXPORT, IMPORT
 * - **版本更新**：[ActivityType.VERSION_TYPES] — CREATE, UPDATE, DELETE, RESTORE
 *
 * 对应的方法前缀 `usage*` / `version*` 明确区分两种语义，
 * 避免将使用统计与版本变更日志混为一谈。
 */
@Dao
interface VaultActivityDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun observeByEntryId(entryId: String): Flow<List<VaultActivityEntity>>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VaultActivityEntity>>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityType = :activityType ORDER BY createdAt DESC")
    fun observeByType(activityType: ActivityType): Flow<List<VaultActivityEntity>>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun observeUsageForEntry(
        entryId: String,
        types: List<ActivityType>
    ): Flow<List<VaultActivityEntity>>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun observeVersionForEntry(
        entryId: String,
        types: List<ActivityType>
    ): Flow<List<VaultActivityEntity>>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityType IN (:types) ORDER BY createdAt DESC")
    fun observeUsage(types: List<ActivityType>): Flow<List<VaultActivityEntity>>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityType IN (:types) ORDER BY createdAt DESC")
    fun observeVersion(types: List<ActivityType>): Flow<List<VaultActivityEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, VaultActivityEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} ORDER BY createdAt DESC")
    fun pagingAll(): PagingSource<Int, VaultActivityEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun pagingUsageForEntry(
        entryId: String,
        types: List<ActivityType>
    ): PagingSource<Int, VaultActivityEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun pagingVersionForEntry(
        entryId: String,
        types: List<ActivityType>
    ): PagingSource<Int, VaultActivityEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityId = :activityId LIMIT 1")
    suspend fun getById(activityId: String): VaultActivityEntity?

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId ORDER BY createdAt DESC")
    suspend fun getByEntryId(entryId: String): List<VaultActivityEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityId = :activityId)")
    suspend fun exists(activityId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ACTIVITY}")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types)")
    suspend fun countUsageByEntryId(entryId: String, types: List<ActivityType>): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types)")
    suspend fun countVersionByEntryId(entryId: String, types: List<ActivityType>): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType = :activityType")
    suspend fun countByEntryIdAndType(entryId: String, activityType: ActivityType): Int

    // ---- aggregate ----

    // 获取使用次数最多的条目（按使用记录数降序）
    @Query("SELECT entryId, COUNT(*) AS cnt FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityType IN (:types) GROUP BY entryId ORDER BY cnt DESC LIMIT :limit")
    suspend fun getMostUsedEntryIds(limit: Int, types: List<ActivityType>): List<EntryUsageCount>

    // 获取指定条目的活动类型分布
    @Query("SELECT activityType, COUNT(*) AS cnt FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId GROUP BY activityType ORDER BY cnt DESC")
    suspend fun getActivityBreakdownByEntryId(entryId: String): List<ActivityTypeBreakdown>

    // ---- usage stats (Flow) ----

    // 以 Flow 观察所有条目的使用统计（usageCount + lastUsedAt）
    @Query("SELECT entryId, COUNT(*) AS usageCount, MAX(createdAt) AS lastUsedAt FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityType IN (:usageTypes) GROUP BY entryId")
    fun observeUsageStats(usageTypes: List<ActivityType>): Flow<List<EntryUsageStatsRow>>

    // 一次性获取所有条目的使用统计
    @Query("SELECT entryId, COUNT(*) AS usageCount, MAX(createdAt) AS lastUsedAt FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityType IN (:usageTypes) GROUP BY entryId")
    suspend fun getUsageStats(usageTypes: List<ActivityType>): List<EntryUsageStatsRow>

    // === Strict Insert (ignore duplicate) ===

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStrict(activity: VaultActivityEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllStrict(activities: List<VaultActivityEntity>)

    // === Import Upsert ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertForImport(activity: VaultActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllForImport(activities: List<VaultActivityEntity>)

    // === Maintenance API ===

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId IN (:entryIds)")
    suspend fun deleteByEntryIds(entryIds: List<String>)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ACTIVITY}")
    suspend fun clear(): Int
}

// 条目使用次数聚合结果
data class EntryUsageCount(
    val entryId: String,
    val cnt: Int
)

// 活动类型分布聚合结果
data class ActivityTypeBreakdown(
    val activityType: String,
    val cnt: Int
)

// 条目使用统计（用于取代 Metadata 中的 usageCount/lastUsedAt）
data class EntryUsageStatsRow(
    val entryId: String,
    val usageCount: Int,
    val lastUsedAt: Long?
)
