package com.aozijx.passly.feature.vault.trash

import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
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

class TrashCommandUseCaseTest {
    @Test
    fun lockedSessionDoesNotQueryOrMoveEntryToTrash() = runTest {
        val fixture = fixture(AuthenticationState.Locked, entry = entry())

        val result = fixture.useCase.moveToTrash(EntryId("entry"))

        assertTrue((result as TrashCommandResult.Failed).error is SessionModeRestricted)
        assertEquals(0, fixture.query.reads)
        assertEquals(0, fixture.commands.moves)
    }

    @Test
    fun missingEntryIsNotMovedToTrash() = runTest {
        val fixture = fixture(AuthenticationState.Authenticated(1L))

        val result = fixture.useCase.moveToTrash(EntryId("missing"))

        assertTrue((result as TrashCommandResult.Failed).error is NotFound)
        assertEquals(1, fixture.query.reads)
        assertEquals(0, fixture.commands.moves)
    }

    @Test
    fun moveToTrashUsesCurrentEntryVersion() = runTest {
        val fixture = fixture(
            AuthenticationState.Authenticated(1L),
            entry = entry(EntryVersion(4)),
        )

        val result = fixture.useCase.moveToTrash(EntryId("entry"))

        assertEquals(TrashCommandResult.Completed, result)
        assertEquals(EntryId("entry") to EntryVersion(4), fixture.commands.lastMove)
    }

    @Test
    fun cancelledMoveAuthorizationDoesNotQueryOrMoveEntry() = runTest {
        val fixture = fixture(
            state = AuthenticationState.Authenticated(1L),
            entry = entry(),
            authorizationAllowed = false,
        )

        val result = fixture.useCase.moveToTrash(EntryId("entry"))

        assertEquals(TrashCommandResult.NotAuthorized, result)
        assertEquals(0, fixture.query.reads)
        assertEquals(0, fixture.commands.moves)
        assertEquals(
            AuthorizationScope.Global(AuthenticationPurpose.DELETE_ENTRY),
            fixture.authorization.lastScope,
        )
    }

    @Test
    fun lockedSessionCannotRestore() = runTest {
        val fixture = fixture(AuthenticationState.Locked)

        val result = fixture.useCase.restore(EntryId("entry"), EntryVersion(2))

        assertTrue((result as TrashCommandResult.Failed).error is SessionModeRestricted)
        assertEquals(0, fixture.commands.restores)
    }

    @Test
    fun restoreUsesExpectedVersionWithoutFreshAuthorization() = runTest {
        val fixture = fixture(AuthenticationState.Authenticated(1L))

        val result = fixture.useCase.restore(EntryId("entry"), EntryVersion(3))

        assertEquals(TrashCommandResult.Completed, result)
        assertEquals(EntryId("entry") to EntryVersion(3), fixture.commands.lastRestore)
        assertEquals(null, fixture.authorization.lastScope)
    }

    @Test
    fun cancelledAuthorizationCannotDeletePermanently() = runTest {
        val fixture = fixture(
            state = AuthenticationState.Authenticated(1L),
            authorizationAllowed = false,
        )

        val result = fixture.useCase.deletePermanently(EntryId("entry"), EntryVersion(4))

        assertEquals(TrashCommandResult.NotAuthorized, result)
        assertEquals(0, fixture.commands.permanentDeletes)
        assertEquals(
            AuthorizationScope.Global(AuthenticationPurpose.DELETE_ENTRY),
            fixture.authorization.lastScope,
        )
    }

    @Test
    fun authorizedEmptyTrashExecutesRepositoryCommand() = runTest {
        val fixture = fixture(AuthenticationState.Authenticated(1L))

        val result = fixture.useCase.empty()

        assertEquals(TrashCommandResult.Completed, result)
        assertEquals(1, fixture.commands.emptyCalls)
    }

    private fun fixture(
        state: AuthenticationState,
        entry: Entry? = null,
        authorizationAllowed: Boolean = true,
    ): Fixture {
        val commands = RecordingCommands()
        val query = RecordingQuery(entry)
        val authorization = RecordingAuthorizationGate(authorizationAllowed)
        return Fixture(
            useCase = TrashCommandUseCase(commands, query, FixedAccess(state), authorization),
            query = query,
            commands = commands,
            authorization = authorization,
        )
    }

    private fun entry(version: EntryVersion = EntryVersion.INITIAL) = Entry(
        identity = EntryIdentity(EntryId("entry"), EntryType.ACCOUNT, version, EntryTimestamps(1L)),
        profile = EntryProfile("Entry"),
    )

    private data class Fixture(
        val useCase: TrashCommandUseCase,
        val query: RecordingQuery,
        val commands: RecordingCommands,
        val authorization: RecordingAuthorizationGate,
    )

    private class FixedAccess(state: AuthenticationState) : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> = MutableStateFlow(state)
        override fun isUnlocked() = authenticationState.value is AuthenticationState.Authenticated
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

    private class RecordingCommands : EntryCommandRepository {
        var moves = 0
        var restores = 0
        var permanentDeletes = 0
        var emptyCalls = 0
        var lastRestore: Pair<EntryId, EntryVersion>? = null

        var lastMove: Pair<EntryId, EntryVersion>? = null

        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion): AppResult<Unit> {
            moves++
            lastMove = id to expectedVersion
            return AppResult.Success(Unit)
        }

        override suspend fun restoreEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
        ): AppResult<Unit> {
            restores++
            lastRestore = id to expectedVersion
            return AppResult.Success(Unit)
        }

        override suspend fun deletePermanently(
            id: EntryId,
            expectedVersion: EntryVersion,
        ): AppResult<Unit> {
            permanentDeletes++
            return AppResult.Success(Unit)
        }

        override suspend fun emptyTrash(): AppResult<Int> {
            emptyCalls++
            return AppResult.Success(1)
        }

        override suspend fun createEntry(entry: Entry) = AppResult.Success(entry.id)
        override suspend fun updateEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
            changes: EntryUpdate,
        ) = AppResult.Success(Unit)

    }

    private class RecordingQuery(private val entry: Entry?) : EntryQueryRepository {
        var reads = 0

        override suspend fun getById(entryId: EntryId): Entry? = entry.also { reads++ }
        override suspend fun findEntriesWithCustomIcons() = emptyList<Entry>()
        override suspend fun count() = 0
    }
}
