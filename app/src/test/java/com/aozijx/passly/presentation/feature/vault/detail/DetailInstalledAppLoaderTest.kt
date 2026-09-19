package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.core.platform.packageinfo.InstalledAppMetadata
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailInstalledAppLoaderTest {
    private val directory = FakeInstalledAppDirectory(
        metadata = mapOf(
            "com.named" to InstalledAppMetadata("Named", "com.named"),
        ),
        launchable = listOf(
            InstalledAppMetadata("First", "com.first"),
            InstalledAppMetadata("Second", "com.second"),
        ),
    )
    private val loader = DetailInstalledAppLoader(directory)

    @Test
    fun associatedAppsAreSortedAndMissingLabelsFallBackToPackageName() = runTest {
        val result = loader.associatedWith(
            entry(
                applicationIds = setOf("com.unknown", "com.named"),
            ),
        )

        assertEquals(
            listOf(
                DetailInstalledApp("Named", "com.named"),
                DetailInstalledApp("com.unknown", "com.unknown"),
            ),
            result,
        )
    }

    @Test
    fun launchableAppsAreMappedWithoutPlatformTypes() = runTest {
        assertEquals(
            listOf(
                DetailInstalledApp("First", "com.first"),
                DetailInstalledApp("Second", "com.second"),
            ),
            loader.launchable(),
        )
    }

    private fun entry(applicationIds: Set<String>) = Entry(
        identity = EntryIdentity(
            id = EntryId("entry"),
            type = EntryType.ACCOUNT,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(
            title = "Entry",
            associations = EntryAssociations(applicationIds = applicationIds),
        ),
    )

    private class FakeInstalledAppDirectory(
        private val metadata: Map<String, InstalledAppMetadata>,
        private val launchable: List<InstalledAppMetadata>,
    ) : InstalledAppDirectory {
        override suspend fun metadataFor(packageName: String): InstalledAppMetadata? =
            metadata[packageName]

        override suspend fun launchableApps(): List<InstalledAppMetadata> = launchable
    }
}
