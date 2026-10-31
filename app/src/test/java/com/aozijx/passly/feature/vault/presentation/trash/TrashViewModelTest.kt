package com.aozijx.passly.feature.vault.presentation.trash

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.R
import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
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
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.feature.vault.trash.TrashCommandUseCase
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `restore reserves busy state before dispatch and blocks competing commands`() = runTest {
        val release = CompletableDeferred<Unit>()
        val commands = RecordingCommands { release.await() }
        val authorization = RecordingAuthorization { true }
        val model = model(flowOf(listOf(entry)), commands, authorization)
        runCurrent()
        model.onAction(TrashUiAction.Restore(entry.id.value, entry.identity.version.value))
        assertEquals(TrashOperationState.Entry(entry.id.value), model.uiState.value.operation)
        model.onAction(TrashUiAction.Restore(entry.id.value, entry.identity.version.value))
        model.onAction(TrashUiAction.DeletePermanently(entry.id.value, entry.identity.version.value))
        model.onAction(TrashUiAction.Empty)
        runCurrent()
        assertEquals(1, commands.restores)
        assertEquals(0, authorization.calls)
        assertEquals(0, commands.deletes)
        assertEquals(0, commands.empties)
        release.complete(Unit)
        runCurrent()
        assertEquals(TrashOperationState.Idle, model.uiState.value.operation)
        clear(model)
    }

    @Test
    fun `permanent delete reserves busy state before dispatch and authorizes once`() = runTest {
        val release = CompletableDeferred<Unit>()
        val commands = RecordingCommands { release.await() }
        val authorization = RecordingAuthorization { true }
        val model = model(flowOf(listOf(entry)), commands, authorization)
        runCurrent()
        model.onAction(TrashUiAction.DeletePermanently(entry.id.value, entry.identity.version.value))
        assertTrue(model.uiState.value.isBusy)
        model.onAction(TrashUiAction.DeletePermanently(entry.id.value, entry.identity.version.value))
        model.onAction(TrashUiAction.Empty)
        runCurrent()
        assertEquals(1, authorization.calls)
        assertEquals(1, commands.deletes)
        assertEquals(0, commands.empties)
        release.complete(Unit)
        runCurrent()
        assertEquals(TrashOperationState.Idle, model.uiState.value.operation)
        clear(model)
    }

    @Test
    fun `repository exception maps to command failure and a later retry succeeds`() = runTest {
        var calls = 0
        val commands = object : EntryCommandRepository by UnusedCommands {
            override suspend fun restoreEntry(
                id: EntryId,
                expectedVersion: EntryVersion,
            ): AppResult<Unit> {
                calls++
                if (calls == 1) error("repository unavailable")
                return AppResult.Success(Unit)
            }
        }
        val model = model(flowOf(listOf(entry)), commands)
        runCurrent()

        model.onAction(TrashUiAction.Restore(entry.id.value, entry.identity.version.value))
        runCurrent()

        assertEquals(
            TrashOperationState.Failed(UiErrorMessage(R.string.trash_error_command)),
            model.uiState.value.operation,
        )
        assertTrue(!model.uiState.value.isBusy)
        model.onAction(TrashUiAction.ClearError)
        model.onAction(TrashUiAction.Restore(entry.id.value, entry.identity.version.value))
        runCurrent()
        assertEquals(TrashOperationState.Idle, model.uiState.value.operation)
        assertEquals(2, calls)
        clear(model)
    }

    @Test
    fun `authorization exception maps to empty failure and retry remains available`() = runTest {
        var attempts = 0
        val commands = RecordingCommands { }
        val authorization = RecordingAuthorization {
            if (++attempts == 1) error("authorization unavailable")
            true
        }
        val model = model(flowOf(listOf(entry)), commands, authorization)
        runCurrent()

        model.onAction(TrashUiAction.Empty)
        runCurrent()

        assertEquals(
            TrashOperationState.Failed(UiErrorMessage(R.string.trash_error_empty)),
            model.uiState.value.operation,
        )
        assertTrue(!model.uiState.value.isBusy)
        assertEquals(0, commands.empties)
        model.onAction(TrashUiAction.ClearError)
        model.onAction(TrashUiAction.Empty)
        runCurrent()
        assertEquals(TrashOperationState.Idle, model.uiState.value.operation)
        assertEquals(1, commands.empties)
        clear(model)
    }

    @Test
    fun `cancelling in-flight command does not publish a failure`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val commands = object : EntryCommandRepository by UnusedCommands {
            override suspend fun restoreEntry(
                id: EntryId,
                expectedVersion: EntryVersion,
            ): AppResult<Unit> {
                entered.complete(Unit)
                awaitCancellation()
            }
        }
        val model = model(flowOf(listOf(entry)), commands)
        runCurrent()
        model.onAction(TrashUiAction.Restore(entry.id.value, entry.identity.version.value))
        runCurrent()
        assertTrue(entered.isCompleted)

        clear(model)
        runCurrent()

        assertEquals(TrashUiState(), model.uiState.value)
    }

    @Test
    fun `command cancellation is not converted into an operation failure`() = runTest {
        val commands = object : EntryCommandRepository by UnusedCommands {
            override suspend fun restoreEntry(
                id: EntryId,
                expectedVersion: EntryVersion,
            ): AppResult<Unit> = throw CancellationException("restore cancelled")
        }
        val model = model(flowOf(listOf(entry)), commands)
        runCurrent()

        model.onAction(TrashUiAction.Restore(entry.id.value, entry.identity.version.value))
        runCurrent()

        assertEquals(TrashOperationState.Idle, model.uiState.value.operation)
        assertTrue(!model.uiState.value.isBusy)
        clear(model)
    }

    @Test
    fun `empty reserves authorization slot and cancelled authorization allows a later retry`() = runTest {
        val release = CompletableDeferred<Unit>()
        var allowed = false
        val commands = RecordingCommands { }
        val authorization = RecordingAuthorization { release.await(); allowed }
        val model = model(flowOf(listOf(entry)), commands, authorization)
        runCurrent()
        model.onAction(TrashUiAction.Empty)
        assertEquals(TrashOperationState.Emptying, model.uiState.value.operation)
        model.onAction(TrashUiAction.Empty)
        model.onAction(TrashUiAction.Restore(entry.id.value, entry.identity.version.value))
        runCurrent()
        assertEquals(1, authorization.calls)
        assertEquals(0, commands.restores)
        release.complete(Unit)
        runCurrent()
        assertEquals(TrashOperationState.Idle, model.uiState.value.operation)
        assertEquals(0, commands.empties)
        allowed = true
        model.onAction(TrashUiAction.Empty)
        runCurrent()
        assertEquals(2, authorization.calls)
        assertEquals(1, commands.empties)
        clear(model)
    }

    @Test
    fun `clearing view model cancels a queued write before repository access`() = runTest {
        val commands = RecordingCommands { }
        val model = model(flowOf(listOf(entry)), commands)
        runCurrent()
        model.onAction(TrashUiAction.Restore(entry.id.value, entry.identity.version.value))
        clear(model)
        model.onAction(TrashUiAction.Restore(entry.id.value, entry.identity.version.value))
        runCurrent()
        assertEquals(0, commands.restores)
        assertEquals(TrashUiState(), model.uiState.value)
    }

    @Test
    fun `retry restarts failed subscription once and keeps repository updates live`() = runTest {
        var subscriptions = 0
        val updates = MutableSharedFlow<List<EntryListItem>>(extraBufferCapacity = 1)
        val model = model(flow {
            if (++subscriptions == 1) error("synthetic query failure")
            emit(listOf(entry))
            emitAll(updates)
        })
        runCurrent()
        assertTrue(model.uiState.value.content is TrashContentState.Failed)
        model.onAction(TrashUiAction.ClearError)
        assertTrue(model.uiState.value.content is TrashContentState.Failed)
        model.onAction(TrashUiAction.RetryLoad)
        assertEquals(TrashContentState.Loading, model.uiState.value.content)
        model.onAction(TrashUiAction.RetryLoad)
        runCurrent()
        assertEquals(2, subscriptions)
        assertEquals(TrashContentState.Loaded(listOf(entry)), model.uiState.value.content)
        updates.emit(emptyList())
        runCurrent()
        assertEquals(TrashContentState.Loaded(emptyList()), model.uiState.value.content)
    }

    @Test
    fun `failed retries remain failures until a later successful query`() = runTest {
        var subscriptions = 0
        val model = model(flow {
            if (++subscriptions < 3) error("synthetic query failure")
            emit(emptyList())
        })
        runCurrent()
        model.onAction(TrashUiAction.RetryLoad)
        runCurrent()
        assertTrue(model.uiState.value.content is TrashContentState.Failed)
        model.onAction(TrashUiAction.RetryLoad)
        runCurrent()
        assertEquals(3, subscriptions)
        assertEquals(TrashContentState.Loaded(emptyList()), model.uiState.value.content)
    }

    @Test
    fun `destroyed model cancels reading and cannot publish late rows`() = runTest {
        val release = CompletableDeferred<List<EntryListItem>>()
        var subscriptions = 0
        val model = model(flow {
            subscriptions++
            emit(withContext(NonCancellable) { release.await() })
        })
        runCurrent()
        clear(model)
        release.complete(listOf(entry))
        model.onAction(TrashUiAction.RetryLoad)
        runCurrent()
        assertEquals(1, subscriptions)
        assertEquals(TrashUiState(), model.uiState.value)
    }

    private fun clear(model: TrashViewModel) {
        ViewModelStore().apply { put("trash", model); clear() }
    }

    private fun model(
        rows: Flow<List<EntryListItem>>,
        commands: EntryCommandRepository = UnusedCommands,
        authorization: AuthorizationGate = RecordingAuthorization { false },
    ): TrashViewModel {
        val queries = object : EntryListQueryRepository {
            override val deletedEntries = rows
            override val availableCategories = flowOf(emptyList<String>())
        }
        val access = object : SecureSessionAccessState {
            override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))
            override fun isUnlocked() = true
        }
        val entryQueries = object : EntryQueryRepository {
            override suspend fun getById(entryId: EntryId) = null
            override suspend fun findEntriesWithCustomIcons() = emptyList<Entry>()
            override suspend fun count() = 0
        }
        return TrashViewModel(queries, TrashCommandUseCase(commands, entryQueries, access, authorization))
    }

    private class RecordingAuthorization(private val allowed: suspend () -> Boolean) : AuthorizationGate {
        var calls = 0
        override suspend fun <T> authorize(scope: AuthorizationScope, input: AuthInput,
            block: suspend (AuthorizationPermit) -> T): AuthorizationResult<T> {
            calls++
            return if (allowed()) AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
            else AuthorizationResult.Cancelled
        }
    }

    private class RecordingCommands(private val wait: suspend () -> Unit) : EntryCommandRepository by UnusedCommands {
        var restores = 0
        var deletes = 0
        var empties = 0
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion): AppResult<Unit> {
            restores++; wait(); return AppResult.Success(Unit)
        }
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion): AppResult<Unit> {
            deletes++; wait(); return AppResult.Success(Unit)
        }
        override suspend fun emptyTrash(): AppResult<Int> {
            empties++; wait(); return AppResult.Success(1)
        }
    }

    private val entry = EntryListItem(
        identity = EntryIdentity(EntryId("deleted"), EntryType.LOGIN, timestamps = EntryTimestamps(1L, deletedAtMs = 2L)),
        profile = EntryProfile(title = "Deleted entry"),
    )

    private object UnusedCommands : EntryCommandRepository {
        override suspend fun createEntry(entry: Entry) = AppResult.Success(entry.id)
        override suspend fun updateEntry(id: EntryId, expectedVersion: EntryVersion, changes: EntryUpdate) = AppResult.Success(Unit)
        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)
    }
}
