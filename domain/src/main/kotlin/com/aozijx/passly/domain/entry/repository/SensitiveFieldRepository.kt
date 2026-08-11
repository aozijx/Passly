package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.sensitive.SensitiveValue

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
        key: SensitiveFieldKey
    ): RevealedSensitiveField?

    suspend fun upsert(
        entryId: EntryId,
        key: SensitiveFieldKey,
        value: SensitiveValue
    ): AppResult<Unit>

    suspend fun delete(
        entryId: EntryId,
        key: SensitiveFieldKey
    ): AppResult<Unit>
}
