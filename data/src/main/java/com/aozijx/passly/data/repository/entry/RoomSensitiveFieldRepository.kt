package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.codec.entry.SecretFieldCodec
import com.aozijx.passly.data.codec.sensitiveFieldKeyFromStorageKey
import com.aozijx.passly.data.codec.toStorageKey
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SecretFieldKeys
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomSensitiveFieldRepository
    @Inject
    constructor(
        private val databaseSession: AppDatabaseSession,
        private val codec: SecretFieldCodec,
        private val fieldStore: SecretFieldStore,
        private val permitVerifier: AuthorizationPermitVerifier,
    ) : SensitiveFieldRepository {
        override suspend fun getPresence(entryId: EntryId): SensitiveFieldPresence =
            databaseSession.queryFullVault(
                onDenied = { SensitiveFieldPresence(entryId, emptySet()) },
            ) {
                SensitiveFieldPresence(
                    entryId,
                    secretFieldQueryDao()
                        .getKeys(entryId.value)
                        .mapNotNull { keyName ->
                            if (keyName == SecretFieldKeys.STRUCT_BUNDLE) {
                                null
                            } else {
                                sensitiveFieldKeyFromStorageKey(keyName)
                            }
                        }.toSet(),
                )
            }

        override suspend fun reveal(
            entryId: EntryId,
            key: SensitiveFieldKey,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): RevealedSensitiveField? = revealMany(entryId, setOf(key), action, permit).singleOrNull()

        override suspend fun revealMany(
            entryId: EntryId,
            keys: Set<SensitiveFieldKey>,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): List<RevealedSensitiveField> =
            databaseSession.queryFullVault(
                onDenied = { emptyList() },
            ) {
                if (!permitVerifier.consume(
                        permit,
                        AuthorizationScope.SensitiveFields(
                            entryId = entryId,
                            fieldKeys = keys,
                            action = action,
                        ),
                    )
                ) {
                    return@queryFullVault emptyList()
                }
                buildSensitiveResults(
                    keys = keys,
                    create = { key ->
                        val entity =
                            secretFieldQueryDao().getField(entryId.value, key.toStorageKey())
                                ?: return@buildSensitiveResults null
                        OwnedChars.fromString(codec.decrypt(entryId.value, key, entity.valueCipher))
                    },
                    assemble = { key, value -> RevealedSensitiveField(entryId, key, value) },
                )
            }

        override suspend fun readBundle(entryId: EntryId): EntrySecret =
            databaseSession.queryFullVault(
                onDenied = { EntrySecret() },
            ) {
                val entryType = entryQueryDao().getById(entryId.value)?.entryType ?: return@queryFullVault EntrySecret()
                fieldStore.readBundle(this, entryId.value, entryType)
            }
    }
