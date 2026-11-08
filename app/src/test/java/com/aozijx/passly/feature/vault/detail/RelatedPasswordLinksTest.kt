package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.Reasons
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
import com.aozijx.passly.testing.RecordingTestLogger
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatedPasswordLinksTest {
    @Test
    fun unlinkDeletesSelectedSymmetricRelatedToLink() =
        runTest {
            val repository = TestEntries(listOf(password("z-source"), password("a-target")))
            val selected = relatedLink("z-source", "a-target")
            val untouched = relatedLink("a-target", "other")
            val links = TestLinks(listOf(selected, untouched))
            val binding = RelatedPasswordLinks(repository, links, testLogger())

            val result = binding.unlink(EntryId("z-source"), EntryId("a-target"), selected.id)

            assertTrue(result is AppResult.Success<*>)
            assertEquals(listOf(selected.id), links.deletes)
            assertEquals(listOf(untouched), links.currentLinks)
        }

    @Test
    fun unlinkIsIdempotentWhenLinkIsAbsent() =
        runTest {
            val repository = TestEntries(listOf(password("source"), password("target")))
            val links = TestLinks()
            val binding = RelatedPasswordLinks(repository, links, testLogger())

            val result = binding.unlink(EntryId("source"), EntryId("target"), EntryLinkId("missing"))

            assertTrue(result is AppResult.Success<*>)
            assertTrue(links.deletes.isEmpty())
        }

    @Test
    fun unlinkRejectsMismatchedLinkIdOrEndpoints() =
        runTest {
            val repository = TestEntries(listOf(password("source"), password("target"), password("other")))
            val correctEdge = relatedLink("source", "target")
            val mismatchedEndpoint = relatedLink("source", "other")
            val links = TestLinks(listOf(correctEdge, mismatchedEndpoint))
            val binding = RelatedPasswordLinks(repository, links, testLogger())

            val wrongId = binding.unlink(EntryId("source"), EntryId("target"), EntryLinkId("unknown"))
            val wrongEndpoint = binding.unlink(EntryId("source"), EntryId("target"), mismatchedEndpoint.id)

            assertTrue(wrongId is AppResult.Failure)
            assertTrue(wrongEndpoint is AppResult.Failure)
            assertTrue(links.deletes.isEmpty())
        }

    @Test
    fun unlinkRejectsInactiveOrNonLoginEndpoints() =
        runTest {
            val source = password("source")
            val inactive = password("inactive").copy(
                identity = password("inactive").identity.copy(
                    timestamps = EntryTimestamps(createdAtMs = 1L, deletedAtMs = 2L),
                ),
            )
            val nonLogin = password("card").copy(
                identity = password("card").identity.copy(type = EntryType.BANK_CARD),
            )
            val inactiveLink = relatedLink("source", "inactive")
            val nonLoginLink = relatedLink("source", "card")
            val repository = TestEntries(listOf(source, password("target"), inactive, nonLogin))
            val links = TestLinks(listOf(inactiveLink, nonLoginLink))
            val binding = RelatedPasswordLinks(repository, links, testLogger())

            val inactiveResult = binding.unlink(EntryId("source"), EntryId("inactive"), inactiveLink.id)
            val nonLoginResult = binding.unlink(EntryId("source"), EntryId("card"), nonLoginLink.id)

            assertTrue(inactiveResult is AppResult.Failure)
            assertTrue(nonLoginResult is AppResult.Failure)
            assertTrue(links.deletes.isEmpty())
        }

    @Test
    fun unlinkLeavesUnrelatedLinkTypesUntouched() =
        runTest {
            val repository = TestEntries(listOf(password("source"), password("target")))
            val unrelated =
                EntryLink.create(
                    id = EntryLinkId("otp-link"),
                    sourceEntryId = EntryId("source"),
                    targetEntryId = EntryId("target"),
                    relationType = EntryRelationType.OTP_FOR,
                    createdAt = 1L,
                )
            val links = TestLinks(listOf(unrelated))
            val binding = RelatedPasswordLinks(repository, links, testLogger())

            val result = binding.unlink(EntryId("source"), EntryId("target"), unrelated.id)

            assertTrue(result is AppResult.Failure)
            assertTrue(links.deletes.isEmpty())
            assertEquals(listOf(unrelated), links.currentLinks)
        }

    @Test
    fun unlinkLogsRepositoryFailureAndPropagatesCancellation() =
        runTest {
            val repository = TestEntries(listOf(password("source"), password("target")))
            val edge = relatedLink("source", "target")
            val repositoryFailure = IllegalStateException("delete failed")
            val logger = RecordingTestLogger()
            val links = TestLinks(listOf(edge), deleteResult = AppResult.Failure(com.aozijx.passly.core.error.model.ValidationError()))
            val binding = RelatedPasswordLinks(repository, links, logger)

            val result = binding.unlink(EntryId("source"), EntryId("target"), edge.id)

            assertTrue(result is AppResult.Failure)
            assertEquals(1, logger.events.size)
            assertEquals(ErrorCodes.Security.VaultOperationFailed, logger.events.single().errorCode)
            assertEquals(Reasons.UnknownFailure, logger.events.single().reason)

            val cancellationFailure = CancellationException("cancelled")
            val cancellingLinks = TestLinks(listOf(edge), deleteFailure = cancellationFailure)
            val cancellingBinding = RelatedPasswordLinks(repository, cancellingLinks, logger)
            val cancellation = runCatching {
                cancellingBinding.unlink(EntryId("source"), EntryId("target"), edge.id)
            }.exceptionOrNull()

            assertSame(cancellationFailure, cancellation)
            assertEquals(1, logger.events.size)
        }

    @Test
    fun candidatesExcludeCurrentAndAlreadyRelatedSavedLoginEntries() =
        runTest {
            val current = password("current")
            val linked = password("linked")
            val available = password("available")
            val repository = TestEntries(listOf(current, linked, available))
            val links = TestLinks(listOf(relatedLink("current", "linked")))
            val binding = RelatedPasswordLinks(repository, links, testLogger())

            val candidates = binding.candidates(EntryId("current"))

            assertEquals(listOf("available"), candidates.map { it.id.value })
        }

    @Test
    fun bindCreatesOneCanonicalRelatedToLinkForTwoSavedPasswords() =
        runTest {
            val repository = TestEntries(listOf(password("z-current"), password("a-target")))
            val links = TestLinks()
            val binding = RelatedPasswordLinks(repository, links, testLogger())

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
            val binding = RelatedPasswordLinks(repository, links, testLogger())

            val result = binding.bind(EntryId("current"), EntryId("draft"))

            assertTrue(result is AppResult.Failure)
            assertTrue(links.upserts.isEmpty())
        }

    @Test
    fun bindingAnExistingSymmetricRelationIsIdempotent() =
        runTest {
            val repository = TestEntries(listOf(password("current"), password("target")))
            val links = TestLinks(listOf(relatedLink("target", "current")))
            val binding = RelatedPasswordLinks(repository, links, testLogger())

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
        private val deleteResult: AppResult<Unit> = AppResult.Success(Unit),
        private val deleteFailure: Exception? = null,
    ) : EntryLinkRepository {
        private val links = initial.toMutableList()
        val upserts = mutableListOf<EntryLink>()
        val deletes = mutableListOf<EntryLinkId>()
        val currentLinks: List<EntryLink> get() = links.toList()

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

        override suspend fun delete(linkId: EntryLinkId): AppResult<Unit> {
            deleteFailure?.let { throw it }
            deletes += linkId
            if (deleteResult is AppResult.Success) links.removeAll { it.id == linkId }
            return deleteResult
        }
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
