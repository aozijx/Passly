package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailEntryLoaderTest {
    @Test
    fun existingEntryLoadsItsSensitiveFieldPresenceAsOneSnapshot() = runTest {
        val entry = accountEntry()
        val presence = SensitiveFieldPresence(entry.id, setOf(SensitiveFieldKey.PASSWORD))
        val sensitiveFields = FakeSensitiveFieldRepository(presence)
        val loader = DetailEntryLoader(
            entryQueryRepository = FakeEntryQueryRepository(entry),
            sensitiveFieldRepository = sensitiveFields,
        )

        assertEquals(
            DetailEntrySnapshot(entry, presence.keys),
            loader.load(entry.id),
        )
        assertEquals(listOf(entry.id), sensitiveFields.presenceRequests)
    }

    @Test
    fun missingEntryDoesNotReadSensitiveFieldMetadata() = runTest {
        val sensitiveFields = FakeSensitiveFieldRepository(
            SensitiveFieldPresence(EntryId("unused"), emptySet()),
        )
        val loader = DetailEntryLoader(
            entryQueryRepository = FakeEntryQueryRepository(null),
            sensitiveFieldRepository = sensitiveFields,
        )

        assertNull(loader.load(EntryId("missing")))
        assertEquals(emptyList<EntryId>(), sensitiveFields.presenceRequests)
    }

    private fun accountEntry() = Entry(
        identity = EntryIdentity(
            id = EntryId("account"),
            type = EntryType.ACCOUNT,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile("Account"),
    )

    private class FakeEntryQueryRepository(
        private val entry: Entry?,
    ) : EntryQueryRepository {
        override suspend fun getById(entryId: EntryId): Entry? = entry
        override suspend fun findEntriesWithCustomIcons() = emptyList<Entry>()
        override suspend fun findAllTags() = emptySet<String>()
        override suspend fun count() = if (entry == null) 0 else 1
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
}
