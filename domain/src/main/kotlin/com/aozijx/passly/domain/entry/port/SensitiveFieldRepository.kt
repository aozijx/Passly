package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence

/**
 * Persistence boundary for sensitive values stored as per-field ciphertext rows.
 *
 * [reveal]/[revealMany] decrypt only the requested fields and require a consumed permit, so a
 * caller never materializes the whole entry's secrets just to show one value. [readBundle]
 * returns the aggregated low-sensitivity secret (sensitive fields are `null` in it); [readAll]
 * reassembles the complete secret and is only meant for batch flows (revision, backup, recovery)
 * that genuinely need every field.
 */
interface SensitiveFieldRepository {
    suspend fun getPresence(entryId: EntryId): SensitiveFieldPresence

    suspend fun reveal(
        entryId: EntryId,
        key: SensitiveFieldKey,
        permit: AuthorizationPermit,
    ): RevealedSensitiveField?

    suspend fun revealMany(
        entryId: EntryId,
        keys: Set<SensitiveFieldKey>,
        permit: AuthorizationPermit,
    ): List<RevealedSensitiveField>

    /** Low-sensitivity aggregate; every field-level secret is `null` in the returned value. */
    suspend fun readBundle(entryId: EntryId): EntrySecret

    /** Complete secret assembled from the bundle plus every field-level ciphertext. */
    suspend fun readAll(entryId: EntryId): EntrySecret
}
