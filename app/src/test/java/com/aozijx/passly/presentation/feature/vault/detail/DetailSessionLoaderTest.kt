package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.platform.packageinfo.InstalledAppDirectory
import com.aozijx.passly.core.platform.packageinfo.InstalledAppMetadata
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.policy.EntryTypePolicy
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailSessionLoaderTest {
    @Test
    fun openBuildsOneCompleteDetailSession() = runTest {
        val account = entry("account", EntryType.ACCOUNT, setOf("com.unknown", "com.named"))
        val member = entry("member", EntryType.LOGIN)
        val sensitiveFields = FakeSensitiveFieldRepository(
            SensitiveFieldPresence(account.id, setOf(SensitiveFieldKey.PASSWORD)),
        )
        val loader = loader(
            entries = mapOf(account.id to account, member.id to member),
            sensitiveFields = sensitiveFields,
            links = listOf(link("member", "account")),
        )

        val session = requireNotNull(loader.open(account.id))

        assertEquals(account, session.presentation.entry)
        assertEquals("summary:ACCOUNT", session.presentation.analysis.strategySummary)
        assertEquals(
            listOf(
                DetailInstalledApp("Named", "com.named"),
                DetailInstalledApp("com.unknown", "com.unknown"),
            ),
            session.presentation.associatedApps,
        )
        assertEquals(setOf(SensitiveFieldKey.PASSWORD), session.presentation.sensitiveFieldKeys)
        assertEquals(listOf(member), session.relatedEntries)
        assertEquals(listOf(account.id), sensitiveFields.presenceRequests)
    }

    @Test
    fun missingEntryStopsBeforeLoadingDependentSessionData() = runTest {
        val sensitiveFields = FakeSensitiveFieldRepository(
            SensitiveFieldPresence(EntryId("unused"), emptySet()),
        )
        val loader = loader(entries = emptyMap(), sensitiveFields = sensitiveFields)

        assertNull(loader.open(EntryId("missing")))
        assertEquals(emptyList<EntryId>(), sensitiveFields.presenceRequests)
    }

    @Test
    fun presentRebuildsAnalysisAndAssociatedAppsAfterAnEdit() = runTest {
        val loader = loader(entries = emptyMap())
        val updated = entry("entry", EntryType.LOGIN, setOf("com.named"))

        val presentation = loader.present(updated)

        assertEquals(updated, presentation.entry)
        assertEquals("summary:LOGIN", presentation.analysis.strategySummary)
        assertEquals(
            listOf(DetailInstalledApp("Named", "com.named")),
            presentation.associatedApps,
        )
    }

    @Test
    fun launchableAppsExposePresentationModelsOnly() = runTest {
        assertEquals(
            listOf(
                DetailInstalledApp("First", "com.first"),
                DetailInstalledApp("Second", "com.second"),
            ),
            loader(entries = emptyMap()).launchableApps(),
        )
    }

    private fun loader(
        entries: Map<EntryId, Entry>,
        sensitiveFields: FakeSensitiveFieldRepository = FakeSensitiveFieldRepository(
            SensitiveFieldPresence(EntryId("unused"), emptySet()),
        ),
        links: List<EntryLink> = emptyList(),
    ) = DetailSessionLoader(
        entryQueryRepository = FakeEntryQueryRepository(entries),
        sensitiveFieldRepository = sensitiveFields,
        installedAppDirectory = FakeInstalledAppDirectory(),
        entryLinkRepository = FakeEntryLinkRepository(links),
        entryTypePolicy = FakeEntryTypePolicy(),
    )

    private fun entry(
        id: String,
        type: EntryType,
        applicationIds: Set<String> = emptySet(),
    ) = Entry(
        identity = EntryIdentity(
            id = EntryId(id),
            type = type,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(
            title = id,
            associations = EntryAssociations(applicationIds = applicationIds),
        ),
        secret = when (type) {
            EntryType.LOGIN -> EntrySecret(LoginCredential())
            else -> EntrySecret()
        },
    )

    private fun link(source: String, target: String) = EntryLink.create(
        id = EntryLinkId("link"),
        sourceEntryId = EntryId(source),
        targetEntryId = EntryId(target),
        relationType = EntryRelationType.MEMBER_OF_ACCOUNT,
        createdAt = 1L,
    )

    private class FakeEntryQueryRepository(
        private val entries: Map<EntryId, Entry>,
    ) : EntryQueryRepository {
        override suspend fun getById(entryId: EntryId): Entry? = entries[entryId]
        override suspend fun findEntriesWithCustomIcons() = emptyList<Entry>()
        override suspend fun count() = entries.size
    }

    private class FakeSensitiveFieldRepository(
        private val presence: SensitiveFieldPresence,
    ) : SensitiveFieldRepository {
        val presenceRequests = mutableListOf<EntryId>()

        override suspend fun getPresence(entryId: EntryId): SensitiveFieldPresence {
            presenceRequests += entryId
            return presence
        }

        override suspend fun reveal(
            entryId: EntryId,
            key: SensitiveFieldKey,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): RevealedSensitiveField? = error("Not used")

        override suspend fun revealMany(
            entryId: EntryId,
            keys: Set<SensitiveFieldKey>,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): List<RevealedSensitiveField> = error("Not used")

        override suspend fun readBundle(entryId: EntryId): EntrySecret = error("Not used")
        override suspend fun readAll(entryId: EntryId): EntrySecret = error("Not used")
    }

    private class FakeInstalledAppDirectory : InstalledAppDirectory {
        override suspend fun metadataFor(packageName: String): InstalledAppMetadata? =
            if (packageName == "com.named") InstalledAppMetadata("Named", packageName) else null

        override suspend fun launchableApps() = listOf(
            InstalledAppMetadata("First", "com.first"),
            InstalledAppMetadata("Second", "com.second"),
        )
    }

    private class FakeEntryLinkRepository(
        private val links: List<EntryLink>,
    ) : EntryLinkRepository {
        override fun observeAll(): Flow<List<EntryLink>> = flowOf(links)
        override fun observeLinks(entryId: EntryId): Flow<List<EntryLink>> = flowOf(links)
        override suspend fun getAll(): List<EntryLink> = links
        override suspend fun getLinks(entryId: EntryId): List<EntryLink> = links
        override suspend fun upsert(link: EntryLink) = AppResult.Success(Unit)
        override suspend fun delete(linkId: EntryLinkId) = AppResult.Success(Unit)
    }

    private class FakeEntryTypePolicy : EntryTypePolicy {
        override fun supportsAutofill(type: EntryType) = false
        override fun suggestedCategory(type: EntryType) = ""
        override fun sensitiveFields(type: EntryType) = emptySet<FieldKey>()
        override fun extractSummary(type: EntryType, entry: Entry) = "summary:${type.name}"
    }
}
