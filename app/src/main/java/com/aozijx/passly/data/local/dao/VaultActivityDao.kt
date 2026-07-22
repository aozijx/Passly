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

    // ===================== observe (Flow) =====================

    /** 观察指定条目所有活动（含使用记录和版本更新）。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun observeByEntryId(entryId: String): Flow<List<VaultActivityEntity>>

    /** 观察全局活动。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VaultActivityEntity>>

    /** 观察指定类型的活动。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityType = :activityType ORDER BY createdAt DESC")
    fun observeByType(activityType: ActivityType): Flow<List<VaultActivityEntity>>

    /** 观察指定条目的使用记录（语义分类：使用记录）。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun observeUsageForEntry(
        entryId: String,
        types: List<ActivityType>
    ): Flow<List<VaultActivityEntity>>

    /** 观察指定条目的版本更新记录（语义分类：版本更新）。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun observeVersionForEntry(
        entryId: String,
        types: List<ActivityType>
    ): Flow<List<VaultActivityEntity>>

    /** 按使用记录类型观察全局活动。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityType IN (:types) ORDER BY createdAt DESC")
    fun observeUsage(types: List<ActivityType>): Flow<List<VaultActivityEntity>>

    /** 按版本更新类型观察全局活动。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityType IN (:types) ORDER BY createdAt DESC")
    fun observeVersion(types: List<ActivityType>): Flow<List<VaultActivityEntity>>

    // ===================== paging (Paging 3) =====================

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, VaultActivityEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} ORDER BY createdAt DESC")
    fun pagingAll(): PagingSource<Int, VaultActivityEntity>

    /** 按条目分页查询使用记录。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun pagingUsageForEntry(
        entryId: String,
        types: List<ActivityType>
    ): PagingSource<Int, VaultActivityEntity>

    /** 按条目分页查询版本更新记录。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun pagingVersionForEntry(
        entryId: String,
        types: List<ActivityType>
    ): PagingSource<Int, VaultActivityEntity>

    // ===================== get (suspend) =====================

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityId = :activityId LIMIT 1")
    suspend fun getById(activityId: String): VaultActivityEntity?

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId ORDER BY createdAt DESC")
    suspend fun getByEntryId(entryId: String): List<VaultActivityEntity>

    // ===================== exists =====================

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE activityId = :activityId)")
    suspend fun exists(activityId: String): Boolean

    // ===================== count / 统计 =====================

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ACTIVITY}")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    /** 按条目统计使用记录数。 */
    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types)")
    suspend fun countUsageByEntryId(entryId: String, types: List<ActivityType>): Int

    /** 按条目统计版本更新记录数。 */
    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType IN (:types)")
    suspend fun countVersionByEntryId(
        entryId: String,
        types: List<ActivityType>
    ): Int

    /** 统计指定条目指定类型的活动数。 */
    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId AND activityType = :activityType")
    suspend fun countByEntryIdAndType(entryId: String, activityType: ActivityType): Int

    // ===================== 聚合查询 =====================

    /**
     * 获取使用次数最多的条目（按使用记录数降序）。
     * @param limit 返回条目数上限
     */
    @Query(
        """
        SELECT entryId, COUNT(*) AS cnt
        FROM ${DatabaseSchema.TABLE_ACTIVITY}
        WHERE activityType IN (:types)
        GROUP BY entryId
        ORDER BY cnt DESC
        LIMIT :limit
    """
    )
    suspend fun getMostUsedEntryIds(
        limit: Int,
        types: List<ActivityType>
    ): List<EntryUsageCount>

    /**
     * 获取指定条目的活动类型分布。
     * @return 每条记录为 (activityType, count)
     */
    @Query(
        """
        SELECT activityType, COUNT(*) AS cnt
        FROM ${DatabaseSchema.TABLE_ACTIVITY}
        WHERE entryId = :entryId
        GROUP BY activityType
        ORDER BY cnt DESC
    """
    )
    suspend fun getActivityBreakdownByEntryId(entryId: String): List<ActivityTypeBreakdown>

    // ===================== insert / 批量 =====================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(activity: VaultActivityEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(activities: List<VaultActivityEntity>)

    // ===================== delete / 清理 =====================

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    /** 批量删除指定条目列表的全部活动记录。 */
    @Query("DELETE FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE entryId IN (:entryIds)")
    suspend fun deleteByEntryIds(entryIds: List<String>)

    /** 按时间裁剪：清理指定时间之前的所有活动记录。 */
    @Query("DELETE FROM ${DatabaseSchema.TABLE_ACTIVITY} WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long)

    /** 清空全表，返回删除行数。 */
    @Query("DELETE FROM ${DatabaseSchema.TABLE_ACTIVITY}")
    suspend fun clear(): Int
}

// ========================== 聚合结果类型 ==========================

/**
 * 条目使用次数聚合结果。
 * @property entryId 条目 ID
 * @property cnt 使用次数
 */
data class EntryUsageCount(
    val entryId: String,
    val cnt: Int
)

/**
 * 活动类型分布聚合结果。
 * @property activityType 活动类型值（对应 [ActivityType.name]）
 * @property cnt 该类型出现次数
 */
data class ActivityTypeBreakdown(
    val activityType: String,
    val cnt: Int
)
