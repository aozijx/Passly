package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.core.platform.packageinfo.InstalledAppMetadata
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.query.EntryDetailData
import com.aozijx.passly.domain.entry.model.query.EntryDetailSnapshot
import com.aozijx.passly.domain.entry.model.query.EntryRelationsSnapshot
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.relation.RelatedPasswordEntry
import com.aozijx.passly.domain.entry.port.EntryDetailQuery
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionResolver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailPresentationLoaderTest {
    @Test
    fun openBuildsOneCompleteDetailSession() =
        runTest {
            val account = entry("account", EntryType.ACCOUNT, setOf("com.unknown", "com.named"))
            val member = entry("member", EntryType.LOGIN)
            val query =
                FakeEntryDetailQuery(
                    details = mapOf(account.id to EntryDetailData(account, setOf(SensitiveFieldKey.PASSWORD))),
                    relations = EntryRelationsSnapshot(listOf(member), emptyList(), emptyList()),
                )
            val loader = loader(query)
            val session = requireNotNull(loader.open(account.id))

            assertEquals(account, session.presentation.entry)
            assertEquals(DetailSectionResolver.resolve(account), session.presentation.sections)
            assertEquals(
                listOf(
                    DetailInstalledApp("Named", "com.named"),
                    DetailInstalledApp("com.unknown", "com.unknown"),
                ),
                session.presentation.associatedApps,
            )
            assertEquals(setOf(SensitiveFieldKey.PASSWORD), session.presentation.sensitiveFieldKeys)
            assertEquals(listOf(member), session.relations.accountGroupEntries)
            assertEquals(emptyList<Entry>(), session.relations.otherRelatedEntries)
            assertEquals(emptyList<RelatedPasswordEntry>(), session.relations.relatedPasswords)
            assertEquals(listOf(account.id), query.openRequests)
        }

    @Test
    fun missingEntryStopsBeforeLoadingDependentSessionData() =
        runTest {
            val query = FakeEntryDetailQuery()
            val loader = loader(query)

            assertNull(loader.open(EntryId("missing")))
            assertEquals(listOf(EntryId("missing")), query.openRequests)
            assertEquals(emptyList<EntryId>(), query.snapshotRequests)
        }

    @Test
    fun presentRebuildsAnalysisAndAssociatedAppsAfterAnEdit() =
        runTest {
            val loader = loader()
            val updated = entry("entry", EntryType.LOGIN, setOf("com.named"))

            val presentation = loader.present(updated)

            assertEquals(updated, presentation.entry)
            assertEquals(DetailSectionResolver.resolve(updated), presentation.sections)
            assertEquals(
                listOf(DetailInstalledApp("Named", "com.named")),
                presentation.associatedApps,
            )
        }

    @Test
    fun launchableAppsExposePresentationModelsOnly() =
        runTest {
            assertEquals(
                listOf(
                    DetailInstalledApp("First", "com.first"),
                    DetailInstalledApp("Second", "com.second"),
                ),
                loader().launchableApps(),
            )
        }

    private fun loader(query: EntryDetailQuery = FakeEntryDetailQuery()) = DetailPresentationLoader(
        sessionQuery = query,
        installedAppDirectory = FakeInstalledAppDirectory(),
    )

    private fun entry(
        id: String,
        type: EntryType,
        applicationIds: Set<String> = emptySet(),
    ) = Entry(
        identity =
            EntryIdentity(
                id = EntryId(id),
                type = type,
                version = EntryVersion.INITIAL,
                timestamps = EntryTimestamps(1L),
            ),
        profile =
            EntryProfile(
                title = id,
                associations = EntryAssociations(applicationIds = applicationIds),
            ),
        secret =
            when (type) {
                EntryType.LOGIN -> EntrySecret(LoginCredential())
                else -> EntrySecret()
            },
    )

    private class FakeEntryDetailQuery(
        private val details: Map<EntryId, EntryDetailData> = emptyMap(),
        private val relations: EntryRelationsSnapshot = EntryRelationsSnapshot(emptyList(), emptyList(), emptyList()),
    ) : EntryDetailQuery {
        val openRequests = mutableListOf<EntryId>()
        val snapshotRequests = mutableListOf<EntryId>()

        override suspend fun open(entryId: EntryId): EntryDetailSnapshot? {
            openRequests += entryId
            return details[entryId]?.let { EntryDetailSnapshot(it, relations) }
        }

        override suspend fun snapshot(entry: Entry): EntryDetailData {
            snapshotRequests += entry.id
            return details[entry.id] ?: EntryDetailData(entry, emptySet())
        }

        override suspend fun reloadRelations(entryId: EntryId): EntryRelationsSnapshot? = relations
    }

    private class FakeInstalledAppDirectory : InstalledAppDirectory {
        override suspend fun metadataFor(packageName: String): InstalledAppMetadata? =
            if (packageName == "com.named") InstalledAppMetadata("Named", packageName) else null

        override suspend fun launchableApps() =
            listOf(
                InstalledAppMetadata("First", "com.first"),
                InstalledAppMetadata("Second", "com.second"),
            )
    }

}
