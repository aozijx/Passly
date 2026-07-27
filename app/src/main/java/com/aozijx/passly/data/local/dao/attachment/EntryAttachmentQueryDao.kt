package com.aozijx.passly.data.local.dao.attachment

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.model.entity.EntryAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryAttachmentQueryDao {

    @Query("SELECT * FROM entry_attachments WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun observeByEntryId(entryId: String): Flow<List<EntryAttachmentEntity>>

    @Query("SELECT * FROM entry_attachments WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, EntryAttachmentEntity>

    @Query("SELECT * FROM entry_attachments WHERE attachmentId = :attachmentId LIMIT 1")
    suspend fun getById(attachmentId: String): EntryAttachmentEntity?

    @Query("SELECT * FROM entry_attachments WHERE entryId = :entryId ORDER BY createdAt DESC")
    suspend fun getByEntryId(entryId: String): List<EntryAttachmentEntity>

    @Query("SELECT * FROM entry_attachments WHERE attachmentId IN (:attachmentIds)")
    suspend fun getByIds(attachmentIds: List<String>): List<EntryAttachmentEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM entry_attachments WHERE attachmentId = :attachmentId)")
    suspend fun exists(attachmentId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM entry_attachments WHERE entryId = :entryId)")
    suspend fun existsByEntryId(entryId: String): Boolean

    @Query("SELECT COUNT(*) FROM entry_attachments WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    @Query("SELECT COUNT(*) FROM entry_attachments WHERE entryId = :entryId AND status = 'COMMITTED'")
    suspend fun countCommittedByEntryId(entryId: String): Int

    @Query("SELECT * FROM entry_attachments WHERE entryId IN (:entryIds) AND status = 'COMMITTED' ORDER BY entryId, createdAt")
    suspend fun getCommittedByEntryIds(entryIds: List<String>): List<EntryAttachmentEntity>

    @Query("SELECT * FROM entry_attachments WHERE owner = :owner AND status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getPendingByOwner(owner: String): List<EntryAttachmentEntity>

    @Query("SELECT * FROM entry_attachments WHERE owner = :owner AND status = 'PENDING' ORDER BY createdAt DESC")
    fun observePendingByOwner(owner: String): Flow<List<EntryAttachmentEntity>>
}
