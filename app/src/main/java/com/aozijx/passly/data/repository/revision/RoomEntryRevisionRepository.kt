package com.aozijx.passly.data.repository.revision

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.revision.EntryContentSnapshotCodec
import com.aozijx.passly.data.codec.revision.SensitiveRevisionSnapshotCodec
import com.aozijx.passly.data.codec.entry.SensitiveFieldCodec
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import com.aozijx.passly.data.model.entity.EntrySensitiveFieldEntity
import com.aozijx.passly.data.repository.VaultTransactionRunner
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.internal.EntryActivityHelper
import com.aozijx.passly.data.repository.entry.internal.EntryRevisionHelper
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.auth.model.AuthorizationPermit
import com.aozijx.passly.domain.auth.model.AuthorizationScope
import com.aozijx.passly.domain.auth.model.SensitiveRevisionAccessAction
import com.aozijx.passly.domain.auth.port.AuthorizationPermitVerifier
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.revision.EntryRevision
import com.aozijx.passly.domain.entry.model.revision.RevisionType
import com.aozijx.passly.domain.entry.model.sensitive.RevealedRevisionSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.repository.EntryRevisionRepository
import com.aozijx.passly.domain.entry.service.SensitiveRevisionRestorePolicy
import com.aozijx.passly.security.crypto.SecureString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEntryRevisionRepository @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: SecureSessionAccessState,
    private val transactionRunner: VaultTransactionRunner,
    private val contentSnapshotCodec: EntryContentSnapshotCodec,
    private val sensitiveRevisionCodec: SensitiveRevisionSnapshotCodec,
    private val sensitiveFieldCodec: SensitiveFieldCodec,
    private val permitVerifier: AuthorizationPermitVerifier,
    private val revisionHelper: EntryRevisionHelper,
    private val activityHelper: EntryActivityHelper,
    private val clock: Clock,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
) : EntryRevisionRepository {

    override suspend fun getRevisions(entryId: String): List<EntryRevision> {
        if (!sessionState.hasFullSecureSessionAccess()) return emptyList()
        return sessionManager.query {
            val metadata = entryQueryDao().getById(entryId) ?: return@query emptyList()
            entryRevisionQueryDao().getByEntryId(entryId).map { entity ->
                entity.toDomain(
                    metadata,
                    revisionAttachmentRefDao().getByRevisionId(entity.revisionId)
                        .sortedBy { it.displayOrder }
                        .map { it.attachmentId },
                )
            }
        }
    }

    override suspend fun getLatestRevision(entryId: String): EntryRevision? {
        if (!sessionState.hasFullSecureSessionAccess()) return null
        return sessionManager.query {
            val entity = entryRevisionQueryDao().getLatest(entryId) ?: return@query null
            val metadata = entryQueryDao().getById(entryId) ?: return@query null
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
        return sessionManager.query {
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
        val result = transactionRunner.write("entry-revision.restore-sensitive") {
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
                changeType = RevisionType.VERSION_RESTORED,
            )
            activityHelper.recordActivity(
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
        val entry = EntryAggregate(
            header = EntryHeader(
                id = EntryId(entryId),
                entryType = metadata.entryType,
                version = EntryVersion(version),
                createdAt = metadata.createdAt,
                updatedAt = createdAt,
                deletedAt = metadata.deletedAt
            ),
            summary = snapshot.summary,
            secret = snapshot.secret,
        )
        return EntryRevision(
            revisionId = revisionId,
            version = version,
            entryId = entryId,
            entry = entry,
            links = snapshot.links,
            attachmentIds = attachmentIds,
            sensitiveFieldKeys = sensitiveRevisionCodec.decode(sensitiveFieldCipherSet)
                .mapTo(linkedSetOf()) { it.key },
            changeType = RevisionType.fromValue(changeType),
            createdAt = createdAt,
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
