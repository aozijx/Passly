package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailEntryUpdateCoordinatorTest {

    @Test
    fun updateAppliesPatchToLatestEntryAndReturnsReloadedVersion() = runTest {
        val query = FakeQueryRepository(entry(version = 7, notes = "latest notes"))
        val command = FakeCommandRepository(query)
        val coordinator = DetailEntryUpdateCoordinator(query, command)

        val result = coordinator.update(ENTRY_ID, DetailEntryPatch.Title("Renamed"))

        val updated = (result as AppResult.Success<Entry>).data
        assertEquals("Renamed", updated.title)
        assertEquals("latest notes", updated.secret.notes)
        assertEquals(8, updated.version.value)
        assertEquals(listOf(7), command.expectedVersions)
    }

    @Test
    fun updateRetriesOneConflictAgainstReloadedEntry() = runTest {
        val query = FakeQueryRepository(entry(version = 3, notes = "before conflict"))
        val command = FakeCommandRepository(query).apply {
            failures += Conflict(errorId = "first-conflict")
            onFailure = {
                query.current = entry(version = 4, notes = "concurrent notes")
            }
        }
        val coordinator = DetailEntryUpdateCoordinator(query, command)

        val result = coordinator.update(ENTRY_ID, DetailEntryPatch.Title("Renamed"))

        val updated = (result as AppResult.Success<Entry>).data
        assertEquals("Renamed", updated.title)
        assertEquals("concurrent notes", updated.secret.notes)
        assertEquals(5, updated.version.value)
        assertEquals(listOf(3, 4), command.expectedVersions)
    }

    @Test
    fun updateReturnsSecondConflictWithoutThirdAttempt() = runTest {
        val query = FakeQueryRepository(entry(version = 3))
        val command = FakeCommandRepository(query).apply {
            failures += Conflict(errorId = "first-conflict")
            failures += Conflict(errorId = "second-conflict")
        }
        val coordinator = DetailEntryUpdateCoordinator(query, command)

        val result = coordinator.update(ENTRY_ID, DetailEntryPatch.Title("Renamed"))

        assertTrue((result as AppResult.Failure).error is Conflict)
        assertEquals(2, command.expectedVersions.size)
    }

    @Test
    fun updateDoesNotRetryNonConflictFailure() = runTest {
        val query = FakeQueryRepository(entry(version = 3))
        val command = FakeCommandRepository(query).apply {
            failures += ValidationError(errorId = "validation")
        }
        val coordinator = DetailEntryUpdateCoordinator(query, command)

        val result = coordinator.update(ENTRY_ID, DetailEntryPatch.Title("Renamed"))

        assertTrue((result as AppResult.Failure).error is ValidationError)
        assertEquals(1, command.expectedVersions.size)
    }

    @Test
    fun updateReturnsNotFoundWhenEntryDisappears() = runTest {
        val query = FakeQueryRepository(null)
        val coordinator = DetailEntryUpdateCoordinator(query, FakeCommandRepository(query))

        val result = coordinator.update(ENTRY_ID, DetailEntryPatch.Title("Renamed"))

        assertTrue((result as AppResult.Failure).error is NotFound)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun concurrentUpdatesAreSerializedAndSecondUsesFirstResult() = runTest {
        val query = FakeQueryRepository(entry(version = 1))
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val command = FakeCommandRepository(query).apply {
            beforeFirstSuccess = {
                firstStarted.complete(Unit)
                releaseFirst.await()
            }
        }
        val coordinator = DetailEntryUpdateCoordinator(query, command)

        val first = async { coordinator.update(ENTRY_ID, DetailEntryPatch.Title("First")) }
        firstStarted.await()
        val second = async { coordinator.update(ENTRY_ID, DetailEntryPatch.Notes("Second notes")) }
        runCurrent()

        assertEquals(listOf(1), command.expectedVersions)
        releaseFirst.complete(Unit)
        first.await()
        val secondResult = (second.await() as AppResult.Success<Entry>).data

        assertEquals(listOf(1, 2), command.expectedVersions)
        assertEquals("First", secondResult.title)
        assertEquals("Second notes", secondResult.secret.notes)
        assertEquals(3, secondResult.version.value)
    }

    private class FakeQueryRepository(initial: Entry?) : EntryQueryRepository {
        var current: Entry? = initial

        override suspend fun getById(entryId: EntryId): Entry? = current

        override suspend fun findEntriesWithCustomIcons(): List<Entry> =
            listOfNotNull(current?.takeIf { it.icon.customReference != null })

        override suspend fun findAllTags(): Set<String> = current?.tags.orEmpty()

        override suspend fun count(): Int = if (current == null) 0 else 1
    }

    private class FakeCommandRepository(
        private val query: FakeQueryRepository,
    ) : EntryCommandRepository {
        val failures = ArrayDeque<com.aozijx.passly.core.error.model.AppError>()
        val expectedVersions = mutableListOf<Int>()
        var onFailure: (() -> Unit)? = null
        var beforeFirstSuccess: (suspend () -> Unit)? = null

        override suspend fun updateEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
            changes: EntryUpdate,
        ): AppResult<Unit> {
            expectedVersions += expectedVersion.value
            failures.removeFirstOrNull()?.let { error ->
                onFailure?.invoke()
                return AppResult.Failure(error)
            }
            beforeFirstSuccess?.let { block ->
                beforeFirstSuccess = null
                block()
            }
            val current = requireNotNull(query.current)
            query.current = current.copy(
                identity = current.identity.copy(version = current.version.next()),
                profile = changes.profile ?: current.profile,
                secret = changes.secret ?: current.secret,
            )
            return AppResult.Success(Unit)
        }

        override suspend fun createEntry(entry: Entry): AppResult<EntryId> =
            AppResult.Success(entry.id)

        override suspend fun moveToTrash(
            id: EntryId,
            expectedVersion: EntryVersion,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun restoreEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun deletePermanently(
            id: EntryId,
            expectedVersion: EntryVersion,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun emptyTrash(): AppResult<Int> = AppResult.Success(0)
    }

    private fun entry(
        version: Int,
        notes: String = "notes",
    ) = Entry(
        identity = EntryIdentity(
            id = ENTRY_ID,
            type = EntryType.LOGIN,
            version = EntryVersion(version),
            timestamps = EntryTimestamps(createdAtMs = 1, updatedAtMs = version.toLong()),
        ),
        profile = EntryProfile(
            title = "Original",
            username = "latest-user",
            favorite = true,
            tags = setOf("Latest"),
        ),
        secret = EntrySecret(
            credential = LoginCredential(password = "latest-password"),
            notes = notes,
        ),
    )

    private companion object {
        val ENTRY_ID = EntryId("entry-1")
    }
}
