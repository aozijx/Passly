package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.codec.entry.SensitiveFieldCodec
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.SecureString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomSensitiveFieldRepository @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
    private val codec: SensitiveFieldCodec,
    private val permitVerifier: AuthorizationPermitVerifier,
) : SensitiveFieldRepository {
    override suspend fun getPresence(entryId: EntryId): SensitiveFieldPresence =
        if (!sessionState.hasFullSecureSessionAccess()) SensitiveFieldPresence(entryId, emptySet())
        else databaseSession.query {
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
        databaseSession.query {
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

}
