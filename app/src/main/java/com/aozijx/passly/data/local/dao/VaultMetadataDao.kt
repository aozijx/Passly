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
    fun observeByType(entryType: EntryType): Flow<List<VaultMetadataEntity>>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryType IN (:entryTypes) AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeByEntryTypes(entryTypes: List<EntryType>): Flow<List<VaultMetadataEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActive(): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingByType(entryType: EntryType): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun pagingDeleted(): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingRecentlyUpdated(): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun pagingRecentlyCreated(): PagingSource<Int, VaultMetadataEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getActive(): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getDeleted(): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} ORDER BY updatedAt DESC")
    suspend fun getAll(): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryId = :entryId LIMIT 1")
    suspend fun getById(entryId: String): VaultMetadataEntity?

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryId IN (:entryIds)")
    suspend fun getByIds(entryIds: List<String>): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getByType(entryType: EntryType): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentlyUpdated(limit: Int): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentlyCreated(limit: Int): List<VaultMetadataEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL")
    suspend fun countActive(): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NULL AND entryType = :entryType")
    suspend fun countByType(entryType: EntryType): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NOT NULL")
    suspend fun countDeleted(): Int

    // ---- insert / update ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<VaultMetadataEntity>)

    @Update
    suspend fun update(entry: VaultMetadataEntity)

    /**
     * 带乐观锁版本校验的更新。
     * 仅当 entryId 和 entryVersion 同时匹配时才执行更新，
     * 并将 entryVersion 自增 1。
     *
     * @return 影响的行数（0 表示版本冲突）
     */
    @Query("UPDATE ${DatabaseSchema.TABLE_METADATA} SET metadataBlob = :metadataBlob, entryVersion = entryVersion + 1, updatedAt = :updatedAt WHERE entryId = :entryId AND entryVersion = :expectedVersion")
    suspend fun optimisticUpdate(
        entryId: String,
        expectedVersion: Int,
        metadataBlob: ByteArray,
        updatedAt: Long
    ): Int

    /**
     * 带乐观锁版本校验的软删除。
     *
     * @return 影响的行数（0 表示版本冲突）
     */
    @Query("UPDATE ${DatabaseSchema.TABLE_METADATA} SET deletedAt = :deletedAt, entryVersion = entryVersion + 1, updatedAt = :updatedAt WHERE entryId = :entryId AND entryVersion = :expectedVersion")
    suspend fun optimisticSoftDelete(
        entryId: String,
        expectedVersion: Int,
        deletedAt: Long,
        updatedAt: Long
    ): Int

    // ---- soft delete / restore ----

    @Query("UPDATE ${DatabaseSchema.TABLE_METADATA} SET deletedAt = :timestamp, updatedAt = :timestamp WHERE entryId = :entryId")
    suspend fun softDelete(entryId: String, timestamp: Long)

    /**
     * 带乐观锁版本校验的恢复。
     *
     * @return 影响的行数（0 表示版本冲突）
     */
    @Query("UPDATE ${DatabaseSchema.TABLE_METADATA} SET deletedAt = NULL, entryVersion = entryVersion + 1, updatedAt = :now WHERE entryId = :entryId AND entryVersion = :expectedVersion AND deletedAt IS NOT NULL")
    suspend fun restoreOptimistic(entryId: String, expectedVersion: Int, now: Long): Int

    @Query("UPDATE ${DatabaseSchema.TABLE_METADATA} SET deletedAt = NULL, updatedAt = :now WHERE entryId = :entryId")
    suspend fun restore(entryId: String, now: Long)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseSchema.TABLE_METADATA} WHERE entryId = :entryId")
    suspend fun deleteById(entryId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_METADATA} WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeleted(before: Long)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_METADATA}")
    suspend fun clear()
}