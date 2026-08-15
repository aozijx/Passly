package com.aozijx.passly.data.local.database.recovery

import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.report
import com.aozijx.passly.data.codec.entry.EntryProfileCodec
import com.aozijx.passly.data.codec.entry.SecretFieldCodec
import com.aozijx.passly.data.codec.revision.EntryContentSnapshotCodec
import com.aozijx.passly.data.codec.revision.SensitiveRevisionSnapshotCodec
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryIssue
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryScan
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.AttachmentRefEntity
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.security.dek.AttachmentContentCrypto
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

internal class DatabaseRecoveryScanner @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val profileCodec: EntryProfileCodec,
    private val secretFieldStore: SecretFieldStore,
    private val revisionCodec: EntryContentSnapshotCodec,
    private val sensitiveRevisionCodec: SensitiveRevisionSnapshotCodec,
    private val sensitiveFieldCodec: SecretFieldCodec,
    private val attachmentCrypto: AttachmentContentCrypto,
    private val telemetry: TelemetryReporter,
) {
    suspend fun scan(
        verified: DatabaseRecoveryStore.VerifiedPackage,
        source: AppDatabase,
    ): DatabaseRecoveryScan {
        val issues = mutableListOf<DatabaseRecoveryIssue>()
        val counts = linkedMapOf<EntryType, Int>()
        var deleted = 0
        var conflicts = 0
        var damaged = 0
        var attachments = 0
        var damagedResources = 0
        readEntryIds(source, issues).forEach { entryId ->
            val entity = readEntry(source, verified.info.id, entryId, issues) ?: run {
                damaged++
                return@forEach
            }
            val candidate = recoverEntry(source, verified, entity, issues)
            if (candidate == null) {
                damaged++
                return@forEach
            }
            counts[entity.entryType] = counts.getOrDefault(entity.entryType, 0) + 1
            if (entity.deletedAt != null) deleted++
            if (databaseSession.query { entryQueryDao().exists(entity.entryId) }) conflicts++
            attachments += candidate.attachments.size
            damagedResources += candidate.damagedResourceCount
        }
        return DatabaseRecoveryScan(
            packageId = verified.info.id,
            recoverableByType = counts,
            deletedEntries = deleted,
            conflictingEntries = conflicts,
            damagedEntries = damaged,
            recoverableAttachments = attachments,
            damagedResources = damagedResources,
            issues = issues.take(MAX_REPORTED_ISSUES),
        )
    }

    suspend fun prepare(
        verified: DatabaseRecoveryStore.VerifiedPackage,
        source: AppDatabase,
        selectedTypes: Set<EntryType>,
    ): RecoveryPlan {
        val issues = mutableListOf<DatabaseRecoveryIssue>()
        val entries = mutableListOf<RecoverableEntry>()
        var conflicts = 0
        readEntryIds(source, issues).forEach { entryId ->
            val entity = readEntry(source, verified.info.id, entryId, issues) ?: return@forEach
            if (entity.entryType !in selectedTypes) return@forEach
            val recovered = recoverEntry(source, verified, entity, issues) ?: return@forEach
            if (databaseSession.query { entryQueryDao().exists(entity.entryId) }) conflicts++
            else entries += recovered
        }
        val acceptedIds = entries.mapTo(hashSetOf()) { it.entity.entryId }
        val links = runCatching { source.entryLinkQueryDao().getAll() }
            .getOrElse { error ->
                issues += issue("link", null, "LINK_TABLE_UNREADABLE")
                telemetry.report(
                    EventLevel.WARN,
                    EventCategory.DATABASE,
                    "database.recovery.links_unreadable",
                    error,
                )
                emptyList()
            }
            .filter { it.sourceEntryId in acceptedIds && it.targetEntryId in acceptedIds }
        return RecoveryPlan(verified.info.id, entries, links, issues, conflicts)
    }

    private suspend fun recoverEntry(
        source: AppDatabase,
        verified: DatabaseRecoveryStore.VerifiedPackage,
        entity: EntryEntity,
        issues: MutableList<DatabaseRecoveryIssue>,
    ): RecoverableEntry? {
        val anonymousId = anonymousId(verified.info.id, entity.entryId)
        val resourceIssuesBefore = issues.count(::isResourceIssue)
        val profile = runCatching { profileCodec.decrypt(entity.summaryBlob, entity.entryId) }
            .onFailure { issues += issue("entry", anonymousId, "PROFILE_DAMAGED") }
            .getOrNull()
        val secret = runCatching {
            secretFieldStore.readAll(source, entity.entryId)
        }.onFailure { issues += issue("entry", anonymousId, "SECRET_DAMAGED") }.getOrNull()
        if (profile == null && secret == null) return null

        val recoveredProfile = profile ?: EntryProfile("待检查的恢复条目 ${anonymousId.take(8)}")
        val recoveredSecret = secret ?: EntrySecret()
        val attachments = runCatching {
            source.attachmentRefQueryDao().getCommittedByEntryId(entity.entryId)
        }.getOrElse {
            issues += issue("attachment", anonymousId, "ATTACHMENT_REFS_UNREADABLE")
            emptyList()
        }.mapNotNull { validateAttachment(source, verified, it, anonymousId, issues) }
        val revisions = runCatching { source.entryRevisionQueryDao().getByEntryId(entity.entryId) }
            .getOrElse {
                issues += issue("revision", anonymousId, "REVISION_TABLE_UNREADABLE")
                emptyList()
            }.mapNotNull { validateRevision(source, verified, it, anonymousId, issues) }
        val icon = validateIcon(verified, recoveredProfile, anonymousId, issues)
        return RecoverableEntry(
            entity,
            recoveredProfile,
            recoveredSecret,
            icon,
            attachments,
            revisions,
            issues.count(::isResourceIssue) - resourceIssuesBefore,
        )
    }

    private suspend fun validateAttachment(
        source: AppDatabase,
        verified: DatabaseRecoveryStore.VerifiedPackage,
        ref: AttachmentRefEntity,
        anonymousId: String,
        issues: MutableList<DatabaseRecoveryIssue>,
    ): RecoverableAttachment? = validateResource(
        source, verified, ref.resourceId, anonymousId, issues,
    )?.let { RecoverableAttachment(ref, it) }

    private suspend fun validateResource(
        source: AppDatabase,
        verified: DatabaseRecoveryStore.VerifiedPackage,
        resourceId: String,
        anonymousId: String,
        issues: MutableList<DatabaseRecoveryIssue>,
    ): RecoverableResource? {
        val resource = source.attachmentResourceDao().getById(resourceId) ?: run {
            issues += issue("attachment", anonymousId, "RESOURCE_METADATA_MISSING")
            return null
        }
        val attachmentRoot = File(
            verified.resourcesDirectory,
            VaultResourcePaths.ATTACHMENTS,
        ).canonicalFile
        val file = File(attachmentRoot, "content/${resource.resourceId}.enc").canonicalFile
        if (!file.path.startsWith(attachmentRoot.path + File.separator) ||
            !file.isFile || file.length() !in 1..MAX_RESOURCE_BYTES
        ) {
            issues += issue("attachment", anonymousId, "RESOURCE_FILE_INVALID")
            return null
        }
        val valid = runCatching {
            val encrypted = file.readBytes()
            try {
                val content = attachmentCrypto.decrypt(encrypted, resource.resourceId)
                try {
                    attachmentCrypto.verifyContentId(content, resource.resourceId)
                } finally {
                    content.fill(0)
                }
            } finally {
                encrypted.fill(0)
            }
        }.getOrDefault(false)
        if (!valid) {
            issues += issue("attachment", anonymousId, "RESOURCE_AUTHENTICATION_FAILED")
            return null
        }
        return RecoverableResource(resource, file)
    }

    private suspend fun validateRevision(
        source: AppDatabase,
        verified: DatabaseRecoveryStore.VerifiedPackage,
        revision: EntryRevisionEntity,
        anonymousId: String,
        issues: MutableList<DatabaseRecoveryIssue>,
    ): RecoverableRevision? = runCatching {
        revisionCodec.decrypt(revision.entryContentCipher, revision.entryId)
        sensitiveRevisionCodec.decode(revision.sensitiveFieldCipherSet).forEach { field ->
            sensitiveFieldCodec.decryptProvisioned(
                revision.entryId,
                field.key,
                field.valueCipher,
            )
        }
        val attachments = source.revisionAttachmentRefDao().getByRevisionId(revision.revisionId)
            .mapNotNull { ref ->
                validateResource(source, verified, ref.resourceId, anonymousId, issues)
                    ?.let { RecoverableRevisionAttachment(ref, it) }
            }
        RecoverableRevision(revision, attachments)
    }.onFailure { issues += issue("revision", anonymousId, "REVISION_DAMAGED") }.getOrNull()

    private fun validateIcon(
        verified: DatabaseRecoveryStore.VerifiedPackage,
        profile: EntryProfile,
        anonymousId: String,
        issues: MutableList<DatabaseRecoveryIssue>,
    ): File? {
        val original = profile.icon.customReference ?: return null
        val root = File(verified.resourcesDirectory, VaultResourcePaths.VAULT_IMAGES).canonicalFile
        val file = File(root, File(original).name).canonicalFile
        if (file.parentFile != root || !file.isFile || file.length() !in 1..MAX_ICON_BYTES) {
            issues += issue("icon", anonymousId, "ICON_FILE_INVALID")
            return null
        }
        return file
    }

    private fun anonymousId(packageId: String, recordId: String): String =
        sha256("$packageId:$recordId").take(16)

    private fun readEntryIds(
        source: AppDatabase,
        issues: MutableList<DatabaseRecoveryIssue>,
    ): List<String> {
        val result = mutableListOf<String>()
        source.openHelper.readableDatabase.query("SELECT entryId FROM entries ORDER BY rowid")
            .use { cursor ->
                val index = cursor.getColumnIndexOrThrow("entryId")
                while (cursor.moveToNext()) {
                    if (result.size >= MAX_ENTRIES) {
                        issues += issue("entry", null, "ENTRY_LIMIT_EXCEEDED")
                        break
                    }
                    val entryId = cursor.getString(index)
                    if (entryId.isNullOrBlank() || entryId.length > MAX_ENTRY_ID_LENGTH) {
                        issues += issue("entry", null, "ENTRY_ID_INVALID")
                    } else {
                        result += entryId
                    }
                }
            }
        return result
    }

    private suspend fun readEntry(
        source: AppDatabase,
        packageId: String,
        entryId: String,
        issues: MutableList<DatabaseRecoveryIssue>,
    ): EntryEntity? = runCatching {
        requireNotNull(source.entryQueryDao().getById(entryId))
    }.onFailure {
        issues += issue("entry", anonymousId(packageId, entryId), "ENTRY_ROW_DAMAGED")
    }.getOrNull()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun issue(category: String, id: String?, reason: String) =
        DatabaseRecoveryIssue(category, id, reason)

    private fun isResourceIssue(issue: DatabaseRecoveryIssue): Boolean =
        issue.category == "attachment" || issue.category == "icon"

    private companion object {
        const val MAX_RESOURCE_BYTES = 128L * 1024L * 1024L
        const val MAX_ICON_BYTES = 16L * 1024L * 1024L
        const val MAX_REPORTED_ISSUES = 500
        const val MAX_ENTRIES = 100_000
        const val MAX_ENTRY_ID_LENGTH = 128
    }
}
