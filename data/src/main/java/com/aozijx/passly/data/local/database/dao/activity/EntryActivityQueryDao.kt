package com.aozijx.passly.data.local.database.dao.activity

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.EntryActivityEntity
import com.aozijx.passly.domain.entry.model.activity.ActivityType
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
interface EntryActivityQueryDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM entry_activities WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun observeByEntryId(entryId: String): Flow<List<EntryActivityEntity>>

    @Query("SELECT * FROM entry_activities ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<EntryActivityEntity>>

    @Query("SELECT * FROM entry_activities WHERE activityType = :activityType ORDER BY createdAt DESC")
    fun observeByType(activityType: ActivityType): Flow<List<EntryActivityEntity>>

    @Query("SELECT * FROM entry_activities WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun observeUsageForEntry(
        entryId: String,
        types: List<ActivityType>
    ): Flow<List<EntryActivityEntity>>

    @Query("SELECT * FROM entry_activities WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun observeVersionForEntry(
        entryId: String,
        types: List<ActivityType>
    ): Flow<List<EntryActivityEntity>>

    @Query("SELECT * FROM entry_activities WHERE activityType IN (:types) ORDER BY createdAt DESC")
    fun observeUsage(types: List<ActivityType>): Flow<List<EntryActivityEntity>>

    @Query("SELECT * FROM entry_activities WHERE activityType IN (:types) ORDER BY createdAt DESC")
    fun observeVersion(types: List<ActivityType>): Flow<List<EntryActivityEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM entry_activities WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, EntryActivityEntity>

    @Query("SELECT * FROM entry_activities ORDER BY createdAt DESC")
    fun pagingAll(): PagingSource<Int, EntryActivityEntity>

    @Query("SELECT * FROM entry_activities WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun pagingUsageForEntry(
        entryId: String,
        types: List<ActivityType>
    ): PagingSource<Int, EntryActivityEntity>

    @Query("SELECT * FROM entry_activities WHERE entryId = :entryId AND activityType IN (:types) ORDER BY createdAt DESC")
    fun pagingVersionForEntry(
        entryId: String,
        types: List<ActivityType>
    ): PagingSource<Int, EntryActivityEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM entry_activities WHERE activityId = :activityId LIMIT 1")
    suspend fun getById(activityId: String): EntryActivityEntity?

    @Query("SELECT * FROM entry_activities WHERE entryId = :entryId ORDER BY createdAt DESC")
    suspend fun getByEntryId(entryId: String): List<EntryActivityEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM entry_activities WHERE activityId = :activityId)")
    suspend fun exists(activityId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM entry_activities")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM entry_activities WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    @Query("SELECT COUNT(*) FROM entry_activities WHERE entryId = :entryId AND activityType IN (:types)")
    suspend fun countUsageByEntryId(entryId: String, types: List<ActivityType>): Int

    @Query("SELECT COUNT(*) FROM entry_activities WHERE entryId = :entryId AND activityType IN (:types)")
    suspend fun countVersionByEntryId(entryId: String, types: List<ActivityType>): Int

    @Query("SELECT COUNT(*) FROM entry_activities WHERE entryId = :entryId AND activityType = :activityType")
    suspend fun countByEntryIdAndType(entryId: String, activityType: ActivityType): Int
}
