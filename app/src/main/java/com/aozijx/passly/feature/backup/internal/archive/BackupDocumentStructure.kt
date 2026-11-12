package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument

internal object BackupDocumentStructure {
    fun validate(document: BackupDocument) {
        if (document.format != BackupDocument.FORMAT) throw BackupIntegrityException(Reasons.Backup.Document.BackupDocumentFormatUnsupported)
        if (document.version != BackupDocument.CURRENT_VERSION) throw BackupIntegrityException(Reasons.Backup.Document.BackupDocumentVersionUnsupported)
        require(document.exportedAt >= 0) { "备份导出时间无效" }
        require(document.entries.size <= BackupBundleValidator.MAX_ENTRIES) { "备份条目数量过多" }
        require(document.resources.size <= BackupBundleValidator.MAX_RESOURCES) { "备份资源数量过多" }
        if (document.entries.map { it.id }.distinct().size != document.entries.size) {
            throw BackupIntegrityException(Reasons.Backup.Document.BackupDocumentDuplicateEntryId)
        }
        if (document.resources.map { it.id }.distinct().size != document.resources.size) {
            throw BackupIntegrityException(Reasons.Backup.Document.BackupDocumentDuplicateResourceId)
        }
        if (document.links.map { it.id }.distinct().size != document.links.size) {
            throw BackupIntegrityException(Reasons.Backup.Document.BackupDocumentDuplicateLinkId)
        }
        require(
            document.links
                .map {
                    val endpoints = listOf(it.sourceEntryId, it.targetEntryId).sorted()
                    endpoints[0] to endpoints[1]
                }
                .distinct()
                .size == document.links.size,
        ) { "备份包含重复关系" }
        require(document.entries.all { BackupBundleValidator.isSafeArchiveId(it.id) }) { "备份包含不安全的条目 ID" }
        require(document.resources.all { BackupBundleValidator.isSafeArchiveId(it.id) }) { "备份包含不安全的资源 ID" }
        require(document.links.all { BackupBundleValidator.isSafeArchiveId(it.id) }) { "备份包含不安全的关系 ID" }
    }
}
