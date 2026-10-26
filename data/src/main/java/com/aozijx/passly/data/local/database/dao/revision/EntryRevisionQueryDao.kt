package com.aozijx.passly.data.local.database.dao.revision

import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryRevisionQueryDao {

    @Query("SELECT * FROM entry_revisions WHERE entryId = :entryId ORDER BY version DESC")
    fun observeByEntryId(entryId: String): Flow<List<EntryRevisionEntity>>

    @Query("SELECT * FROM entry_revisions WHERE entryId = :entryId ORDER BY version DESC")
    suspend fun getByEntryId(entryId: String): List<EntryRevisionEntity>

    @Query("SELECT * FROM entry_revisions WHERE entryId = :entryId AND version = :version")
    suspend fun getByVersion(entryId: String, version: Int): EntryRevisionEntity?

    @Query("SELECT * FROM entry_revisions WHERE entryId = :entryId AND revisionId = :revisionId LIMIT 1")
    suspend fun getById(entryId: String, revisionId: String): EntryRevisionEntity?

    @Query("SELECT * FROM entry_revisions WHERE entryId = :entryId ORDER BY version DESC LIMIT 1")
    suspend fun getLatest(entryId: String): EntryRevisionEntity?

    @Query("SELECT COUNT(*) FROM entry_revisions")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM entry_revisions WHERE iconCustomReference = :path")
    suspend fun countByIconCustomReference(path: String): Int

    @Query("SELECT DISTINCT iconCustomReference FROM entry_revisions WHERE entryId = :entryId AND iconCustomReference IS NOT NULL")
    suspend fun getIconReferences(entryId: String): List<String>

    @Query("SELECT DISTINCT iconCustomReference FROM entry_revisions WHERE iconCustomReference IS NOT NULL")
    suspend fun getAllIconReferences(): List<String>

    @Query("SELECT DISTINCT iconCustomReference FROM entry_revisions WHERE entryId = :entryId AND iconCustomReference IS NOT NULL AND version <= (SELECT version FROM entry_revisions WHERE entryId = :entryId ORDER BY version DESC LIMIT 1 OFFSET :keepCount)")
    suspend fun getIconReferencesBeyondEntryLimit(entryId: String, keepCount: Int): List<String>

    @Query("SELECT DISTINCT iconCustomReference FROM entry_revisions WHERE iconCustomReference IS NOT NULL AND revisionId IN (SELECT revisionId FROM entry_revisions ORDER BY createdAt DESC, revisionId DESC LIMIT -1 OFFSET :keepCount)")
    suspend fun getIconReferencesBeyondGlobalLimit(keepCount: Int): List<String>
}
