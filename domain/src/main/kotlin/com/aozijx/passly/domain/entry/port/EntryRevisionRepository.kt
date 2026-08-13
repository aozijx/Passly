package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.history.EntryRevision
import com.aozijx.passly.domain.entry.model.sensitive.RevealedRevisionSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

interface EntryRevisionRepository {
    suspend fun getRevisions(entryId: EntryId): List<EntryRevision>
    suspend fun getLatestRevision(entryId: EntryId): EntryRevision?

    suspend fun revealSensitiveFields(
        entryId: EntryId,
        revisionId: String,
        fieldKeys: Set<SensitiveFieldKey>,
        permit: AuthorizationPermit,
    ): List<RevealedRevisionSensitiveField>

    /**
     * Replaces current high-sensitivity fields with the exact revision snapshot.
     * [fieldKeys] must equal the union of current and historical keys so a permit cannot
     * authorize only the values being added while silently deleting another protected value.
     */
    suspend fun restoreSensitiveFields(
        entryId: EntryId,
        revisionId: String,
        fieldKeys: Set<SensitiveFieldKey>,
        permit: AuthorizationPermit,
    ): AppResult<Unit>
}
