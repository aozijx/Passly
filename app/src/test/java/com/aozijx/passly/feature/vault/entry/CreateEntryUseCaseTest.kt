package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateEntryUseCaseTest {
    @Test
    fun unauthorizedDraft_doesNotCreateIdentityOrTouchRepository() = runBlocking {
        val fixture = fixture(AuthenticationState.Locked)

        val result = fixture.useCase(validLoginDraft())

        assertTrue((result as AppResult.Failure).error is SessionModeRestricted)
        assertEquals(0, fixture.repository.createCalls)
        assertEquals(0, fixture.idCalls)
        assertEquals(0, fixture.clockCalls)
    }

    @Test
    fun invalidDraft_doesNotCreateIdentityOrTouchRepository() = runBlocking {
        val fixture = fixture(AuthenticationState.Authenticated(1L))
        val invalid = EntryDraft(EntryDraftTarget.New(EntryType.LOGIN))
            .withValue(
                EntryTypeDefinitions[EntryType.LOGIN],
                FieldKey.TITLE,
                EntryDraftValue.Text("Mail"),
            )

        val result = fixture.useCase(invalid)

        assertTrue((result as AppResult.Failure).error is ValidationError)
        assertEquals(0, fixture.repository.createCalls)
        assertEquals(0, fixture.idCalls)
        assertEquals(0, fixture.clockCalls)
    }

    @Test
    fun existingTarget_isRejectedBeforeIdentityCreation() = runBlocking {
        val fixture = fixture(AuthenticationState.Authenticated(1L))
        val definition = EntryTypeDefinitions[EntryType.LOGIN]
        val draft = EntryDraft(
            EntryDraftTarget.Existing(EntryId("existing"), EntryType.LOGIN, EntryVersion.INITIAL),
        )
            .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text("Mail"))
            .withValue(definition, FieldKey.PASSWORD, EntryDraftValue.Text("secret"))

        val result = fixture.useCase(draft)

        assertTrue((result as AppResult.Failure).error is ValidationError)
        assertEquals(0, fixture.repository.createCalls)
        assertEquals(0, fixture.idCalls)
        assertEquals(0, fixture.clockCalls)
    }

    @Test
    fun validDraft_createsIdentityAndTimestampOnceThenPersistsOnce() = runBlocking {
        val fixture = fixture(AuthenticationState.Authenticated(1L))

        val result = fixture.useCase(validLoginDraft())

        assertEquals(AppResult.Success(EntryId("generated-id")), result)
        assertEquals(1, fixture.repository.createCalls)
        assertEquals(1, fixture.idCalls)
        assertEquals(1, fixture.clockCalls)
        val entry = requireNotNull(fixture.repository.createdEntry)
        assertEquals(EntryId("generated-id"), entry.id)
        assertEquals(EntryVersion.INITIAL, entry.version)
        assertEquals(123L, entry.createdAt)
        assertEquals(123L, entry.updatedAt)
    }

    @Test
    fun repositoryFailure_isReturnedUnchanged() = runBlocking {
        val failure = AppResult.Failure(Conflict(errorId = "conflict"))
        val fixture = fixture(AuthenticationState.Authenticated(1L), failure)

        val result = fixture.useCase(validLoginDraft())

        assertSame(failure, result)
        assertEquals(1, fixture.repository.createCalls)
    }

    private fun validLoginDraft(): EntryDraft {
        val definition = EntryTypeDefinitions[EntryType.LOGIN]
        return EntryDraft(EntryDraftTarget.New(EntryType.LOGIN))
            .withValue(definition, FieldKey.TITLE, EntryDraftValue.Text("Mail"))
            .withValue(definition, FieldKey.PASSWORD, EntryDraftValue.Text(" secret "))
    }

    private fun fixture(
        state: AuthenticationState,
        repositoryResult: AppResult<EntryId> = AppResult.Success(EntryId("generated-id")),
    ): Fixture {
        val repository = RecordingEntryCommandRepository(repositoryResult)
        var idCalls = 0
        var clockCalls = 0
        val useCase = CreateEntryUseCase(
            entryCommandRepository = repository,
            secureSessionAccessState = FixedSecureSessionAccessState(state),
            materializer = EntryDraftMaterializer(),
            idFactory = { idCalls++; EntryId("generated-id") },
            clock = { clockCalls++; 123L },
        )
        return Fixture(useCase, repository, { idCalls }, { clockCalls })
    }

    private class Fixture(
        val useCase: CreateEntryUseCase,
        val repository: RecordingEntryCommandRepository,
        private val idCallCount: () -> Int,
        private val clockCallCount: () -> Int,
    ) {
        val idCalls get() = idCallCount()
        val clockCalls get() = clockCallCount()
    }

    private class FixedSecureSessionAccessState(state: AuthenticationState) : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> = MutableStateFlow(state)
        override fun isUnlocked(): Boolean = authenticationState.value is AuthenticationState.Authenticated ||
            authenticationState.value is AuthenticationState.RecoveryMode
    }

    private class RecordingEntryCommandRepository(
        private val createResult: AppResult<EntryId>,
    ) : EntryCommandRepository {
        var createCalls = 0
        var createdEntry: Entry? = null

        override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
            createCalls++
            createdEntry = entry
            return createResult
        }

        override suspend fun updateEntry(id: EntryId, expectedVersion: EntryVersion, changes: EntryUpdate) =
            AppResult.Success(Unit)
        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)
    }
}
