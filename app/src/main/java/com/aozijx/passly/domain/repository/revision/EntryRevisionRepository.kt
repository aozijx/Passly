package com.aozijx.passly.domain.repository.revision

import com.aozijx.passly.domain.model.revision.EntryRevision

interface EntryRevisionRepository {
    suspend fun getRevisions(entryId: String): List<EntryRevision>
    suspend fun getLatestRevision(entryId: String): EntryRevision?
}
