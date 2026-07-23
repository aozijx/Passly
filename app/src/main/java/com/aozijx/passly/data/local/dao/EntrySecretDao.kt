package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aozijx.passly.data.model.entity.EntrySecretEntity

@Dao
interface EntrySecretDao {

    // ---- get (suspend) ----

    @Query("SELECT * FROM entry_secrets WHERE entryId = :entryId LIMIT 1")
    suspend fun getByEntryId(entryId: String): EntrySecretEntity?

    @Query("SELECT * FROM entry_secrets WHERE entryId IN (:entryIds)")
    suspend fun getByEntryIds(entryIds: List<String>): List<EntrySecretEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM entry_secrets WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM entry_secrets")
    suspend fun count(): Int

    // === Strict Insert (fail on duplicate PK) ===

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(secret: EntrySecretEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(secrets: List<EntrySecretEntity>)

    // === Import Upsert (overwrite on duplicate) ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertForImport(secret: EntrySecretEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllForImport(secrets: List<EntrySecretEntity>)

    // === Update ===

    @Update
    suspend fun update(secret: EntrySecretEntity)

    @Query("UPDATE entry_secrets SET secretBlob = :secretBlob WHERE entryId = :entryId")
    suspend fun updateBlob(entryId: String, secretBlob: ByteArray)

    // === Maintenance API ===

    @Query("DELETE FROM entry_secrets WHERE entryId = :entryId")
    suspend fun delete(entryId: String)

    @Query("DELETE FROM entry_secrets")
    suspend fun clear(): Int
}
