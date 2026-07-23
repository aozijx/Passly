package com.aozijx.passly.data.local.dao.activity

import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.domain.model.activity.ActivityType
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryActivityAnalyticsDao {

    // ---- aggregate ----

    @Query("SELECT entryId, COUNT(*) AS cnt FROM entry_activities WHERE activityType IN (:types) GROUP BY entryId ORDER BY cnt DESC LIMIT :limit")
    suspend fun getMostUsedEntryIds(limit: Int, types: List<ActivityType>): List<EntryUsageCount>

    @Query("SELECT activityType, COUNT(*) AS cnt FROM entry_activities WHERE entryId = :entryId GROUP BY activityType ORDER BY cnt DESC")
    suspend fun getActivityBreakdownByEntryId(entryId: String): List<ActivityTypeBreakdown>

    // ---- usage stats (Flow) ----

    @Query("SELECT entryId, COUNT(*) AS usageCount, MAX(createdAt) AS lastUsedAt FROM entry_activities WHERE activityType IN (:usageTypes) GROUP BY entryId")
    fun observeUsageStats(usageTypes: List<ActivityType>): Flow<List<EntryUsageStatsRow>>

    @Query("SELECT entryId, COUNT(*) AS usageCount, MAX(createdAt) AS lastUsedAt FROM entry_activities WHERE activityType IN (:usageTypes) GROUP BY entryId")
    suspend fun getUsageStats(usageTypes: List<ActivityType>): List<EntryUsageStatsRow>
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
