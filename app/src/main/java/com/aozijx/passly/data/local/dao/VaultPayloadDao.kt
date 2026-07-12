package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aozijx.passly.data.local.database.DatabaseConfig
import com.aozijx.passly.data.model.entity.VaultPayloadEntity

@Dao
interface VaultPayloadDao {

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_CREDENTIALS} WHERE entryId = :entryId LIMIT 1")
    suspend fun getByEntryId(entryId: String): VaultPayloadEntity?

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_CREDENTIALS} WHERE entryId IN (:entryIds)")
    suspend fun getByEntryIds(entryIds: List<String>): List<VaultPayloadEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseConfig.TABLE_CREDENTIALS} WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // ---- insert / update ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credential: VaultPayloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(credentials: List<VaultPayloadEntity>)

    @Update
    suspend fun update(credential: VaultPayloadEntity)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseConfig.TABLE_CREDENTIALS} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_CREDENTIALS}")
    suspend fun clear()
}
