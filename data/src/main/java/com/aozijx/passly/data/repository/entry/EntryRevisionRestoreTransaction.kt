package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.data.codec.entry.SecretBundleCodec
import com.aozijx.passly.data.codec.revision.EntryContentSnapshotCodec
import com.aozijx.passly.data.codec.revision.SensitiveRevisionSnapshotCodec
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.AttachmentRefEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity
import com.aozijx.passly.data.local.database.entity.RevisionAttachmentRefEntity
import com.aozijx.passly.data.mapper.entry.toDatabaseFlags
import com.aozijx.passly.data.repository.entry.command.EntryActivityWriter
import com.aozijx.passly.data.repository.entry.command.EntryRevisionWriter
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.attachment.AttachmentStatus
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.query.EntryCapability
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.policy.SensitiveRevisionRestorePolicy
import javax.inject.Inject

internal data class RestoredRevision(
    val version: EntryVersion,
    val previousIconPath: String?,
    val restoredIconPath: String?,
)

/** The database-only core of revision restore. The caller supplies the Room transaction. */
internal class EntryRevisionRestoreTransaction @Inject constructor(
    private val contentCodec: EntryContentSnapshotCodec,
    private val bundleCodec: SecretBundleCodec,
    private val sensitiveCodec: SensitiveRevisionSnapshotCodec,
    private val permitConsumer: RevisionPermitConsumer,
    private val revisionWriter: EntryRevisionWriter,
    private val activityWriter: EntryActivityWriter,
) {
    suspend fun restore(
        db: AppDatabase,
        entryId: EntryId,
        revisionId: EntryRevisionId,
        expectedVersion: EntryVersion,
        permit: AuthorizationPermit?,
        now: Long,
    ): EntryVersion = restoreWithResources(
        db,
        entryId,
        revisionId,
        expectedVersion,
        permit,
        now,
    ).version

    suspend fun restoreWithResources(
        db: AppDatabase,
        entryId: EntryId,
        revisionId: EntryRevisionId,
        expectedVersion: EntryVersion,
        permit: AuthorizationPermit?,
        now: Long,
    ): RestoredRevision = with(db) {
        val current = entryQueryDao().getById(entryId.value) ?: throw NotFound()
        if (current.version != expectedVersion.value) throw Conflict()
        val revision = entryRevisionQueryDao().getById(entryId.value, revisionId.value)
            ?: throw NotFound()
        val content = contentCodec.decrypt(revision.entryContentCipher, entryId.value)
        if (content.secret.credential.kind != current.entryType.credentialKind) {
            throw ValidationError()
        }
        val historicalSensitive = sensitiveCodec.decode(revision.sensitiveFieldCipherSet)
        val currentSensitiveKeys = secretFieldQueryDao().getKeys(entryId.value)
            .mapNotNullTo(linkedSetOf()) { name ->
                SensitiveFieldKey.entries.firstOrNull { it.name == name }
            }
        val historicalSensitiveKeys = historicalSensitive.mapTo(linkedSetOf()) { it.key }
        val affectedSensitiveKeys = SensitiveRevisionRestorePolicy.affectedFields(
            currentFields = currentSensitiveKeys,
            historicalFields = historicalSensitiveKeys,
        )
        if (affectedSensitiveKeys.isNotEmpty()) {
            val authorized = permit != null && permitConsumer.consumeRestore(
                permit = permit,
                entryId = entryId,
                revisionId = revisionId,
                keys = affectedSensitiveKeys,
            )
            if (!authorized) throw ValidationError()
        }

        validateLinks(this, entryId, content.links)
        val revisionAttachments = revisionAttachmentRefDao().getByRevisionId(revisionId.value)
        revisionAttachments.forEach { ref ->
            if (attachmentResourceDao().getById(ref.resourceId) == null) throw NotFound()
        }

        val hasAttachments = revisionAttachments.isNotEmpty()
        val capabilityFlags = restoredCapabilities(
            content.secret,
            historicalSensitiveKeys,
            hasAttachments,
        ).toDatabaseFlags()
        val affected = entryCommandDao().optimisticUpdate(
            entryId = entryId.value,
            expectedVersion = expectedVersion.value,
            title = content.summary.title,
            username = content.summary.username,
            primaryUrl = content.summary.associations.primaryUrl,
            domains = content.summary.associations.domains,
            applicationIds = content.summary.associations.applicationIds,
            iconName = content.summary.icon.name,
            iconCustomReference = content.summary.icon.customReference,
            favorite = content.summary.favorite,
            tags = content.summary.tags,
            iconColor = content.summary.icon.color,
            expiresAt = content.summary.expiresAtMs,
            capabilityFlags = capabilityFlags,
            otpType = content.secret.otp?.config?.type?.name,
            updatedAt = now,
        )
        if (affected != 1) throw Conflict()

        replaceSecretRows(
            db = this,
            entryId = entryId.value,
            bundle = content.secret,
            sensitiveFields = historicalSensitive,
            now = now,
        )
        entryLinkCommandDao().deleteByEntryId(entryId.value)
        if (content.links.isNotEmpty()) {
            entryLinkCommandDao().insertAllStrict(content.links.map { it.toEntity() })
        }
        attachmentRefCommandDao().deleteCommittedByEntryId(entryId.value)
        if (revisionAttachments.isNotEmpty()) {
            attachmentRefCommandDao().insertAllStrict(
                revisionAttachments.map { it.toCurrentRef(entryId.value) },
            )
        }

        val newVersion = expectedVersion.next()
        revisionWriter.snapshotChanges(
            db = this,
            entryId = entryId.value,
            entryVersion = newVersion.value,
            summary = content.summary,
            secret = content.secret,
            now = now,
            change = RevisionChange.VERSION_RESTORED,
        )
        activityWriter.recordActivity(this, entryId.value, ActivityType.RESTORE, now)

        RestoredRevision(
            version = newVersion,
            previousIconPath = current.iconCustomReference,
            restoredIconPath = content.summary.icon.customReference,
        )
    }

    private suspend fun replaceSecretRows(
        db: AppDatabase,
        entryId: String,
        bundle: EntrySecret,
        sensitiveFields: List<com.aozijx.passly.data.codec.revision.SensitiveFieldCipherSnapshot>,
        now: Long,
    ) = with(db) {
        secretFieldCommandDao().deleteAll(entryId)
        val rows = sensitiveFields.map { field ->
            EntrySecretFieldEntity(
                entryId = entryId,
                fieldKey = field.key.name,
                valueCipher = field.valueCipher,
                keyVersion = field.keyVersion,
                updatedAt = now,
            )
        } + EntrySecretFieldEntity(
            entryId = entryId,
            fieldKey = SecretBundleCodec.FIELD_KEY,
            valueCipher = bundleCodec.encrypt(bundle, entryId),
            keyVersion = 1,
            updatedAt = now,
        )
        secretFieldCommandDao().insertAllStrict(rows)
    }

    private suspend fun validateLinks(
        db: AppDatabase,
        entryId: EntryId,
        links: List<EntryLink>,
    ) {
        if (links.any { entryId != it.sourceEntryId && entryId != it.targetEntryId }) {
            throw ValidationError()
        }
        val endpointIds = links.flatMapTo(linkedSetOf()) {
            listOf(it.sourceEntryId.value, it.targetEntryId.value)
        }
        if (endpointIds.isEmpty()) return
        val endpoints = db.entryQueryDao().getByIds(endpointIds.toList())
        if (endpoints.size != endpointIds.size || endpoints.any { it.deletedAt != null }) {
            throw NotFound()
        }
    }

    private fun restoredCapabilities(
        bundle: EntrySecret,
        sensitiveKeys: Set<SensitiveFieldKey>,
        hasAttachments: Boolean,
    ): EntryCapabilities {
        val values = EntryCapabilities.from(bundle, hasAttachments).values.toMutableSet()
        if (
            SensitiveFieldKey.PASSWORD in sensitiveKeys ||
            SensitiveFieldKey.SSH_PASSPHRASE in sensitiveKeys
        ) {
            values += EntryCapability.PASSWORD
        }
        if (SensitiveFieldKey.OTP_SECRET in sensitiveKeys) values += EntryCapability.OTP
        return EntryCapabilities(values)
    }

    private fun EntryLink.toEntity() = EntryLinkEntity(
        linkId = id.value,
        sourceEntryId = sourceEntryId.value,
        targetEntryId = targetEntryId.value,
        relationType = relationType,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun RevisionAttachmentRefEntity.toCurrentRef(entryId: String) = AttachmentRefEntity(
        attachmentId = attachmentId,
        resourceId = resourceId,
        entryId = entryId,
        stagingOwnerId = null,
        fileName = fileName,
        mimeType = mimeType,
        displayOrder = displayOrder,
        status = AttachmentStatus.COMMITTED.name,
        createdAt = createdAt,
    )

}
