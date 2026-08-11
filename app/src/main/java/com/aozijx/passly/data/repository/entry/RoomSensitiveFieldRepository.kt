package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.auth.model.AuthorizationPermit
import com.aozijx.passly.domain.auth.model.AuthorizationScope
import com.aozijx.passly.domain.auth.port.AuthorizationPermitVerifier
import com.aozijx.passly.domain.authentication.SensitiveAccessAction
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.entry.SensitiveFieldCodec
import com.aozijx.passly.data.model.entity.EntrySensitiveFieldEntity
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.repository.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.security.crypto.SecureString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSensitiveFieldRepository @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: SecureSessionAccessState,
    private val transactionRunner: VaultTransactionRunner,
    private val codec: SensitiveFieldCodec,
    private val clock: Clock,
    private val permitVerifier: AuthorizationPermitVerifier,
) : SensitiveFieldRepository {
    override suspend fun getPresence(entryId: EntryId): SensitiveFieldPresence =
        if (!sessionState.hasFullSecureSessionAccess()) SensitiveFieldPresence(entryId, emptySet())
        else sessionManager.query {
            SensitiveFieldPresence(
                entryId,
                sensitiveFieldQueryDao().getKeys(entryId.value)
                    .mapTo(linkedSetOf(), SensitiveFieldKey::valueOf)
            )
        }

    override suspend fun reveal(
        entryId: EntryId,
        key: SensitiveFieldKey,
        permit: AuthorizationPermit,
    ): RevealedSensitiveField? = revealMany(entryId, setOf(key), permit).singleOrNull()

    override suspend fun revealMany(
        entryId: EntryId,
        keys: Set<SensitiveFieldKey>,
        permit: AuthorizationPermit,
    ): List<RevealedSensitiveField> = if (
        !sessionState.hasFullSecureSessionAccess() ||
        !permitVerifier.consume(
            permit,
            AuthorizationScope.SensitiveFields(
                entryId = entryId,
                fieldKeys = keys,
                action = SensitiveAccessAction.REVEAL,
            ),
        )
    ) emptyList() else {
        sessionManager.query {
            keys.mapNotNull { key ->
                val entity = sensitiveFieldQueryDao().getField(entryId.value, key.name)
                    ?: return@mapNotNull null
                RevealedSensitiveField(
                    entryId = entryId,
                    key = key,
                    value = SecureString.fromString(
                        codec.decrypt(entryId.value, key, entity.valueCipher)
                    ),
                )
            }
        }
    }

    override suspend fun upsert(
        entryId: EntryId,
        key: SensitiveFieldKey,
        value: SensitiveValue
    ): AppResult<Unit> = transactionRunner.write("sensitive-field.upsert") {
        val chars = value.toCharArray()
        try {
            sensitiveFieldCommandDao().upsert(
                EntrySensitiveFieldEntity(
                    entryId = entryId.value,
                    fieldKey = key.name,
                    valueCipher = codec.encrypt(entryId.value, key, String(chars)),
                    keyVersion = 1,
                    updatedAt = clock.now()
                )
            )
        } finally {
            chars.fill('\u0000')
        }
    }

    override suspend fun delete(entryId: EntryId, key: SensitiveFieldKey): AppResult<Unit> =
        transactionRunner.write("sensitive-field.delete") {
            sensitiveFieldCommandDao().delete(entryId.value, key.name)
            Unit
        }
}
