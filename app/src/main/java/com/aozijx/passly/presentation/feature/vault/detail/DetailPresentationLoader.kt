package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.feature.vault.detail.DetailEntryData
import com.aozijx.passly.feature.vault.detail.DetailSessionQuery
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionKey
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionResolver
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal data class DetailEntryPresentation(
    val entry: Entry,
    val sections: List<DetailSectionKey>,
    val associatedApps: List<DetailInstalledApp>,
    val sensitiveFieldKeys: Set<SensitiveFieldKey>,
)

internal data class DetailSessionSnapshot(
    val presentation: DetailEntryPresentation,
    val relatedEntries: List<Entry>,
)

internal class DetailPresentationLoader @Inject constructor(
    private val sessionQuery: DetailSessionQuery,
    private val installedAppDirectory: InstalledAppDirectory,
) {
    suspend fun open(entryId: EntryId): DetailSessionSnapshot? {
        val session = sessionQuery.open(entryId) ?: return null
        return DetailSessionSnapshot(
            presentation = present(session.detail),
            relatedEntries = session.relatedEntries,
        )
    }

    suspend fun present(entry: Entry): DetailEntryPresentation =
        present(sessionQuery.snapshot(entry))

    suspend fun launchableApps(): List<DetailInstalledApp> =
        installedAppDirectory.launchableApps().map { metadata ->
            DetailInstalledApp(metadata.label, metadata.packageName)
        }

    private suspend fun present(detail: DetailEntryData): DetailEntryPresentation = coroutineScope {
        val apps = async { associatedApps(detail.entry) }
        DetailEntryPresentation(
            entry = detail.entry,
            sections = DetailSectionResolver.resolve(detail.entry),
            associatedApps = apps.await(),
            sensitiveFieldKeys = detail.sensitiveFieldKeys,
        )
    }

    private suspend fun associatedApps(entry: Entry): List<DetailInstalledApp> =
        entry.associations.applicationIds.sorted().map { packageName ->
            val metadata = installedAppDirectory.metadataFor(packageName)
            DetailInstalledApp(
                label = metadata?.label?.takeIf(String::isNotBlank) ?: packageName,
                packageName = packageName,
            )
        }
}
