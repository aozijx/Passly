package com.aozijx.passly.feature.vault.presentation.trash

import com.aozijx.passly.core.error.result.AppResult
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
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.feature.vault.trash.TrashCommandUseCase
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
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
        model.javaClass.getDeclaredMethod("onCleared").apply { isAccessible = true }.invoke(model)
        release.complete(listOf(entry))
        model.onAction(TrashUiAction.RetryLoad)
        runCurrent()
        assertEquals(1, subscriptions)
        assertEquals(TrashUiState(), model.uiState.value)
    }

    private fun model(rows: Flow<List<EntryListItem>>): TrashViewModel {
        val queries = object : EntryListQueryRepository {
            override val deletedEntries = rows
            override val availableCategories = flowOf(emptyList<String>())
        }
        val access = object : SecureSessionAccessState {
            override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))
            override fun isUnlocked() = true
        }
        val authorization = object : AuthorizationGate {
            override suspend fun <T> authorize(scope: AuthorizationScope, input: AuthInput,
                block: suspend (AuthorizationPermit) -> T): AuthorizationResult<T> = AuthorizationResult.Cancelled
        }
        return TrashViewModel(queries, TrashCommandUseCase(UnusedCommands, access, authorization))
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
