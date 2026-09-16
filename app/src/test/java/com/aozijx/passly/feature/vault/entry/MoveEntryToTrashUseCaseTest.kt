package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveEntryToTrashUseCaseTest {
    @Test
    fun lockedSessionDoesNotReadOrWriteEntry() = runTest {
        val fixture = fixture(AuthenticationState.Locked, entry())

        val result = fixture.useCase(EntryId("entry-id"))

        assertTrue((result as MoveEntryToTrashResult.Failed).error is SessionModeRestricted)
        assertEquals(0, fixture.query.reads)
        assertEquals(0, fixture.commands.moves)
    }

    @Test
    fun missingEntryReturnsNotFoundWithoutWriting() = runTest {
        val fixture = fixture(AuthenticationState.Authenticated(1L), null)

        val result = fixture.useCase(EntryId("missing"))

        assertTrue((result as MoveEntryToTrashResult.Failed).error is NotFound)
        assertEquals(1, fixture.query.reads)
        assertEquals(0, fixture.commands.moves)
    }

    @Test
    fun successfulMoveUsesCurrentVersion() = runTest {
        val fixture = fixture(AuthenticationState.Authenticated(1L), entry(version = EntryVersion(4)))

        val result = fixture.useCase(EntryId("entry-id"))

        assertEquals(MoveEntryToTrashResult.Moved, result)
        assertEquals(EntryId("entry-id") to EntryVersion(4), fixture.commands.lastMove)
    }

    @Test
    fun cancelledAuthorizationDoesNotReadOrWriteEntry() = runTest {
        val fixture = fixture(
            state = AuthenticationState.Authenticated(1L),
            entry = entry(),
            authorizationAllowed = false,
        )

        val result = fixture.useCase(EntryId("entry-id"))

        assertEquals(MoveEntryToTrashResult.NotAuthorized, result)
        assertEquals(0, fixture.query.reads)
        assertEquals(0, fixture.commands.moves)
        assertEquals(
            AuthorizationScope.Global(AuthenticationPurpose.DELETE_ENTRY),
            fixture.authorizationGate.lastScope,
        )
    }

    private fun fixture(
        state: AuthenticationState,
        entry: Entry?,
        authorizationAllowed: Boolean = true,
    ): Fixture {
        val query = RecordingQuery(entry)
        val commands = RecordingCommands()
        val authorizationGate = RecordingAuthorizationGate(authorizationAllowed)
        return Fixture(
            MoveEntryToTrashUseCase(
                commands,
                query,
                FixedAccess(state),
                authorizationGate,
            ),
            query,
            commands,
            authorizationGate,
        )
    }

    private fun entry(version: EntryVersion = EntryVersion.INITIAL) = Entry(
        identity = EntryIdentity(EntryId("entry-id"), EntryType.ACCOUNT, version, EntryTimestamps(1L)),
        profile = EntryProfile("Entry"),
    )

    private data class Fixture(
        val useCase: MoveEntryToTrashUseCase,
        val query: RecordingQuery,
        val commands: RecordingCommands,
        val authorizationGate: RecordingAuthorizationGate,
    )

    private class FixedAccess(state: AuthenticationState) : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> = MutableStateFlow(state)
        override fun isUnlocked() = authenticationState.value is AuthenticationState.Authenticated
    }

    private class RecordingQuery(private val entry: Entry?) : EntryQueryRepository {
        var reads = 0
        override suspend fun getById(entryId: EntryId): Entry? = entry.also { reads++ }
        override suspend fun findEntriesWithCustomIcons() = emptyList<Entry>()
        override suspend fun findAllTags() = emptySet<String>()
        override suspend fun count() = 0
    }

    private class RecordingCommands : EntryCommandRepository {
        var moves = 0
        var lastMove: Pair<EntryId, EntryVersion>? = null
        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion): AppResult<Unit> {
            moves++
            lastMove = id to expectedVersion
            return AppResult.Success(Unit)
        }
        override suspend fun createEntry(entry: Entry) = AppResult.Success(entry.id)
        override suspend fun updateEntry(id: EntryId, expectedVersion: EntryVersion, changes: EntryUpdate) = AppResult.Success(Unit)
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)
    }

    private class RecordingAuthorizationGate(
        private val allowed: Boolean,
    ) : AuthorizationGate {
        var lastScope: AuthorizationScope? = null

        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> {
            lastScope = scope
            return if (allowed) {
                AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
            } else {
                AuthorizationResult.Cancelled
            }
        }
    }
}
