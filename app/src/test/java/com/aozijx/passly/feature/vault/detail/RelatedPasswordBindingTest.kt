package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatedPasswordBindingTest {
    @Test
    fun candidatesExcludeCurrentAndAlreadyRelatedSavedLoginEntries() =
        runTest {
            val current = password("current")
            val linked = password("linked")
            val available = password("available")
            val repository = TestEntries(listOf(current, linked, available))
            val links = TestLinks(listOf(relatedLink("current", "linked")))
            val binding = RelatedPasswordBinding(repository, links, testLogger())

            val candidates = binding.candidates(EntryId("current"))

            assertEquals(listOf("available"), candidates.map { it.id.value })
        }

    @Test
    fun bindCreatesOneCanonicalRelatedToLinkForTwoSavedPasswords() =
        runTest {
            val repository = TestEntries(listOf(password("z-current"), password("a-target")))
            val links = TestLinks()
            val binding = RelatedPasswordBinding(repository, links, testLogger())

            val result = binding.bind(EntryId("z-current"), EntryId("a-target"))

            assertTrue(result is AppResult.Success<*>)
            val saved = links.upserts.single()
            assertEquals(EntryId("a-target"), saved.sourceEntryId)
            assertEquals(EntryId("z-current"), saved.targetEntryId)
            assertEquals(EntryRelationType.RELATED_TO, saved.relationType)
        }

    @Test
    fun bindRejectsAnUnpersistedEndpointWithoutWriting() =
        runTest {
            val repository = TestEntries(listOf(password("current")))
            val links = TestLinks()
            val binding = RelatedPasswordBinding(repository, links, testLogger())

            val result = binding.bind(EntryId("current"), EntryId("draft"))

            assertTrue(result is AppResult.Failure)
            assertTrue(links.upserts.isEmpty())
        }

    @Test
    fun bindingAnExistingSymmetricRelationIsIdempotent() =
        runTest {
            val repository = TestEntries(listOf(password("current"), password("target")))
            val links = TestLinks(listOf(relatedLink("target", "current")))
            val binding = RelatedPasswordBinding(repository, links, testLogger())

            val result = binding.bind(EntryId("current"), EntryId("target"))

            assertTrue(result is AppResult.Success<*>)
            assertTrue(links.upserts.isEmpty())
        }

    private class TestEntries(
        private val items: List<EntryListItem>,
    ) : EntryListQueryRepository {
        override val availableCategories: Flow<List<String>> = emptyFlow()
        override val deletedEntries: Flow<List<EntryListItem>> = emptyFlow()

        override suspend fun getActiveByType(entryType: EntryType): List<EntryListItem> =
            items.filter { it.entryType == entryType && it.deletedAt == null }
    }

    private class TestLinks(
        initial: List<EntryLink> = emptyList(),
    ) : EntryLinkRepository {
        private val links = initial.toMutableList()
        val upserts = mutableListOf<EntryLink>()

        override fun observeAll(): Flow<List<EntryLink>> = emptyFlow()

        override fun observeLinks(entryId: EntryId): Flow<List<EntryLink>> = emptyFlow()

        override suspend fun getAll(): List<EntryLink> = links

        override suspend fun getLinks(entryId: EntryId): List<EntryLink> =
            links.filter { it.sourceEntryId == entryId || it.targetEntryId == entryId }

        override suspend fun upsert(link: EntryLink): AppResult<Unit> {
            upserts += link
            links += link
            return AppResult.Success(Unit)
        }

        override suspend fun delete(linkId: EntryLinkId): AppResult<Unit> = AppResult.Success(Unit)
    }

    private companion object {
        fun password(id: String) =
            EntryListItem(
                identity =
                    EntryIdentity(
                        id = EntryId(id),
                        type = EntryType.LOGIN,
                        timestamps = EntryTimestamps(createdAtMs = 1L),
                    ),
                profile = EntryProfile(title = id),
            )

        fun relatedLink(
            first: String,
            second: String,
        ) =
            EntryLink.create(
                id = EntryLinkId("$first-$second"),
                sourceEntryId = EntryId(first),
                targetEntryId = EntryId(second),
                relationType = EntryRelationType.RELATED_TO,
                createdAt = 1L,
            )
    }
}
