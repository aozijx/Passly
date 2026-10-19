package com.aozijx.passly.feature.autofill.core

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.domain.autofill.model.AutofillSaveOutcome
import com.aozijx.passly.domain.autofill.port.ApplicationLabelResolver
import com.aozijx.passly.domain.autofill.port.AutofillEntryWriter
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.entry.EntryDraftMaterializer
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveAutofillCredentialTest {
    @Test
    fun blankPasswordIsRejectedBeforePersistence() = runBlocking {
        val fixture = fixture()

        val outcome = fixture.operation(
            draft(password = "  "),
        )

        assertEquals(AutofillSaveOutcome.InvalidDraft, outcome)
        assertEquals(0, fixture.repository.createCalls)
    }

    @Test
    fun passwordOnlyDraftCreatesTypedLoginCredential() = runBlocking {
        val fixture = fixture()

        val outcome = fixture.operation(
            draft(username = "", password = "secret"),
        )

        assertEquals(AutofillSaveOutcome.Saved(EntryId("saved-entry")), outcome)
        val entry = requireNotNull(fixture.repository.createdEntry)
        assertEquals(EntryType.LOGIN, entry.type)
        assertEquals("", entry.username)
        assertTrue(entry.secret.credential is LoginCredential)
        assertEquals("secret", (entry.secret.credential as LoginCredential).password)
    }

    private fun fixture(): Fixture {
        val repository = RecordingEntryCommandRepository()
        val session = UnlockedSession()
        val createEntry = CreateEntryUseCase(
            entryCommandRepository = repository,
            secureSessionAccessState = session,
            materializer = EntryDraftMaterializer(),
            autofillEntryWriter = repository,
            logger = testLogger(),
            idFactory = { EntryId("saved-entry") },
            clock = { 1L },
            monotonicClock = { 1L },
        )
        return Fixture(
            operation = SaveAutofillCredential(
                applicationLabelResolver = ApplicationLabelResolver { "Example" },
                createEntry = createEntry,
            ),
            repository = repository,
        )
    }

    private fun draft(
        username: String = "alice",
        password: String,
    ) = AutofillSaveDraft(
        packageName = "com.example.app",
        webDomain = "example.com",
        pageTitle = "Example",
        username = username,
        password = password,
    )

    private data class Fixture(
        val operation: SaveAutofillCredential,
        val repository: RecordingEntryCommandRepository,
    )

    private class UnlockedSession : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.AutofillAuthenticated(1L))

        override fun isUnlocked(): Boolean = true
    }

    private class RecordingEntryCommandRepository : EntryCommandRepository, AutofillEntryWriter {
        var createCalls = 0
        var createdEntry: Entry? = null

        override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
            createCalls++
            createdEntry = entry
            return AppResult.Success(EntryId("saved-entry"))
        }

        override suspend fun create(entry: Entry): AppResult<EntryId> = createEntry(entry)

        override suspend fun updateEntry(id: EntryId, expectedVersion: EntryVersion, changes: EntryUpdate) =
            AppResult.Success(Unit)

        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)
    }
}
