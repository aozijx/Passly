package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.domain.model.entry.EntryType
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultMetadataDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<VaultMetadataEntity>>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActiveByType(entryType: EntryType): Flow<List<VaultMetadataEntity>>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryType IN (:entryTypes) AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActiveByTypes(entryTypes: List<EntryType>): Flow<List<VaultMetadataEntity>>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryId IN (:entryIds) AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActiveByIds(entryIds: List<String>): Flow<List<VaultMetadataEntity>>

    @Query("SELECT DISTINCT entryType FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY entryType")
    fun observeDistinctActiveEntryTypes(): Flow<List<EntryType>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActive(): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActiveByType(entryType: EntryType): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun pagingDeleted(): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActiveRecentlyUpdated(): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun pagingActiveRecentlyCreated(): PagingSource<Int, VaultMetadataEntity>

    // ---- get (suspend) ----

    // 获取所有活跃条目
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getActive(): List<VaultMetadataEntity>

    // 获取所有已删除条目
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getDeleted(): List<VaultMetadataEntity>

    // 返回所有条目（含已删除）
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} ORDER BY updatedAt DESC")
    suspend fun getAll(): List<VaultMetadataEntity>

    // 按 PK 查询（不论是否已删除）
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryId = :entryId LIMIT 1")
    suspend fun getById(entryId: String): VaultMetadataEntity?

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryId IN (:entryIds)")
    suspend fun getByIds(entryIds: List<String>): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getActiveByType(entryType: EntryType): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getActivePage(limit: Int, offset: Int): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getActiveRecentlyUpdated(limit: Int): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getActiveRecentlyCreated(limit: Int): List<VaultMetadataEntity>

    // ---- exists ----

    // 是否存在指定 entryId 的行（不论是否已删除）
    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // 是否存在活跃（未删除）的指定条目
    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryId = :entryId AND deletedAt IS NULL)")
    suspend fun existsActive(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL")
    suspend fun countActive(): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL AND entryType = :entryType")
    suspend fun countActiveByType(entryType: EntryType): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NOT NULL")
    suspend fun countDeleted(): Int

    // === Strict Insert (fail on duplicate PK) ===

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(entry: VaultMetadataEntity)

    // === Import Upsert (overwrite on duplicate) ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertForImport(entry: VaultMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllForImport(entries: List<VaultMetadataEntity>)

    // === Update ===

    @Update
    suspend fun update(entry: VaultMetadataEntity)

    // 带乐观锁版本校验的更新，返回影响行数（0 表示版本冲突）
    @Query("UPDATE ${DatabaseSchema.TABLE_METADATA} SET metadataBlob = :metadataBlob, entryVersion = entryVersion + 1, updatedAt = :updatedAt WHERE entryId = :entryId AND entryVersion = :expectedVersion")
    suspend fun optimisticUpdate(
        entryId: String,
        expectedVersion: Int,
        metadataBlob: ByteArray,
        updatedAt: Long
    ): Int

    // 带乐观锁版本校验的软删除，返回影响行数（0 表示版本冲突）
    @Query("UPDATE ${DatabaseSchema.TABLE_METADATA} SET deletedAt = :deletedAt, entryVersion = entryVersion + 1, updatedAt = :updatedAt WHERE entryId = :entryId AND entryVersion = :expectedVersion")
    suspend fun optimisticSoftDelete(
        entryId: String,
        expectedVersion: Int,
        deletedAt: Long,
        updatedAt: Long
    ): Int

    // ---- soft delete / restore ----

    @Query("UPDATE ${DatabaseSchema.TABLE_METADATA} SET deletedAt = :timestamp, updatedAt = :timestamp WHERE entryId = :entryId")
    suspend fun softDeleteById(entryId: String, timestamp: Long)

    // 带乐观锁版本校验的恢复，返回影响行数（0 表示版本冲突）
    @Query("UPDATE ${DatabaseSchema.TABLE_METADATA} SET deletedAt = NULL, entryVersion = entryVersion + 1, updatedAt = :now WHERE entryId = :entryId AND entryVersion = :expectedVersion AND deletedAt IS NOT NULL")
    suspend fun restoreOptimistic(entryId: String, expectedVersion: Int, now: Long): Int

    @Query("UPDATE ${DatabaseSchema.TABLE_METADATA} SET deletedAt = NULL, updatedAt = :now WHERE entryId = :entryId")
    suspend fun restoreById(entryId: String, now: Long)

    // === Maintenance API ===

    @Query("DELETE FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryId = :entryId")
    suspend fun deleteById(entryId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeleted(before: Long)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_METADATA}")
    suspend fun clear(): Int
}
