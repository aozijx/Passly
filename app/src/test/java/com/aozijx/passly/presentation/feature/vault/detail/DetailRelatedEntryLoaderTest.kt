package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailRelatedEntryLoaderTest {
    @Test
    fun accountLoadsExistingMembersAndIgnoresMissingEntries() = runTest {
        val account = entry("account", EntryType.ACCOUNT)
        val member = entry("member", EntryType.LOGIN)
        val links = listOf(
            link("one", "member", "account"),
            link("two", "missing", "account"),
        )
        val loader = DetailRelatedEntryLoader(
            entryQueryRepository = FakeEntryQueryRepository(
                mapOf(member.id to member),
            ),
            entryLinkRepository = FakeEntryLinkRepository(links),
        )

        assertEquals(listOf(member), loader.loadFor(account))
    }

    @Test
    fun entryWithoutAccountRelationshipHasNoRelatedEntries() = runTest {
        val entry = entry("standalone", EntryType.LOGIN)
        val loader = DetailRelatedEntryLoader(
            entryQueryRepository = FakeEntryQueryRepository(emptyMap()),
            entryLinkRepository = FakeEntryLinkRepository(emptyList()),
        )

        assertEquals(emptyList<Entry>(), loader.loadFor(entry))
    }

    private fun entry(id: String, type: EntryType) = Entry(
        identity = EntryIdentity(
            id = EntryId(id),
            type = type,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(id),
        secret = when (type) {
            EntryType.LOGIN -> EntrySecret(LoginCredential())
            else -> EntrySecret()
        },
    )

    private fun link(id: String, source: String, target: String) = EntryLink.create(
        id = EntryLinkId(id),
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
}
