package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.domain.entry.policy.EntryLinkPolicy
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.feature.backup.internal.model.BackupCompatibilitySummary

internal data class BackupImportPlan(
    val bundle: BackupBundle,
    val summary: BackupCompatibilitySummary,
)

internal object BackupImportPlanner {
    private val archiveOnlyFields = setOf("icon_name", "icon_color")

    internal fun isCandidateValid(validate: () -> Unit): Boolean =
        try {
            validate()
            true
        } catch (_: IllegalArgumentException) {
            false
        }

    fun plan(
        bundle: BackupBundle,
        strategy: BackupImportStrategy,
    ): BackupImportPlan {
        if (strategy == BackupImportStrategy.STRICT) {
            BackupBundleValidator.validate(bundle, requireResourceData = bundle.document.resources.isNotEmpty())
            return BackupImportPlan(bundle, bundle.compatibilitySummary)
        }
        BackupDocumentStructure.validate(bundle.document)
        KeyedBackupBundleValidator.validateResourceIntegrity(
            bundle = bundle,
            entryIds = bundle.document.entries.mapTo(linkedSetOf()) { it.id },
            requireResourceData = bundle.document.resources.isNotEmpty(),
        )
        var ignoredFieldCount = 0
        var skippedEntryCount = 0
        val acceptedEntries =
            bundle.document.entries.mapNotNull { entry ->
                if (BackupArchiveKeyRegistry.entryType(entry.type) == null) {
                    skippedEntryCount++
                    return@mapNotNull null
                }
                val knownFields =
                    entry.fields.filter { field ->
                        val known = BackupArchiveKeyRegistry.field(field.key) != null || field.key in archiveOnlyFields
                        if (!known) ignoredFieldCount++
                        known
                    }
                val candidate = entry.copy(fields = knownFields, attachmentIds = emptyList())
                val valid =
                    isCandidateValid {
                        BackupBundleValidator.validate(
                            BackupBundle(
                                document =
                                    bundle.document.copy(
                                        entries = listOf(candidate),
                                        links = emptyList(),
                                        resources = emptyList(),
                                    ),
                            ),
                            requireResourceData = false,
                        )
                    }
                if (!valid) {
                    skippedEntryCount++
                    null
                } else {
                    entry.copy(fields = knownFields)
                }
            }
        val acceptedIds = acceptedEntries.mapTo(hashSetOf()) { it.id }
        val acceptedTypes =
            acceptedEntries.associate { entry ->
                entry.id to requireNotNull(BackupArchiveKeyRegistry.entryType(entry.type))
            }
        val acceptedResources = bundle.document.resources.filter { it.entryId in acceptedIds }
        val acceptedResourceIds = acceptedResources.mapTo(hashSetOf()) { it.id }
        val acceptedResourceData = linkedMapOf<String, ByteArray>()
        bundle.resourceData.forEach { (id, content) ->
            if (id in acceptedResourceIds) acceptedResourceData[id] = content else content.fill(0)
        }
        val attachmentIdsByEntry =
            acceptedResources
                .filter { it.kind == BackupResourceKind.ATTACHMENT }
                .groupBy({ it.entryId }, { it.id })
        val normalizedEntries =
            acceptedEntries.map { entry ->
                entry.copy(attachmentIds = attachmentIdsByEntry[entry.id].orEmpty())
            }
        val acceptedLinks =
            bundle.document.links.filter { link ->
                val relation = BackupArchiveKeyRegistry.relationType(link.relationType) ?: return@filter false
                val source = acceptedTypes[link.sourceEntryId] ?: return@filter false
                val target = acceptedTypes[link.targetEntryId] ?: return@filter false
                EntryLinkPolicy.isAllowed(relation, source, target)
            }
        val plannedBundle =
            BackupBundle(
                document =
                    bundle.document.copy(
                        entries = normalizedEntries,
                        links = acceptedLinks,
                        resources = acceptedResources,
                    ),
                resourceData = acceptedResourceData,
                sourceEntryCount = bundle.sourceEntryCount,
                compatibilitySummary =
                    BackupCompatibilitySummary(
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

}
