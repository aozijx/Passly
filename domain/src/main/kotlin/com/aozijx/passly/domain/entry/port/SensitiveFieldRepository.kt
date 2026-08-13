package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence

/**
 * Persistence boundary for high-sensitivity values.
 *
 * It deliberately has no method that decrypts every field of an entry. Authorization and
 * sensitive-key lifetime are enforced by the domain service and security implementation above
 * this storage port.
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

}
