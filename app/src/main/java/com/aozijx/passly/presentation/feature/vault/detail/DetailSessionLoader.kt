package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.policy.EntryTypePolicy
import com.aozijx.passly.domain.entry.policy.EntryValidation
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionKey
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionResolver
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal data class DetailEntryAnalysis(
    val entryType: EntryType,
    val strategySummary: String,
    val validationError: String?,
    val strategyReady: Boolean,
    val sections: List<DetailSectionKey>,
)

internal data class DetailEntryPresentation(
    val entry: Entry,
    val analysis: DetailEntryAnalysis,
    val associatedApps: List<DetailInstalledApp>,
    val sensitiveFieldKeys: Set<SensitiveFieldKey>,
)

internal data class DetailSessionSnapshot(
    val presentation: DetailEntryPresentation,
    val relatedEntries: List<Entry>,
)

internal class DetailSessionLoader @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val sensitiveFieldRepository: SensitiveFieldRepository,
    private val installedAppDirectory: InstalledAppDirectory,
    private val entryLinkRepository: EntryLinkRepository,
    private val entryTypePolicy: EntryTypePolicy,
) {
    suspend fun open(entryId: EntryId): DetailSessionSnapshot? {
        val entry = entryQueryRepository.getById(entryId) ?: return null
        return coroutineScope {
            val presentation = async { present(entry) }
            val relatedEntries = async { relatedEntries(entry) }
            DetailSessionSnapshot(
                presentation = presentation.await(),
                relatedEntries = relatedEntries.await(),
            )
        }
    }

    suspend fun present(entry: Entry): DetailEntryPresentation = coroutineScope {
        val apps = async { associatedApps(entry) }
        val sensitiveFields = async { sensitiveFieldRepository.getPresence(entry.id).keys }
        DetailEntryPresentation(
            entry = entry,
            analysis = analyze(entry),
            associatedApps = apps.await(),
            sensitiveFieldKeys = sensitiveFields.await(),
        )
    }

    suspend fun launchableApps(): List<DetailInstalledApp> =
        installedAppDirectory.launchableApps().map { metadata ->
            DetailInstalledApp(metadata.label, metadata.packageName)
        }

    private fun analyze(entry: Entry): DetailEntryAnalysis = DetailEntryAnalysis(
        entryType = entry.type,
        strategySummary = entryTypePolicy.extractSummary(entry.type, entry),
        validationError = EntryValidation.validate(entry).firstOrNull()?.let {
            "${it.field.name}:${it.code.name}"
        },
        strategyReady = true,
        sections = DetailSectionResolver.resolve(entry),
    )

    private suspend fun associatedApps(entry: Entry): List<DetailInstalledApp> =
        entry.associations.applicationIds.sorted().map { packageName ->
            val metadata = installedAppDirectory.metadataFor(packageName)
            DetailInstalledApp(
                label = metadata?.label?.takeIf(String::isNotBlank) ?: packageName,
                packageName = packageName,
            )
        }

    private suspend fun relatedEntries(entry: Entry): List<Entry> {
        val relatedIds = DetailRelatedEntryIds.resolve(
            entryId = entry.id,
            entryType = entry.type,
            links = entryLinkRepository.getAll(),
        )
        return buildList {
            relatedIds.forEach { relatedId ->
                entryQueryRepository.getById(relatedId)?.let(::add)
            }
        }
    }
}
