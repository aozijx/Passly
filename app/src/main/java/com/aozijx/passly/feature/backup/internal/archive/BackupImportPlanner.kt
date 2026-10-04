package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.domain.entry.policy.EntryLinkPolicy
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.model.BackupCompatibilitySummary

internal data class BackupImportPlan(
    val bundle: BackupBundle,
    val summary: BackupCompatibilitySummary,
)

internal object BackupImportPlanner {
    private val safeId = Regex("[A-Za-z0-9_-]{1,160}")
    private val archiveOnlyFields = setOf("icon_name", "icon_color")

    fun plan(bundle: BackupBundle, strategy: BackupImportStrategy): BackupImportPlan {
        if (strategy == BackupImportStrategy.STRICT) {
            BackupBundleValidator.validate(bundle, requireResourceData = bundle.document.resources.isNotEmpty())
            return BackupImportPlan(bundle, bundle.compatibilitySummary)
        }
        validateStructuralHeader(bundle.document)
        KeyedBackupBundleValidator.validateResourceIntegrity(
            bundle = bundle,
            entryIds = bundle.document.entries.mapTo(linkedSetOf()) { it.id },
            requireResourceData = bundle.document.resources.isNotEmpty(),
        )
        var ignoredFieldCount = 0
        var skippedEntryCount = 0
        val acceptedEntries = bundle.document.entries.mapNotNull { entry ->
            if (BackupArchiveKeyRegistry.entryType(entry.type) == null) {
                skippedEntryCount++
                return@mapNotNull null
            }
            val knownFields = entry.fields.filter { field ->
                val known = BackupArchiveKeyRegistry.field(field.key) != null || field.key in archiveOnlyFields
                if (!known) ignoredFieldCount++
                known
            }
            val candidate = entry.copy(fields = knownFields, attachmentIds = emptyList())
            val valid = runCatching {
                BackupBundleValidator.validate(
                    BackupBundle(
                        document = bundle.document.copy(
                            entries = listOf(candidate),
                            links = emptyList(),
                            resources = emptyList(),
                        ),
                    ),
                    requireResourceData = false,
                )
            }.isSuccess
            if (!valid) {
                skippedEntryCount++
                null
            } else {
                entry.copy(fields = knownFields)
            }
        }
        val acceptedIds = acceptedEntries.mapTo(hashSetOf()) { it.id }
        val acceptedTypes = acceptedEntries.associate { entry ->
            entry.id to requireNotNull(BackupArchiveKeyRegistry.entryType(entry.type))
        }
        val acceptedResources = bundle.document.resources.filter { it.entryId in acceptedIds }
        val acceptedResourceIds = acceptedResources.mapTo(hashSetOf()) { it.id }
        val acceptedResourceData = linkedMapOf<String, ByteArray>()
        bundle.resourceData.forEach { (id, content) ->
            if (id in acceptedResourceIds) acceptedResourceData[id] = content else content.fill(0)
        }
        val attachmentIdsByEntry = acceptedResources
            .filter { it.kind == BackupResourceKind.ATTACHMENT }
            .groupBy({ it.entryId }, { it.id })
        val normalizedEntries = acceptedEntries.map { entry ->
            entry.copy(attachmentIds = attachmentIdsByEntry[entry.id].orEmpty())
        }
        val acceptedLinks = bundle.document.links.filter { link ->
            val relation = BackupArchiveKeyRegistry.relationType(link.relationType) ?: return@filter false
            val source = acceptedTypes[link.sourceEntryId] ?: return@filter false
            val target = acceptedTypes[link.targetEntryId] ?: return@filter false
            EntryLinkPolicy.isAllowed(relation, source, target)
        }
        val plannedBundle = BackupBundle(
            document = bundle.document.copy(
                entries = normalizedEntries,
                links = acceptedLinks,
                resources = acceptedResources,
            ),
            resourceData = acceptedResourceData,
            sourceEntryCount = bundle.sourceEntryCount,
            compatibilitySummary = BackupCompatibilitySummary(
                skippedEntryCount = skippedEntryCount,
                ignoredFieldCount = ignoredFieldCount,
                prunedLinkCount = bundle.document.links.size - acceptedLinks.size,
                prunedResourceCount = bundle.document.resources.size - acceptedResources.size,
            ),
        )
        BackupBundleValidator.validate(
            plannedBundle,
            requireResourceData = acceptedResources.isNotEmpty(),
        )
        return BackupImportPlan(
            bundle = plannedBundle,
            summary = plannedBundle.compatibilitySummary,
        )
    }

    private fun validateStructuralHeader(document: BackupDocument) {
        require(document.format == BackupDocument.FORMAT) { "不支持的备份文档格式: ${document.format}" }
        require(document.version == BackupDocument.CURRENT_VERSION) { "不支持的备份文档版本: ${document.version}" }
        require(document.exportedAt >= 0) { "备份导出时间无效" }
        require(document.entries.size <= BackupBundleValidator.MAX_ENTRIES) { "备份条目数量过多" }
        require(document.resources.size <= BackupBundleValidator.MAX_RESOURCES) { "备份资源数量过多" }
        require(document.entries.map { it.id }.distinct().size == document.entries.size) { "备份包含重复条目 ID" }
        require(document.resources.map { it.id }.distinct().size == document.resources.size) { "备份包含重复资源 ID" }
        require(document.links.map { it.id }.distinct().size == document.links.size) { "备份包含重复关系 ID" }
        require(
            document.links.map { Triple(it.sourceEntryId, it.targetEntryId, it.relationType) }
                .distinct().size == document.links.size
        ) { "备份包含重复关系" }
        require(document.entries.all { safeId.matches(it.id) }) { "备份包含不安全的条目 ID" }
        require(document.resources.all { safeId.matches(it.id) }) { "备份包含不安全的资源 ID" }
        require(document.links.all { safeId.matches(it.id) }) { "备份包含不安全的关系 ID" }
    }
}
