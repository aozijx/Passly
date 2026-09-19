package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.domain.entry.model.Entry
import javax.inject.Inject

internal class DetailInstalledAppLoader @Inject constructor(
    private val installedAppDirectory: InstalledAppDirectory,
) {
    suspend fun associatedWith(entry: Entry): List<DetailInstalledApp> =
        entry.associations.applicationIds
            .sorted()
            .map { packageName ->
                val metadata = installedAppDirectory.metadataFor(packageName)
                DetailInstalledApp(
                    label = metadata?.label?.takeIf(String::isNotBlank) ?: packageName,
                    packageName = packageName,
                )
            }

    suspend fun launchable(): List<DetailInstalledApp> =
        installedAppDirectory.launchableApps().map { metadata ->
            DetailInstalledApp(
                label = metadata.label,
                packageName = metadata.packageName,
            )
        }
}
