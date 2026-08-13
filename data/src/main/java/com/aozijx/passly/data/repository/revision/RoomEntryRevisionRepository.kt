package com.aozijx.passly.data.repository.revision

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.codec.revision.EntryContentSnapshotCodec
import com.aozijx.passly.data.codec.revision.SensitiveRevisionSnapshotCodec
import com.aozijx.passly.data.codec.entry.SensitiveFieldCodec
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.data.local.database.entity.EntrySensitiveFieldEntity
import com.aozijx.passly.data.local.database.DatabaseTransactionRunner
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.command.EntryActivityWriter
import com.aozijx.passly.data.repository.entry.command.EntryRevisionWriter
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveRevisionAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.history.EntryRevision
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.sensitive.RevealedRevisionSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import com.aozijx.passly.domain.entry.policy.SensitiveRevisionRestorePolicy
import com.aozijx.passly.security.crypto.SecureString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomEntryRevisionRepository @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
    private val databaseTransactions: DatabaseTransactionRunner,
    private val contentSnapshotCodec: EntryContentSnapshotCodec,
    private val sensitiveRevisionCodec: SensitiveRevisionSnapshotCodec,
    private val sensitiveFieldCodec: SensitiveFieldCodec,
    private val permitVerifier: AuthorizationPermitVerifier,
    private val revisionHelper: EntryRevisionWriter,
    private val activityWriter: EntryActivityWriter,
    private val clock: DatabaseClock,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
) : EntryRevisionRepository {

    override suspend fun getRevisions(entryId: EntryId): List<EntryRevision> {
        if (!sessionState.hasFullSecureSessionAccess()) return emptyList()
        return databaseSession.query {
            val metadata = entryQueryDao().getById(entryId.value) ?: return@query emptyList()
            entryRevisionQueryDao().getByEntryId(entryId.value).map { entity ->
                entity.toDomain(
                    metadata,
                    revisionAttachmentRefDao().getByRevisionId(entity.revisionId)
                        .sortedBy { it.displayOrder }
                        .map { it.attachmentId },
                )
            }
        }
    }

    override suspend fun getLatestRevision(entryId: EntryId): EntryRevision? {
        if (!sessionState.hasFullSecureSessionAccess()) return null
        return databaseSession.query {
            val entity = entryRevisionQueryDao().getLatest(entryId.value) ?: return@query null
            val metadata = entryQueryDao().getById(entryId.value) ?: return@query null
            entity.toDomain(
                metadata,
                revisionAttachmentRefDao().getByRevisionId(entity.revisionId)
                    .sortedBy { it.displayOrder }
                    .map { it.attachmentId },
            )
        }
    }

    override suspend fun revealSensitiveFields(
        entryId: EntryId,
        revisionId: String,
        fieldKeys: Set<SensitiveFieldKey>,
        permit: AuthorizationPermit,
    ): List<RevealedRevisionSensitiveField> {
        if (
            fieldKeys.isEmpty() ||
            !sessionState.hasFullSecureSessionAccess() ||
            !permitVerifier.consume(
                permit,
                sensitiveRevisionScope(
                    entryId = entryId,
                    revisionId = revisionId,
                    fieldKeys = fieldKeys,
                    action = SensitiveRevisionAccessAction.REVEAL,
                ),
            )
        ) {
            return emptyList()
        }
        return databaseSession.query {
            val revision = entryRevisionQueryDao().getById(entryId.value, revisionId)
                ?: return@query emptyList()
            val snapshots = sensitiveRevisionCodec.decode(revision.sensitiveFieldCipherSet)
                .associateBy { it.key }
            val revealed = mutableListOf<RevealedRevisionSensitiveField>()
            try {
                fieldKeys.forEach { key ->
                    val snapshot = snapshots[key] ?: return@forEach
                    revealed += RevealedRevisionSensitiveField(
                        revisionId = revisionId,
                        entryId = entryId,
                        key = key,
                        value = SecureString.fromString(
                            sensitiveFieldCodec.decrypt(
                                entryId = entryId.value,
                                key = key,
                                cipher = snapshot.valueCipher,
                            )
                        ),
                    )
                }
                revealed
            } catch (error: Throwable) {
                revealed.forEach { it.value.wipe() }
                throw error
            }
        }
    }

    override suspend fun restoreSensitiveFields(
        entryId: EntryId,
        revisionId: String,
        fieldKeys: Set<SensitiveFieldKey>,
        permit: AuthorizationPermit,
    ): AppResult<Unit> {
        val result = databaseTransactions.write("entry-revision.restore-sensitive") {
            val revision = entryRevisionQueryDao().getById(entryId.value, revisionId)
                ?: throw NotFound()
            entryQueryDao().getById(entryId.value) ?: throw NotFound()
            val snapshots = sensitiveRevisionCodec.decode(revision.sensitiveFieldCipherSet)
            val currentKeys = sensitiveFieldQueryDao().getKeys(entryId.value)
                .mapTo(linkedSetOf(), SensitiveFieldKey::valueOf)
            val historicalKeys = snapshots.mapTo(linkedSetOf()) { it.key }
            val affectedKeys = SensitiveRevisionRestorePolicy.affectedFields(
                currentFields = currentKeys,
                historicalFields = historicalKeys,
            )
            if (
                !SensitiveRevisionRestorePolicy.isExactAuthorization(
                    authorizedFields = fieldKeys,
                    currentFields = currentKeys,
                    historicalFields = historicalKeys,
                )
            ) {
                throw ValidationError()
            }
            if (
                !permitVerifier.consume(
                    permit,
                    sensitiveRevisionScope(
                        entryId = entryId,
                        revisionId = revisionId,
                        fieldKeys = affectedKeys,
                        action = SensitiveRevisionAccessAction.RESTORE,
                    ),
                )
            ) {
                throw SessionModeRestricted()
            }

            val now = clock.now()
            sensitiveFieldCommandDao().deleteAll(entryId.value)
            snapshots.forEach { snapshot ->
                sensitiveFieldCommandDao().upsert(
                    EntrySensitiveFieldEntity(
                        entryId = entryId.value,
                        fieldKey = snapshot.key.name,
                        valueCipher = snapshot.valueCipher,
                        keyVersion = snapshot.keyVersion,
                        updatedAt = now,
                    )
                )
            }
            revisionHelper.snapshotCurrent(
                db = this,
                entryId = entryId.value,
                now = now,
                change = RevisionChange.VERSION_RESTORED,
            )
            activityWriter.recordActivity(
                db = this,
                entryId = entryId.value,
                activityType = ActivityType.SENSITIVE_CHANGE,
                now = now,
            )
        }
        result.onSuccessSuspend { attachmentGarbageCollector.drain() }
        return result
    }

    private suspend fun EntryRevisionEntity.toDomain(
        metadata: EntryEntity,
        attachmentIds: List<String>,
    ): EntryRevision {
        val snapshot = contentSnapshotCodec.decrypt(entryContentCipher, entryId)
        val entry = Entry(
            identity = EntryIdentity(
                id = EntryId(entryId),
                type = metadata.entryType,
                version = EntryVersion(version),
                timestamps = EntryTimestamps(
                    createdAtMs = metadata.createdAt,
                    updatedAtMs = createdAt,
                    deletedAtMs = metadata.deletedAt,
                ),
            ),
            profile = snapshot.summary,
            secret = snapshot.secret,
        )
        return EntryRevision(
            id = EntryRevisionId(revisionId),
            entryId = EntryId(entryId),
            version = EntryVersion(version),
            createdAtMs = createdAt,
            change = runCatching { RevisionChange.valueOf(changeType) }
                .getOrDefault(RevisionChange.VALUE_CHANGED),
            snapshot = entry,
            links = snapshot.links,
            attachmentIds = attachmentIds.toSet(),
            sensitiveFieldKeys = sensitiveRevisionCodec.decode(sensitiveFieldCipherSet)
                .mapTo(linkedSetOf()) { it.key },
        )
    }

    private fun sensitiveRevisionScope(
        entryId: EntryId,
        revisionId: String,
        fieldKeys: Set<SensitiveFieldKey>,
        action: SensitiveRevisionAccessAction,
    ) = AuthorizationScope.SensitiveRevision(
        entryId = entryId,
        revisionId = revisionId,
        fieldKeys = fieldKeys,
        action = action,
    )
}
