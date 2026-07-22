package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.model.entity.VaultAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultAttachmentDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun observeByEntryId(entryId: String): Flow<List<VaultAttachmentEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, VaultAttachmentEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId = :attachmentId")
    suspend fun getById(attachmentId: String): VaultAttachmentEntity?

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId ORDER BY createdAt DESC")
    suspend fun getByEntryId(entryId: String): List<VaultAttachmentEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId = :attachmentId)")
    suspend fun exists(attachmentId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    // ---- insert ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: VaultAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<VaultAttachmentEntity>)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId = :attachmentId")
    suspend fun deleteById(attachmentId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT}")
    suspend fun clear(): Int
}