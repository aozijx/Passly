package com.aozijx.passly.feature.settings.backup

import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupDirectorySettingsInteractorTest {
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
    fun `clear releases the persisted grant before clearing settings`() = runTest {
        val operations = mutableListOf<String>()
        val repository = FakeRepository(operations = operations)
        val access = FakeAccess(
            acquireResult = BackupDirectoryGrantResult.Unavailable,
            operations = operations,
        )
        val interactor = BackupDirectorySettingsInteractor(repository, access)

        interactor.clear("content://tree/original")

        assertEquals(
            listOf("release:content://tree/original", "clear"),
            operations,
        )
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

    private class FakeRepository(
        initialUri: String? = null,
        private val operations: MutableList<String> = mutableListOf(),
        private val failOnSave: Boolean = false,
    ) : BackupDirectorySettingsRepository {
        override val backupDirectoryUri: Flow<String?> = MutableStateFlow(initialUri)
        var savedUri: String? = initialUri
            private set

        override suspend fun setBackupDirectoryUri(uri: String) {
            operations += "save:$uri"
            if (failOnSave) error("settings write failed")
            savedUri = uri
        }

        override suspend fun clearBackupDirectoryUri() {
            savedUri = null
            operations += "clear"
        }
    }

    private class FakeAccess(
        private val acquireResult: BackupDirectoryGrantResult,
        private val operations: MutableList<String> = mutableListOf(),
    ) : BackupDirectoryAccess {
        val acquiredUris = mutableListOf<String>()

        override suspend fun acquire(treeUri: String): BackupDirectoryGrantResult {
            operations += "acquire:$treeUri"
            acquiredUris += treeUri
            return acquireResult
        }

        override suspend fun release(treeUri: String) {
            operations += "release:$treeUri"
        }
    }
}
