package com.aozijx.passly.feature.vault.detail

import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditDetailEntryUseCaseTest {

    @Test
    fun tagEditDoesNotSubmitTheIncompleteSecretBundle() = runTest {
        val query = FakeQueryRepository(
            entry(version = 1).copy(
                secret = EntrySecret(credential = LoginCredential(password = null)),
            ),
        )
        val command = FakeCommandRepository(query)
        val useCase = EditDetailEntryUseCase(query, command)

        val result = useCase.edit(ENTRY_ID, DetailEntryEdit.SetTags(emptySet()))

        assertTrue(result is AppResult.Success)
        assertEquals(emptySet<String>(), command.submittedChanges.single().profile?.tags)
        assertNull(command.submittedChanges.single().secret)
    }

    @Test
    fun updateAppliesPatchToLatestEntryAndReturnsReloadedVersion() = runTest {
        val query = FakeQueryRepository(entry(version = 7, notes = "latest notes"))
        val command = FakeCommandRepository(query)
        val useCase = EditDetailEntryUseCase(query, command)

        val result = useCase.edit(ENTRY_ID, DetailEntryEdit.SetTitle("Renamed"))

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
        val useCase = EditDetailEntryUseCase(query, command)

        val result = useCase.edit(ENTRY_ID, DetailEntryEdit.SetTitle("Renamed"))

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
        val useCase = EditDetailEntryUseCase(query, command)

        val result = useCase.edit(ENTRY_ID, DetailEntryEdit.SetTitle("Renamed"))

        assertTrue((result as AppResult.Failure).error is Conflict)
        assertEquals(2, command.expectedVersions.size)
    }

    @Test
    fun updateDoesNotRetryNonConflictFailure() = runTest {
        val query = FakeQueryRepository(entry(version = 3))
        val command = FakeCommandRepository(query).apply {
            failures += ValidationError(errorId = "validation")
        }
        val useCase = EditDetailEntryUseCase(query, command)

        val result = useCase.edit(ENTRY_ID, DetailEntryEdit.SetTitle("Renamed"))

        assertTrue((result as AppResult.Failure).error is ValidationError)
        assertEquals(1, command.expectedVersions.size)
    }

    @Test
    fun updateReturnsNotFoundWhenEntryDisappears() = runTest {
        val query = FakeQueryRepository(null)
        val useCase = EditDetailEntryUseCase(query, FakeCommandRepository(query))

        val result = useCase.edit(ENTRY_ID, DetailEntryEdit.SetTitle("Renamed"))

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
        val useCase = EditDetailEntryUseCase(query, command)

        val first = async { useCase.edit(ENTRY_ID, DetailEntryEdit.SetTitle("First")) }
        firstStarted.await()
        val second = async { useCase.edit(ENTRY_ID, DetailEntryEdit.SetNotes("Second notes")) }
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

    @Test
    fun primaryUrlEditPreservesLatestApplicationIds() = runTest {
        val query = FakeQueryRepository(
            entry(
                version = 2,
                primaryUrl = "https://old.example.com",
                applicationIds = setOf("com.example.latest"),
            ),
        )
        val useCase = EditDetailEntryUseCase(query, FakeCommandRepository(query))

        val result = useCase.edit(
            ENTRY_ID,
            DetailEntryEdit.SetPrimaryUrl("https://new.example.com"),
        )

        val updated = (result as AppResult.Success<Entry>).data
        assertEquals("https://new.example.com", updated.associations.primaryUrl)
        assertEquals(setOf("com.example.latest"), updated.associations.applicationIds)
    }

    @Test
    fun applicationIdEditPreservesLatestPrimaryUrl() = runTest {
        val query = FakeQueryRepository(
            entry(
                version = 2,
                primaryUrl = "https://latest.example.com",
                applicationIds = setOf("com.example.old"),
            ),
        )
        val useCase = EditDetailEntryUseCase(query, FakeCommandRepository(query))

        val result = useCase.edit(
            ENTRY_ID,
            DetailEntryEdit.SetApplicationIds(setOf("com.example.new")),
        )

        val updated = (result as AppResult.Success<Entry>).data
        assertEquals("https://latest.example.com", updated.associations.primaryUrl)
        assertEquals(setOf("com.example.new"), updated.associations.applicationIds)
    }

    @Test
    fun toggleFavoriteReevaluatesLatestEntryAfterConflict() = runTest {
        val query = FakeQueryRepository(entry(version = 3, favorite = true))
        val command = FakeCommandRepository(query).apply {
            failures += Conflict(errorId = "first-conflict")
            onFailure = { query.current = entry(version = 4, favorite = false) }
        }
        val useCase = EditDetailEntryUseCase(query, command)

        val result = useCase.edit(ENTRY_ID, DetailEntryEdit.ToggleFavorite)

        val updated = (result as AppResult.Success<Entry>).data
        assertEquals(true, updated.favorite)
        assertEquals(listOf(3, 4), command.expectedVersions)
    }

    private class FakeQueryRepository(initial: Entry?) : EntryQueryRepository {
        var current: Entry? = initial

        override suspend fun getById(entryId: EntryId): Entry? = current

        override suspend fun findEntriesWithCustomIcons(): List<Entry> =
            listOfNotNull(current?.takeIf { it.icon.customReference != null })


        override suspend fun count(): Int = if (current == null) 0 else 1
    }

    private class FakeCommandRepository(
        private val query: FakeQueryRepository,
    ) : EntryCommandRepository {
        val failures = ArrayDeque<com.aozijx.passly.core.error.model.AppError>()
        val expectedVersions = mutableListOf<Int>()
        val submittedChanges = mutableListOf<EntryUpdate>()
        var onFailure: (() -> Unit)? = null
        var beforeFirstSuccess: (suspend () -> Unit)? = null

        override suspend fun updateEntry(
            id: EntryId,
            expectedVersion: EntryVersion,
            changes: EntryUpdate,
        ): AppResult<Unit> {
            expectedVersions += expectedVersion.value
            submittedChanges += changes
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
        favorite: Boolean = true,
        primaryUrl: String? = null,
        applicationIds: Set<String> = emptySet(),
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
            associations = EntryAssociations(
                primaryUrl = primaryUrl,
                applicationIds = applicationIds,
            ),
            favorite = favorite,
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
