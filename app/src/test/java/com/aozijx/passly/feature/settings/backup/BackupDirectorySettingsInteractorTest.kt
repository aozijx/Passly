package com.aozijx.passly.feature.settings.backup

import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupDirectorySettingsInteractorTest {
    @Test
    fun `inspection delegates without changing repository state`() = runTest {
        val repository = FakeRepository(initialUri = "content://tree/original")
        val access = FakeAccess(
            acquireResult = BackupDirectoryGrantResult.Unavailable,
            inspectResult = PersistedDirectoryAccessStatus.GRANT_MISSING,
        )
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        assertEquals(
            PersistedDirectoryAccessStatus.GRANT_MISSING,
            interactor.inspect("content://tree/original"),
        )
        assertEquals("content://tree/original", repository.savedUri)
        assertEquals(listOf("content://tree/original"), access.inspectedUris)
    }

    @Test
    fun `selection persists only a granted original tree uri`() = runTest {
        val repository = FakeRepository()
        val access = FakeAccess(
            acquireResult = BackupDirectoryGrantResult.Granted("content://tree/original"),
        )
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        assertEquals(
            BackupDirectorySelectionResult.Saved,
            interactor.select("content://tree/original"),
        )
        assertEquals("content://tree/original", repository.savedUri)
        assertEquals(listOf("content://tree/original"), access.acquiredUris)
    }

    @Test
    fun `cancelled or denied selection never changes settings`() = runTest {
        val repository = FakeRepository()
        val access = FakeAccess(BackupDirectoryGrantResult.PermissionDenied)
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        assertEquals(BackupDirectorySelectionResult.Cancelled, interactor.select(null))
        assertEquals(
            BackupDirectorySelectionResult.PermissionDenied,
            interactor.select("content://tree/denied"),
        )
        assertNull(repository.savedUri)
    }

    @Test
    fun `clear clears settings before releasing the persisted grant`() = runTest {
        val operations = mutableListOf<String>()
        val repository = FakeRepository(operations = operations)
        val access = FakeAccess(
            acquireResult = BackupDirectoryGrantResult.Unavailable,
            operations = operations,
        )
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        interactor.clear("content://tree/original")

        assertEquals(
            listOf("clear", "release:content://tree/original"),
            operations,
        )
    }

    @Test
    fun `failed grant release does not restore cleared directory setting`() = runTest {
        val operations = mutableListOf<String>()
        val repository = FakeRepository(
            initialUri = "content://tree/original",
            operations = operations,
        )
        val access = FakeAccess(
            acquireResult = BackupDirectoryGrantResult.Unavailable,
            operations = operations,
            failOnReleaseUri = "content://tree/original",
        )
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        interactor.clear("content://tree/original")

        assertNull(repository.savedUri)
        assertEquals(
            listOf("clear", "release:content://tree/original"),
            operations,
        )
    }

    @Test
    fun `cancellation after clear commit still releases the persisted grant`() = runTest {
        val operations = mutableListOf<String>()
        val repository = FakeRepository(
            initialUri = "content://tree/original",
            operations = operations,
            cancelAfterClear = true,
        )
        val access = FakeAccess(
            acquireResult = BackupDirectoryGrantResult.Unavailable,
            operations = operations,
        )
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        val cancellation = try {
            interactor.clear("content://tree/original")
            null
        } catch (error: CancellationException) {
            error
        }

        assertEquals("settings cleared before cancellation", cancellation?.message)
        assertNull(repository.savedUri)
        assertEquals(
            listOf("clear", "release:content://tree/original"),
            operations,
        )
    }

    @Test
    fun `failed clear keeps grant when settings still reference it`() = runTest {
        val operations = mutableListOf<String>()
        val repository = FakeRepository(
            initialUri = "content://tree/original",
            operations = operations,
            failOnClear = true,
        )
        val access = FakeAccess(
            acquireResult = BackupDirectoryGrantResult.Unavailable,
            operations = operations,
        )
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        val failure = try {
            interactor.clear("content://tree/original")
            null
        } catch (error: IllegalStateException) {
            error
        }

        assertEquals("settings clear failed", failure?.message)
        assertEquals("content://tree/original", repository.savedUri)
        assertEquals(listOf("clear"), operations)
    }

    @Test
    fun `replacing directory saves new uri before releasing previous grant`() = runTest {
        val operations = mutableListOf<String>()
        val repository = FakeRepository(
            initialUri = "content://tree/previous",
            operations = operations,
        )
        val access = FakeAccess(
            acquireResult = BackupDirectoryGrantResult.Granted("content://tree/new"),
            operations = operations,
        )
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        assertEquals(
            BackupDirectorySelectionResult.Saved,
            interactor.select("content://tree/new"),
        )

        assertEquals(
            listOf(
                "acquire:content://tree/new",
                "save:content://tree/new",
                "release:content://tree/previous",
            ),
            operations,
        )
    }

    @Test
    fun `failed settings write rolls back newly acquired grant`() = runTest {
        val operations = mutableListOf<String>()
        val repository = FakeRepository(
            initialUri = "content://tree/previous",
            operations = operations,
            failOnSave = true,
        )
        val access = FakeAccess(
            acquireResult = BackupDirectoryGrantResult.Granted("content://tree/new"),
            operations = operations,
        )
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        assertEquals(
            BackupDirectorySelectionResult.Unavailable,
            interactor.select("content://tree/new"),
        )

        assertEquals(
            listOf(
                "acquire:content://tree/new",
                "save:content://tree/new",
                "release:content://tree/new",
            ),
            operations,
        )
    }

    @Test
    fun `cancellation after settings commit retains the newly referenced grant`() = runTest {
        val operations = mutableListOf<String>()
        val repository = FakeRepository(
            initialUri = "content://tree/previous",
            operations = operations,
            cancelAfterSave = true,
        )
        val access = FakeAccess(
            acquireResult = BackupDirectoryGrantResult.Granted("content://tree/new"),
            operations = operations,
        )
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        val cancellation = try {
            interactor.select("content://tree/new")
            null
        } catch (error: CancellationException) {
            error
        }

        assertEquals("settings committed before cancellation", cancellation?.message)
        assertEquals("content://tree/new", repository.savedUri)
        assertEquals(
            listOf("acquire:content://tree/new", "save:content://tree/new"),
            operations,
        )
    }

    @Test
    fun `failure releasing previous grant does not revoke newly saved directory`() = runTest {
        val operations = mutableListOf<String>()
        val repository = FakeRepository(
            initialUri = "content://tree/previous",
            operations = operations,
        )
        val access = FakeAccess(
            acquireResult = BackupDirectoryGrantResult.Granted("content://tree/new"),
            operations = operations,
            failOnReleaseUri = "content://tree/previous",
        )
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        assertEquals(
            BackupDirectorySelectionResult.Saved,
            interactor.select("content://tree/new"),
        )

        assertEquals("content://tree/new", repository.savedUri)
        assertEquals(
            listOf(
                "acquire:content://tree/new",
                "save:content://tree/new",
                "release:content://tree/previous",
            ),
            operations,
        )
    }

    private class FakeRepository(
        initialUri: String? = null,
        private val operations: MutableList<String> = mutableListOf(),
        private val failOnSave: Boolean = false,
        private val cancelAfterSave: Boolean = false,
        private val cancelAfterClear: Boolean = false,
        private val failOnClear: Boolean = false,
    ) : BackupDirectorySettingsRepository {
        private val state = MutableStateFlow(initialUri)
        override val backupDirectoryUri: Flow<String?> = state
        var savedUri: String? = initialUri
            private set

        override suspend fun setBackupDirectoryUri(uri: String) {
            operations += "save:$uri"
            if (failOnSave) error("settings write failed")
            savedUri = uri
            state.value = uri
            if (cancelAfterSave) throw CancellationException("settings committed before cancellation")
        }

        override suspend fun clearBackupDirectoryUri() {
            operations += "clear"
            if (failOnClear) error("settings clear failed")
            savedUri = null
            state.value = null
            if (cancelAfterClear) throw CancellationException("settings cleared before cancellation")
        }
    }

    private class FakeAccess(
        private val acquireResult: BackupDirectoryGrantResult,
        private val operations: MutableList<String> = mutableListOf(),
        private val inspectResult: PersistedDirectoryAccessStatus =
            PersistedDirectoryAccessStatus.GRANTED,
        private val failOnReleaseUri: String? = null,
    ) : BackupDirectoryAccess {
        val acquiredUris = mutableListOf<String>()
        val inspectedUris = mutableListOf<String?>()

        override fun inspect(treeUri: String?): PersistedDirectoryAccessStatus {
            inspectedUris += treeUri
            return inspectResult
        }

        override suspend fun acquire(treeUri: String): BackupDirectoryGrantResult {
            operations += "acquire:$treeUri"
            acquiredUris += treeUri
            return acquireResult
        }

        override suspend fun release(treeUri: String) {
            operations += "release:$treeUri"
            if (treeUri == failOnReleaseUri) error("grant release failed")
        }
    }
}
