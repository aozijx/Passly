package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import com.aozijx.passly.domain.entry.model.EntrySecret
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailSessionQueryTest {
    @Test
    fun `related account group and password links load through one batch`() = runTest {
        val current = entry("login", EntryType.LOGIN)
        val related = listOf(
            entry("account", EntryType.ACCOUNT),
            entry("otp", EntryType.OTP),
            entry("sibling", EntryType.LOGIN),
        )
        val query = RecordingEntryQuery(current, related)
        val sessionQuery =
            DetailSessionQuery(
                entryQueryRepository = query,
                sensitiveFieldRepository = EmptySensitiveFieldRepository,
                entryLinkRepository = TestLinks(
                    listOf(
                        link("member", "login", "account", EntryRelationType.MEMBER_OF_ACCOUNT),
                        link("otp", "otp", "login", EntryRelationType.OTP_FOR),
                        link("related", "login", "sibling", EntryRelationType.RELATED_TO),
                    ),
                ),
            )

        val result = sessionQuery.open(current.id)

        assertEquals(listOf(EntryId("account"), EntryId("otp"), EntryId("sibling")), query.batchRequests.single())
        assertEquals(listOf("account", "otp", "sibling"), result?.relatedEntries?.map { it.id.value })
        assertEquals(1, query.singleReads)
    }

    private class RecordingEntryQuery(
        private val current: Entry,
        related: List<Entry>,
    ) : EntryQueryRepository {
        private val entries = related.associateBy(Entry::id)
        val batchRequests = mutableListOf<List<EntryId>>()
        var singleReads = 0

        override suspend fun getById(entryId: EntryId): Entry? {
            singleReads++
            return current.takeIf { it.id == entryId }
        }

        override suspend fun getByIds(entryIds: List<EntryId>): List<Entry> {
            batchRequests += entryIds
            return entryIds.mapNotNull(entries::get)
        }
    }

    private class TestLinks(
        private val links: List<EntryLink>,
    ) : EntryLinkRepository {
        override fun observeAll(): Flow<List<EntryLink>> = emptyFlow()
        override fun observeLinks(entryId: EntryId): Flow<List<EntryLink>> = emptyFlow()
        override suspend fun getAll(): List<EntryLink> = links
        override suspend fun getLinks(entryId: EntryId): List<EntryLink> = links.filter {
            it.sourceEntryId == entryId || it.targetEntryId == entryId
        }
        override suspend fun upsert(link: EntryLink): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun delete(linkId: EntryLinkId): AppResult<Unit> = AppResult.Success(Unit)
    }

    private object EmptySensitiveFieldRepository : SensitiveFieldRepository {
        override suspend fun getPresence(entryId: EntryId) = SensitiveFieldPresence(entryId, emptySet())
        override suspend fun reveal(
            entryId: EntryId,
            key: SensitiveFieldKey,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): RevealedSensitiveField? = null
        override suspend fun revealMany(
            entryId: EntryId,
            keys: Set<SensitiveFieldKey>,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): List<RevealedSensitiveField> = emptyList()
        override suspend fun readBundle(entryId: EntryId): EntrySecret = EntrySecret()
    }

    private fun entry(id: String, type: EntryType) =
        Entry(
            identity = EntryIdentity(EntryId(id), type, timestamps = EntryTimestamps(1L)),
            profile = EntryProfile(title = id),
            secret =
                when (type) {
                    EntryType.LOGIN -> EntrySecret(LoginCredential())
                    EntryType.OTP -> EntrySecret(OtpCredential(OtpConfig()))
                    else -> EntrySecret()
                },
        )

    private fun link(id: String, source: String, target: String, type: EntryRelationType) =
        EntryLink.create(
            id = EntryLinkId(id),
            sourceEntryId = EntryId(source),
            targetEntryId = EntryId(target),
            relationType = type,
            createdAt = 1L,
        )
}
