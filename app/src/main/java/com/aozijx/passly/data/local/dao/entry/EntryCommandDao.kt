package com.aozijx.passly.data.local.dao.entry

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aozijx.passly.data.model.entity.EntryEntity

@Dao
interface EntryCommandDao {

    // === Strict Insert (fail on duplicate PK) ===

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(entry: EntryEntity)

    // === Import Upsert (overwrite on duplicate) ===

    @Upsert
    suspend fun upsertForImport(entry: EntryEntity)

    @Upsert
    suspend fun upsertAllForImport(entries: List<EntryEntity>)

    // === Optimistic Lock Update ===

    @Query("UPDATE vault_entries SET summaryBlob = :summaryBlob, capabilityFlags = :capabilityFlags, otpType = :otpType, version = version + 1, updatedAt = :updatedAt WHERE entryId = :entryId AND version = :expectedVersion")
    suspend fun optimisticUpdate(
        entryId: String,
        expectedVersion: Int,
        summaryBlob: ByteArray,
        capabilityFlags: Int,
        otpType: String?,
        updatedAt: Long
    ): Int

    // === Optimistic Lock Soft Delete ===

    @Query("UPDATE vault_entries SET deletedAt = :deletedAt, version = version + 1, updatedAt = :updatedAt WHERE entryId = :entryId AND version = :expectedVersion")
    suspend fun optimisticSoftDelete(
        entryId: String,
        expectedVersion: Int,
        deletedAt: Long,
        updatedAt: Long
    ): Int

    // === Optimistic Lock Restore ===

    @Query("UPDATE vault_entries SET deletedAt = NULL, version = version + 1, updatedAt = :now WHERE entryId = :entryId AND version = :expectedVersion AND deletedAt IS NOT NULL")
    suspend fun restoreOptimistic(entryId: String, expectedVersion: Int, now: Long): Int

    // === Hard Delete ===

    @Query("DELETE FROM vault_entries WHERE entryId = :entryId")
    suspend fun deleteById(entryId: String)

    @Query("DELETE FROM vault_entries WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeleted(before: Long)
}
