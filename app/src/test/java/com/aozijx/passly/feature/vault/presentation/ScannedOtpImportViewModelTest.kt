package com.aozijx.passly.feature.vault.presentation

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.policy.EntryDraftMaterializer
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.feature.scanner.presentation.ScannerImportHandlingResult
import com.aozijx.passly.feature.scanner.presentation.ScannerImportOutcome
import com.aozijx.passly.feature.scanner.presentation.ScannerImportSession
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.testing.testLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannedOtpImportViewModelTest {
    @Test
    fun confirmedWriteContinuesWhenScannerCallerLeaves() = runTest {
        val access = MutableAccess()
        val started = CompletableDeferred<Unit>()
        val persistence = CompletableDeferred<AppResult<EntryId>>()
        var writes = 0
        var persisted = false
        val repository = object : EntryCommandRepository by RepositoryDefaults {
            override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                writes++
                started.complete(Unit)
                return persistence.await().also { persisted = it is AppResult.Success }
            }
        }
        val viewModel = model(repository, access, this)
        val confirmation = launch { viewModel.importScannedOtp(OtpConfig(secret = "MY")) }
        runCurrent()
        withTimeout(1_000) { started.await() }

        confirmation.cancelAndJoin()
        persistence.complete(AppResult.Success(EntryId("saved")))
        runCurrent()

        assertEquals(1, writes)
        assertTrue(persisted)
    }

    @Test
    fun confirmedWriteUsesTheResultEvenIfAccessIsRevokedAfterPersistenceStarts() = runTest {
        val access = MutableAccess()
        val persistence = CompletableDeferred<AppResult<EntryId>>()
        val started = CompletableDeferred<Unit>()
        val repository = object : EntryCommandRepository by RepositoryDefaults {
            override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                started.complete(Unit)
                return persistence.await()
            }
        }
        val viewModel = model(repository, access, this)
        var result: Boolean? = null
        val confirmation = launch { result = viewModel.importScannedOtp(OtpConfig(secret = "MY")) }
        runCurrent()
        withTimeout(1_000) { started.await() }
        access.authenticationState.value = AuthenticationState.Locked
        persistence.complete(AppResult.Success(EntryId("saved")))
        runCurrent()

        assertTrue(confirmation.isCompleted)
        assertEquals(true, result)
    }

    @Test
    fun scannerWaitsForPersistenceAndReceivesFailure() = runTest {
        val persistence = CompletableDeferred<AppResult<EntryId>>()
        var writes = 0
        val repository = object : EntryCommandRepository by RepositoryDefaults {
            override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                writes++
                return persistence.await()
            }
        }
        val viewModel = model(repository, MutableAccess(), this)
        var completed: Boolean? = null
        val confirmation = launch {
            completed = viewModel.importScannedOtp(OtpConfig(secret = "MY"))
        }
        runCurrent()

        assertEquals(1, writes)
        assertFalse(confirmation.isCompleted)
        persistence.complete(AppResult.Failure(ValidationError()))
        runCurrent()

        assertEquals(false, completed)
    }

    @Test
    fun concurrentConfirmationAfterScannerRecreationCannotStartAnotherWrite() = runTest {
        val persistence = CompletableDeferred<AppResult<EntryId>>()
        var writes = 0
        val repository = object : EntryCommandRepository by RepositoryDefaults {
            override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                writes++
                return persistence.await()
            }
        }
        val viewModel = model(repository, MutableAccess(), this)
        val firstSession = ScannerImportSession()
        val first = async {
            firstSession.confirm {
                if (viewModel.importScannedOtp(OtpConfig(secret = "MY"))) {
                    ScannerImportHandlingResult.Applied
                } else {
                    ScannerImportHandlingResult.Rejected
                }
            }
        }
        runCurrent()
        firstSession.close()
        val secondSession = ScannerImportSession()
        val secondResult = secondSession.confirm {
            if (viewModel.importScannedOtp(OtpConfig(secret = "MY"))) {
                ScannerImportHandlingResult.Applied
            } else {
                ScannerImportHandlingResult.Rejected
            }
        }
        runCurrent()

        assertEquals(1, writes)
        assertEquals(ScannerImportOutcome.Rejected, secondResult)
        persistence.complete(AppResult.Success(EntryId("saved")))
        runCurrent()
        assertEquals(ScannerImportOutcome.Ignored, first.await())
    }

    @Test
    fun changedAuthenticationSessionBeforeOwnedTaskStartsPreventsPersistence() = runTest {
        val access = MutableAccess()
        var writes = 0
        val repository = object : EntryCommandRepository by RepositoryDefaults {
            override suspend fun createEntry(entry: Entry): AppResult<EntryId> {
                writes++
                return AppResult.Success(entry.id)
            }
        }
        val viewModel = model(repository, access, this)
        val result = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            viewModel.importScannedOtp(OtpConfig(secret = "MY"))
        }
        access.authenticationState.value = AuthenticationState.Locked
        runCurrent()

        assertFalse(result.await())
        assertEquals(0, writes)
    }

    private fun model(
        repository: EntryCommandRepository,
        access: MutableAccess,
        ownerScope: CoroutineScope,
    ) = ScannedOtpImportViewModel(
        ImportScannedOtpUseCaseFixture.create(repository, access),
        access,
        ownerScope,
    )

    private class MutableAccess : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(
            AuthenticationState.Authenticated(1L),
        )

        override fun isUnlocked() = authenticationState.value is AuthenticationState.Authenticated
    }

    private object ImportScannedOtpUseCaseFixture {
        fun create(
            repository: EntryCommandRepository,
            access: MutableAccess,
        ) = com.aozijx.passly.feature.vault.entry.ImportScannedOtpUseCase(
            CreateEntryUseCase(repository, access, EntryDraftMaterializer(), testLogger()),
            access,
            testLogger(),
        )
    }

    private object RepositoryDefaults : EntryCommandRepository {
        override suspend fun createEntry(entry: Entry): AppResult<EntryId> = AppResult.Success(entry.id)
        override suspend fun updateEntry(
            id: EntryId,
            expectedVersion: com.aozijx.passly.domain.entry.model.EntryVersion,
            changes: com.aozijx.passly.domain.entry.model.EntryUpdate,
        ) = AppResult.Success(Unit)
        override suspend fun moveToTrash(
            id: EntryId,
            expectedVersion: com.aozijx.passly.domain.entry.model.EntryVersion,
        ) = AppResult.Success(Unit)
        override suspend fun restoreEntry(
            id: EntryId,
            expectedVersion: com.aozijx.passly.domain.entry.model.EntryVersion,
        ) = AppResult.Success(Unit)
        override suspend fun deletePermanently(
            id: EntryId,
            expectedVersion: com.aozijx.passly.domain.entry.model.EntryVersion,
        ) = AppResult.Success(Unit)
        override suspend fun emptyTrash() = AppResult.Success(0)
    }
}
