package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.domain.model.entry.EntryType
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActiveByType(entryType: EntryType): Flow<List<EntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE entryType IN (:entryTypes) AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActiveByTypes(entryTypes: List<EntryType>): Flow<List<EntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE entryId IN (:entryIds) AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActiveByIds(entryIds: List<String>): Flow<List<EntryEntity>>

    @Query("SELECT DISTINCT entryType FROM vault_entries WHERE deletedAt IS NULL ORDER BY entryType")
    fun observeDistinctActiveEntryTypes(): Flow<List<EntryType>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActive(): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActiveByType(entryType: EntryType): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun pagingDeleted(): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActiveRecentlyUpdated(): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun pagingActiveRecentlyCreated(): PagingSource<Int, EntryEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getActive(): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getDeleted(): List<EntryEntity>

    @Query("SELECT * FROM vault_entries ORDER BY updatedAt DESC")
    suspend fun getAll(): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE entryId = :entryId LIMIT 1")
    suspend fun getById(entryId: String): EntryEntity?

    @Query("SELECT * FROM vault_entries WHERE entryId IN (:entryIds)")
    suspend fun getByIds(entryIds: List<String>): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getActiveByType(entryType: EntryType): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getActivePage(limit: Int, offset: Int): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getActiveRecentlyUpdated(limit: Int): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getActiveRecentlyCreated(limit: Int): List<EntryEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM vault_entries WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM vault_entries WHERE entryId = :entryId AND deletedAt IS NULL)")
    suspend fun existsActive(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM vault_entries WHERE deletedAt IS NULL")
    suspend fun countActive(): Int

    @Query("SELECT COUNT(*) FROM vault_entries WHERE deletedAt IS NULL AND entryType = :entryType")
    suspend fun countActiveByType(entryType: EntryType): Int

    @Query("SELECT COUNT(*) FROM vault_entries WHERE deletedAt IS NOT NULL")
    suspend fun countDeleted(): Int

    // === Strict Insert (fail on duplicate PK) ===

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(entry: EntryEntity)

    // === Import Upsert (overwrite on duplicate) ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertForImport(entry: EntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllForImport(entries: List<EntryEntity>)

    // === Update ===

    @Update
    suspend fun update(entry: EntryEntity)

    // 带乐观锁版本校验的更新，返回影响行数（0 表示版本冲突）
    @Query("UPDATE vault_entries SET summaryBlob = :summaryBlob, version = version + 1, updatedAt = :updatedAt WHERE entryId = :entryId AND version = :expectedVersion")
    suspend fun optimisticUpdate(
        entryId: String,
        expectedVersion: Int,
        summaryBlob: ByteArray,
        updatedAt: Long
    ): Int

    // 带乐观锁版本校验的软删除，返回影响行数（0 表示版本冲突）
    @Query("UPDATE vault_entries SET deletedAt = :deletedAt, version = version + 1, updatedAt = :updatedAt WHERE entryId = :entryId AND version = :expectedVersion")
    suspend fun optimisticSoftDelete(
        entryId: String,
        expectedVersion: Int,
        deletedAt: Long,
        updatedAt: Long
    ): Int

    // ---- soft delete / restore ----

    @Query("UPDATE vault_entries SET deletedAt = :timestamp, updatedAt = :timestamp WHERE entryId = :entryId")
    suspend fun softDeleteById(entryId: String, timestamp: Long)

    // 带乐观锁版本校验的恢复，返回影响行数（0 表示版本冲突）
    @Query("UPDATE vault_entries SET deletedAt = NULL, version = version + 1, updatedAt = :now WHERE entryId = :entryId AND version = :expectedVersion AND deletedAt IS NOT NULL")
    suspend fun restoreOptimistic(entryId: String, expectedVersion: Int, now: Long): Int

    @Query("UPDATE vault_entries SET deletedAt = NULL, updatedAt = :now WHERE entryId = :entryId")
    suspend fun restoreById(entryId: String, now: Long)

    // === Maintenance API ===

    @Query("DELETE FROM vault_entries WHERE entryId = :entryId")
    suspend fun deleteById(entryId: String)

    @Query("DELETE FROM vault_entries WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeleted(before: Long)

    @Query("DELETE FROM vault_entries")
    suspend fun clear(): Int
}
