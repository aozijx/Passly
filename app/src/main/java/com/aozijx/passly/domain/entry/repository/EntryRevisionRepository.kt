package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.domain.entry.model.revision.EntryRevision

interface EntryRevisionRepository {
    suspend fun getRevisions(entryId: String): List<EntryRevision>
    suspend fun getLatestRevision(entryId: String): EntryRevision?
}
