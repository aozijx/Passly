package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.policy.EntryDraftMaterializer
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImportScannedOtpUseCaseTest {
    @Test
    fun `import is acknowledged only after repository confirms storage`() = runTest {
        val stored = CompletableDeferred<AppResult<EntryId>>()
        val repository = Repository { stored.await() }
        val result = async { importer(repository)(OtpConfig(secret = "MY")) }
        runCurrent()
        assertEquals(1, repository.calls)
        assertFalse(result.isCompleted)
        stored.complete(AppResult.Success(EntryId("saved")))
        assertTrue(result.await())
    }

    @Test
    fun `failed writes and unexpected storage errors are rejected`() = runTest {
        assertFalse(importer(Repository { AppResult.Failure(ValidationError()) })(OtpConfig(secret = "MY")))
        assertFalse(importer(Repository { error("storage unavailable") })(OtpConfig(secret = "MY")))
    }

    @Test
    fun `locked session and unavailable secret never reach persistence`() = runTest {
        val repository = Repository { AppResult.Success(EntryId("saved")) }
        assertFalse(importer(repository, AuthenticationState.Locked)(OtpConfig(secret = "MY")))
        assertFalse(importer(repository)(OtpConfig(secret = null)))
        assertEquals(0, repository.calls)
    }

    @Test
    fun `storage cancellation propagates without a rejection result`() = runTest {
        val result = async {
            importer(Repository { throw CancellationException("cancelled") })(OtpConfig(secret = "MY"))
        }
        try {
            result.await()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            assertTrue(result.isCancelled)
        }
    }

    private fun importer(
        repository: EntryCommandRepository,
        authentication: AuthenticationState = AuthenticationState.Authenticated(1L),
    ): ImportScannedOtpUseCase {
        val session = object : SecureSessionAccessState {
            override val authenticationState = MutableStateFlow(authentication)
            override fun isUnlocked() = authentication is AuthenticationState.Authenticated
        }
        return ImportScannedOtpUseCase(
            CreateEntryUseCase(repository, session, EntryDraftMaterializer(), testLogger()),
            session,
            testLogger(),
        )
    }

    private class Repository(val create: suspend () -> AppResult<EntryId>) : EntryCommandRepository {
        var calls = 0
        override suspend fun createEntry(entry: Entry): AppResult<EntryId> { calls++; return create() }
        override suspend fun updateEntry(id: EntryId, expectedVersion: EntryVersion, changes: EntryUpdate) = AppResult.Success(Unit)
        override suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)
    }
}
