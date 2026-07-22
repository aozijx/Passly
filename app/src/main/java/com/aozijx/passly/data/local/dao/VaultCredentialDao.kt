package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.model.entity.VaultCredentialEntity

@Dao
interface VaultCredentialDao {

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_CREDENTIALS} WHERE entryId = :entryId LIMIT 1")
    suspend fun getByEntryId(entryId: String): VaultCredentialEntity?

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_CREDENTIALS} WHERE entryId IN (:entryIds)")
    suspend fun getByEntryIds(entryIds: List<String>): List<VaultCredentialEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_CREDENTIALS} WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_CREDENTIALS}")
    suspend fun count(): Int

    // ---- insert / update ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credential: VaultCredentialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(credentials: List<VaultCredentialEntity>)

    @Update
    suspend fun update(credential: VaultCredentialEntity)

    @Query("UPDATE ${DatabaseSchema.TABLE_CREDENTIALS} SET credentialBlob = :credentialBlob WHERE entryId = :entryId")
    suspend fun updateBlob(entryId: String, credentialBlob: ByteArray)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseSchema.TABLE_CREDENTIALS} WHERE entryId = :entryId")
    suspend fun delete(entryId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_CREDENTIALS}")
    suspend fun clear()
}