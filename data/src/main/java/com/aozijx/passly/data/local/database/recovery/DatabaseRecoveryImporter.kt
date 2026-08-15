package com.aozijx.passly.data.local.database.recovery

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.codec.entry.EntryProfileCodec
import com.aozijx.passly.data.database.model.DatabaseRecoveryReport
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.AttachmentResourceState
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.entry.toDatabaseFlags
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

internal class DatabaseRecoveryImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val databaseSession: AppDatabaseSession,
    private val profileCodec: EntryProfileCodec,
    private val secretFieldStore: SecretFieldStore,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
) {
    suspend fun restore(plan: RecoveryPlan): DatabaseRecoveryReport {
        val journal = RecoveryFileJournal()
        return attachmentGarbageCollector.withMutationLock {
            try {
                prepareFiles(plan, journal)
                databaseSession.transaction { importPlan(plan) }.also { journal.commit() }
            } catch (error: Throwable) {
                journal.rollback()
                throw error
            }
        }
    }

    private fun prepareFiles(plan: RecoveryPlan, journal: RecoveryFileJournal) {
        plan.entries.forEach { entry ->
            entry.icon?.let { source ->
                val root = VaultResourcePaths.vaultImagesDir(context).also(File::mkdirs)
                val target = File(
                    root,
                    "recovered_${sha256("${plan.packageId}:${entry.entity.entryId}").take(32)}.bin",
                )
                journal.create(target, source)
                entry.restoredIconPath = target.absolutePath
            }
            entry.attachments.forEach { prepareResource(it.resource, journal) }
            entry.revisions.flatMap(RecoverableRevision::attachments)
                .forEach { prepareResource(it.resource, journal) }
        }
    }

    private fun prepareResource(resource: RecoverableResource, journal: RecoveryFileJournal) {
        val target = attachmentGarbageCollector.resourceFile(resource.entity.resourceId)
        if (!target.exists()) journal.create(target, resource.file)
    }

    private suspend fun AppDatabase.importPlan(plan: RecoveryPlan): DatabaseRecoveryReport {
        var restoredEntries = 0
        var restoredAttachments = 0
        var restoredRevisions = 0
        var restoredLinks = 0
        var skippedConflicts = plan.preexistingConflicts
        val importedIds = hashSetOf<String>()
        plan.entries.forEach { recovered ->
            val id = recovered.entity.entryId
            if (entryQueryDao().exists(id)) {
                skippedConflicts++
                return@forEach
            }
            val profile = recovered.profile.copy(
                icon = recovered.profile.icon.copy(customReference = recovered.restoredIconPath),
            )
            val capabilityFlags = EntryCapabilities.from(
                recovered.secret,
                hasAttachments = recovered.attachments.isNotEmpty(),
            ).toDatabaseFlags()
            entryCommandDao().insertStrict(
                recovered.entity.copy(
                    capabilityFlags = capabilityFlags,
                    otpType = recovered.secret.otp?.config?.type?.name,
                    searchIndexVersion = 0,
                    summaryBlob = profileCodec.encrypt(profile, id),
                ),
            )
            secretFieldStore.replaceAll(this, id, recovered.secret)
            recovered.attachments.forEach { attachment ->
                ensureResource(attachment.resource)
                if (attachmentRefQueryDao().getById(attachment.ref.attachmentId) == null) {
                    attachmentRefCommandDao().insertStrict(
                        attachment.ref.copy(status = "COMMITTED", stagingOwnerId = null),
                    )
                    restoredAttachments++
                }
            }
            recovered.revisions.forEach { revision ->
                if (entryRevisionQueryDao().getById(id, revision.entity.revisionId) == null) {
                    revision.attachments.forEach { ensureResource(it.resource) }
                    entryRevisionCommandDao().insertStrict(revision.entity)
                    val refs = revision.attachments.map(RecoverableRevisionAttachment::ref)
                    if (refs.isNotEmpty()) revisionAttachmentRefDao().insertAllStrict(refs)
                    restoredRevisions++
                }
            }
            importedIds += id
            restoredEntries++
        }
        plan.links.forEach { link ->
            if (link.sourceEntryId !in importedIds || link.targetEntryId !in importedIds) return@forEach
            if (entryLinkQueryDao().getById(link.linkId) != null) return@forEach
            entryLinkCommandDao().upsert(link)
            restoredLinks++
        }
        return DatabaseRecoveryReport(
            plan.packageId,
            restoredEntries,
            skippedConflicts,
            restoredAttachments,
            plan.entries.sumOf(RecoverableEntry::damagedResourceCount),
            restoredRevisions,
            restoredLinks,
            plan.issues.take(500),
        )
    }

    private suspend fun AppDatabase.ensureResource(resource: RecoverableResource) {
        if (attachmentResourceDao().getById(resource.entity.resourceId) == null) {
            attachmentResourceDao().insertStrict(
                resource.entity.copy(lifecycleState = AttachmentResourceState.ACTIVE),
            )
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}

internal class RecoveryFileJournal {
    private val created = mutableListOf<File>()

    fun create(target: File, source: File) {
        if (target.exists()) return
        target.parentFile?.let { check(it.exists() || it.mkdirs()) }
        val staging = File(target.parentFile, ".${target.name}.recovery.tmp")
        try {
            source.copyTo(staging, overwrite = false)
            check(staging.renameTo(target)) { "Unable to commit recovered resource file" }
            created += target
        } finally {
            staging.delete()
        }
    }

    fun commit() = created.clear()

    fun rollback() {
        created.asReversed().forEach(File::delete)
        created.clear()
    }
}
