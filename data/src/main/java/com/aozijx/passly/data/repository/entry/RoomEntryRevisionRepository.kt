package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.codec.entry.SecretFieldCodec
import com.aozijx.passly.data.codec.revision.EntryContentSnapshotCodec
import com.aozijx.passly.data.codec.revision.SensitiveRevisionSnapshotCodec
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.repository.entry.command.RestoreEntryRevisionExecutor
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.sensitive.RevealedRevisionSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import com.aozijx.passly.domain.sensitive.OwnedChars
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomEntryRevisionRepository @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val databaseTransactions: DatabaseTransactionRunner,
    private val sessionState: SecureSessionAccessState,
    private val contentCodec: EntryContentSnapshotCodec,
    private val sensitiveCodec: SensitiveRevisionSnapshotCodec,
    private val secretFieldCodec: SecretFieldCodec,
    private val permitConsumer: RevisionPermitConsumer,
    private val restoreExecutor: RestoreEntryRevisionExecutor,
) : EntryRevisionRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMetadata(entryId: EntryId): Flow<List<EntryRevisionMetadata>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else databaseSession.observeFlow {
                entryRevisionQueryDao().observeByEntryId(entryId.value)
                    .map { revisions -> revisions.map(EntryRevisionEntity::toRevisionMetadata) }
            }
        }

    override suspend fun loadRedacted(
        entryId: EntryId,
        revisionId: EntryRevisionId,
    ): AppResult<RedactedEntryRevision> = databaseTransactions.read("entry_revision_load") {
        val revision = entryRevisionQueryDao().getById(entryId.value, revisionId.value)
            ?: throw NotFound()
        val content = contentCodec.decrypt(revision.entryContentCipher, entryId.value)
        val sensitiveFingerprints = sensitiveCodec.decodeFingerprints(
            revision.sensitiveFieldCipherSet,
        )
        RedactedEntryRevision(
            metadata = revision.toRevisionMetadata(),
            profile = content.summary,
            secret = content.secret,
            links = content.links,
            attachmentIds = revisionAttachmentRefDao().getByRevisionId(revisionId.value)
                .mapTo(linkedSetOf()) { it.attachmentId },
            sensitiveFieldKeys = sensitiveFingerprints.keys,
            sensitiveFieldFingerprints = sensitiveFingerprints,
        )
    }

    override suspend fun reveal(
        entryId: EntryId,
        revisionId: EntryRevisionId,
        keys: Set<SensitiveFieldKey>,
        permit: AuthorizationPermit,
    ): AppResult<List<RevealedRevisionSensitiveField>> =
        databaseTransactions.read("entry_revision_reveal") {
            if (keys.isEmpty()) throw ValidationError()
            val revision = entryRevisionQueryDao().getById(entryId.value, revisionId.value)
                ?: throw NotFound()
            val fields = sensitiveCodec.decode(revision.sensitiveFieldCipherSet)
            val fieldsByKey = fields.associateBy { it.key }
            if (!fieldsByKey.keys.containsAll(keys)) throw NotFound()
            if (!permitConsumer.consumeReveal(permit, entryId, revisionId, keys)) {
                throw ValidationError()
            }
            keys.map { key ->
                RevealedRevisionSensitiveField(
                    revisionId = revisionId.value,
                    entryId = entryId,
                    key = key,
                    value = OwnedChars.fromString(
                        secretFieldCodec.decrypt(
                            entryId = entryId.value,
                            key = key,
                            cipher = fieldsByKey.getValue(key).valueCipher,
                        ),
                    ),
                )
            }
        }

    override suspend fun restore(
        entryId: EntryId,
        revisionId: EntryRevisionId,
        expectedVersion: EntryVersion,
        permit: AuthorizationPermit?,
    ): AppResult<EntryVersion> = restoreExecutor.execute(
        entryId = entryId,
        revisionId = revisionId,
        expectedVersion = expectedVersion,
        permit = permit,
    )
}

internal fun EntryRevisionEntity.toRevisionMetadata() = EntryRevisionMetadata(
    id = EntryRevisionId(revisionId),
    entryId = EntryId(entryId),
    version = EntryVersion(version),
    createdAtMs = createdAt,
    change = RevisionChange.valueOf(changeType),
)
