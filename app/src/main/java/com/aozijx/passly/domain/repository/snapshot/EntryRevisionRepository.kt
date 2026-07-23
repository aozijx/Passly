package com.aozijx.passly.domain.repository.snapshot

import com.aozijx.passly.domain.model.snapshot.EntryRevision

interface EntryRevisionRepository {
    suspend fun getRevisions(entryId: String): List<EntryRevision>
    suspend fun getLatestRevision(entryId: String): EntryRevision?
}
