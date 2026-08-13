package com.aozijx.passly.data.local.database.recovery

import com.aozijx.passly.data.database.model.DatabaseRecoveryIssue
import com.aozijx.passly.data.local.database.entity.AttachmentRefEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.data.local.database.entity.RevisionAttachmentRefEntity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import java.io.File

internal data class RecoveryPlan(
    val packageId: String,
    val entries: List<RecoverableEntry>,
    val links: List<EntryLinkEntity>,
    val issues: List<DatabaseRecoveryIssue>,
    val preexistingConflicts: Int,
)

internal data class RecoverableEntry(
    val entity: EntryEntity,
    val profile: EntryProfile,
    val secret: EntrySecret,
    val icon: File?,
    val attachments: List<RecoverableAttachment>,
    val revisions: List<RecoverableRevision>,
    val damagedResourceCount: Int,
    var restoredIconPath: String? = null,
)

internal data class RecoverableAttachment(
    val ref: AttachmentRefEntity,
    val resource: RecoverableResource,
)

internal data class RecoverableResource(
    val entity: AttachmentResourceEntity,
    val file: File,
)

internal data class RecoverableRevision(
    val entity: EntryRevisionEntity,
    val attachments: List<RecoverableRevisionAttachment>,
)

internal data class RecoverableRevisionAttachment(
    val ref: RevisionAttachmentRefEntity,
    val resource: RecoverableResource,
)
