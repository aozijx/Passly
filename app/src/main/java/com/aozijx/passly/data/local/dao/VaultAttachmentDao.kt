package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.DatabaseConfig
import com.aozijx.passly.data.model.entity.VaultAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultAttachmentDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ATTACHMENTS} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun observeByEntryId(entryId: String): Flow<List<VaultAttachmentEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ATTACHMENTS} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, VaultAttachmentEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ATTACHMENTS} WHERE attachmentId = :attachmentId")
    suspend fun getById(attachmentId: String): VaultAttachmentEntity?

    // ---- insert ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: VaultAttachmentEntity)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseConfig.TABLE_ATTACHMENTS} WHERE attachmentId = :attachmentId")
    suspend fun deleteById(attachmentId: String)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_ATTACHMENTS} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_ATTACHMENTS} WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseConfig.TABLE_ATTACHMENTS} WHERE attachmentId = :attachmentId)")
    suspend fun exists(attachmentId: String): Boolean
}