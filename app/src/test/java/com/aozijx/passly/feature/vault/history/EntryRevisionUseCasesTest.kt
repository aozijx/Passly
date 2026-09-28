package com.aozijx.passly.feature.vault.history

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.domain.access.model.SensitiveRevisionAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.sensitive.RevealedSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.RevealedRevisionSensitiveField
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldPresence
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class EntryRevisionUseCasesTest {
    @Test
    fun `reveal authorizes and forwards exact revision key set`() = runBlocking {
        val repository = RecordingRevisionRepository()
        val gate = RecordingAuthorizationGate()
        val useCase = RevealRevisionFieldsUseCase(repository, gate)
        val keys = setOf(SensitiveFieldKey.PASSWORD, SensitiveFieldKey.OTP_SECRET)

        val result = useCase(EntryId("entry"), EntryRevisionId("revision"), keys)

        assertEquals(
            AuthorizationScope.SensitiveRevision(
                entryId = EntryId("entry"),
                revisionId = "revision",
                fieldKeys = keys,
                action = SensitiveRevisionAccessAction.REVEAL,
            ),
            gate.scope,
        )
        assertEquals(RevisionOperationResult.Succeeded(emptyList<RevealedRevisionSensitiveField>()), result)
        assertEquals(keys, repository.revealedKeys)
    }

    @Test
    fun `reveal cancellation is not reported as an error`() = runBlocking {
        val repository = RecordingRevisionRepository()
        val useCase = RevealRevisionFieldsUseCase(
            repository,
            RecordingAuthorizationGate(AuthorizationResult.Cancelled),
        )

        val result = useCase(
            EntryId("entry"),
            EntryRevisionId("revision"),
            setOf(SensitiveFieldKey.PASSWORD),
        )

        assertSame(RevisionOperationResult.Cancelled, result)
        assertEquals(null, repository.revealedKeys)
    }

    @Test
    fun `restore authorizes union and forwards current version once`() = runBlocking {
        val repository = RecordingRevisionRepository(
            snapshot = snapshot(setOf(SensitiveFieldKey.PASSWORD)),
            restoreResult = AppResult.failure(Conflict()),
        )
        val gate = RecordingAuthorizationGate()
        val useCase = RestoreEntryRevisionUseCase(
            repository,
            FakeEntryRepository(currentEntry()),
            FakeSensitiveRepository(setOf(SensitiveFieldKey.OTP_SECRET)),
            gate,
        )

        val result = useCase(EntryId("entry"), EntryRevisionId("revision"))

        assertSame(RevisionOperationResult.Stale, result)
        assertEquals(
            AuthorizationScope.SensitiveRevision(
                entryId = EntryId("entry"),
                revisionId = "revision",
                fieldKeys = setOf(SensitiveFieldKey.PASSWORD, SensitiveFieldKey.OTP_SECRET),
                action = SensitiveRevisionAccessAction.RESTORE,
            ),
            gate.scope,
        )
        assertEquals(EntryVersion(7), repository.restoredVersion)
        assertEquals(1, repository.restoreCalls)
    }

    @Test
    fun `restore without sensitive fields bypasses authorization`() = runBlocking {
        val repository = RecordingRevisionRepository(snapshot = snapshot(emptySet()))
        val gate = RecordingAuthorizationGate()
        val useCase = RestoreEntryRevisionUseCase(
            repository,
            FakeEntryRepository(currentEntry()),
            FakeSensitiveRepository(emptySet()),
            gate,
        )

        val result = useCase(EntryId("entry"), EntryRevisionId("revision"))

        assertEquals(RevisionOperationResult.Succeeded(EntryVersion(8)), result)
        assertEquals(null, gate.scope)
        assertEquals(null, repository.restorePermit)
    }

    private object TestPermit : AuthorizationPermit

    private class RecordingAuthorizationGate(
        private val result: AuthorizationResult<Any?>? = null,
    ) : AuthorizationGate {
        var scope: AuthorizationScope? = null

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> {
            this.scope = scope
            return result as AuthorizationResult<T>? ?: AuthorizationResult.Allowed(block(TestPermit))
        }
    }

    private class RecordingRevisionRepository(
        private val snapshot: RedactedEntryRevision? = null,
        private val restoreResult: AppResult<EntryVersion> = AppResult.success(EntryVersion(8)),
    ) : EntryRevisionRepository {
        var revealedKeys: Set<SensitiveFieldKey>? = null
        var restoredVersion: EntryVersion? = null
        var restorePermit: AuthorizationPermit? = null
        var restoreCalls: Int = 0

        override fun observeMetadata(entryId: EntryId): Flow<List<com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata>> = emptyFlow()

        override suspend fun loadRedacted(entryId: EntryId, revisionId: EntryRevisionId) =
            snapshot?.let { AppResult.success(it) } ?: error("unused")

        override suspend fun reveal(
            entryId: EntryId,
            revisionId: EntryRevisionId,
            keys: Set<SensitiveFieldKey>,
            permit: AuthorizationPermit,
        ): AppResult<List<RevealedRevisionSensitiveField>> {
            revealedKeys = keys
            return AppResult.success(emptyList())
        }

        override suspend fun restore(
            entryId: EntryId,
            revisionId: EntryRevisionId,
            expectedVersion: EntryVersion,
            permit: AuthorizationPermit?,
        ): AppResult<EntryVersion> {
            restoreCalls += 1
            restoredVersion = expectedVersion
            restorePermit = permit
            return restoreResult
        }
    }

    private class FakeEntryRepository(private val entry: Entry) : EntryQueryRepository {
        override suspend fun getById(entryId: EntryId) = entry
        override suspend fun findEntriesWithCustomIcons() = emptyList<Entry>()
        override suspend fun count() = 1
    }

    private class FakeSensitiveRepository(
        private val keys: Set<SensitiveFieldKey>,
    ) : SensitiveFieldRepository {
        override suspend fun getPresence(entryId: EntryId) = SensitiveFieldPresence(entryId, keys)
        override suspend fun reveal(
            entryId: EntryId,
            key: SensitiveFieldKey,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): RevealedSensitiveField? = error("unused")
        override suspend fun revealMany(
            entryId: EntryId,
            keys: Set<SensitiveFieldKey>,
            action: SensitiveAccessAction,
            permit: AuthorizationPermit,
        ): List<RevealedSensitiveField> = error("unused")
        override suspend fun readBundle(entryId: EntryId): EntrySecret = error("unused")
        override suspend fun readAll(entryId: EntryId): EntrySecret = error("unused")
    }

    private fun currentEntry() = Entry(
        identity = EntryIdentity(
            id = EntryId("entry"),
            type = EntryType.LOGIN,
            version = EntryVersion(7),
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile("Current"),
        secret = EntrySecret(credential = com.aozijx.passly.domain.entry.model.credential.LoginCredential()),
    )

    private fun snapshot(keys: Set<SensitiveFieldKey>) = RedactedEntryRevision(
        metadata = EntryRevisionMetadata(
            id = EntryRevisionId("revision"),
            entryId = EntryId("entry"),
            version = EntryVersion(1),
            createdAtMs = 1L,
            change = RevisionChange.VALUE_CHANGED,
        ),
        profile = EntryProfile("Historical"),
        secret = EntrySecret(credential = com.aozijx.passly.domain.entry.model.credential.LoginCredential()),
        sensitiveFieldKeys = keys,
    )
}
