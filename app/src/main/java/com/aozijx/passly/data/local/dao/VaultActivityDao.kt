package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.DatabaseConfig
import com.aozijx.passly.data.model.entity.ActivityType
import com.aozijx.passly.data.model.entity.VaultActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultActivityDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ACTIVITY} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun observeByEntryId(entryId: String): Flow<List<VaultActivityEntity>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ACTIVITY} ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VaultActivityEntity>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ACTIVITY} WHERE activityType = :activityType ORDER BY createdAt DESC")
    fun observeByType(activityType: ActivityType): Flow<List<VaultActivityEntity>>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ACTIVITY} WHERE activityId = :activityId LIMIT 1")
    suspend fun getById(activityId: String): VaultActivityEntity?

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ACTIVITY} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, VaultActivityEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ACTIVITY} ORDER BY createdAt DESC")
    fun pagingAll(): PagingSource<Int, VaultActivityEntity>

    // ---- insert ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(activity: VaultActivityEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(activities: List<VaultActivityEntity>)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseConfig.TABLE_ACTIVITY} WHERE activityId = :activityId")
    suspend fun deleteById(activityId: String)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_ACTIVITY} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_ACTIVITY}")
    suspend fun clear()

    @Query("DELETE FROM ${DatabaseConfig.TABLE_ACTIVITY} WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long)

    // ---- maintenance ----

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_ACTIVITY}")
    suspend fun count(): Int
}