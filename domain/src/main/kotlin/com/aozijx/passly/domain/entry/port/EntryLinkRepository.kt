package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import kotlinx.coroutines.flow.Flow

interface EntryLinkRepository {
    fun observeAll(): Flow<List<EntryLink>>
    fun observeLinks(entryId: EntryId): Flow<List<EntryLink>>
    suspend fun getAll(): List<EntryLink>
    suspend fun getLinks(entryId: EntryId): List<EntryLink>
    suspend fun upsert(link: EntryLink): AppResult<Unit>
    suspend fun delete(linkId: EntryLinkId): AppResult<Unit>
}
