package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.sensitive.RevealedRevisionSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import kotlinx.coroutines.flow.Flow

interface EntryRevisionRepository {
    suspend fun initializeHistory(): AppResult<Unit>

    fun observeMetadata(entryId: EntryId): Flow<List<EntryRevisionMetadata>>

    suspend fun loadRedacted(
        entryId: EntryId,
        revisionId: EntryRevisionId,
    ): AppResult<RedactedEntryRevision>

    suspend fun reveal(
        entryId: EntryId,
        revisionId: EntryRevisionId,
        keys: Set<SensitiveFieldKey>,
        permit: AuthorizationPermit,
    ): AppResult<List<RevealedRevisionSensitiveField>>

    suspend fun restore(
        entryId: EntryId,
        revisionId: EntryRevisionId,
        expectedVersion: EntryVersion,
        permit: AuthorizationPermit?,
    ): AppResult<EntryVersion>
}
