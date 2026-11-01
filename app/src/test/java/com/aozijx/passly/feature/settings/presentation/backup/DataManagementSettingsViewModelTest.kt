package com.aozijx.passly.feature.settings.presentation.backup

import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import com.aozijx.passly.feature.settings.backup.BackupDirectoryAccess
import com.aozijx.passly.feature.settings.backup.BackupDirectoryGrantResult
import com.aozijx.passly.feature.settings.backup.BackupDirectorySettingsInteractor
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataManagementSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `remains loading until persisted backup preferences arrive`() = runTest {
        val viewModel = DataManagementSettingsViewModel(
            directorySource = EmptyRepository,
            directorySettings = BackupDirectorySettingsInteractor(EmptyRepository, NoOpAccess),
        )

        assertEquals(DataManagementSettingsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `late directory result from an exited page is ignored`() = runTest {
        val repository = RecordingRepository()
        val access = RecordingAccess()
        val viewModel = DataManagementSettingsViewModel(
            directorySource = repository,
            directorySettings = BackupDirectorySettingsInteractor(repository, access),
        )
        val oldOwner = viewModel.onPageEntered()
        val requestId = viewModel.beginDirectorySelection(oldOwner)
        assertNotNull(requestId)

        viewModel.onPageExited(oldOwner)
        viewModel.onPageEntered()
        viewModel.onAction(
            DataManagementSettingsUiAction.BackupDirectoryPicked(
                uri = "content://stale",
                pageOwner = oldOwner,
                requestId = requireNotNull(requestId),
            ),
        )
        runCurrent()

        assertEquals(0, access.acquired)
        assertEquals(null, repository.savedUri)
    }

    private object EmptyRepository : BackupDirectorySettingsRepository {
        override val backupDirectoryUri = emptyFlow<String?>()
        override suspend fun setBackupDirectoryUri(uri: String) = Unit
        override suspend fun clearBackupDirectoryUri() = Unit
    }

    private object NoOpAccess : BackupDirectoryAccess {
        override suspend fun acquire(treeUri: String) = BackupDirectoryGrantResult.Unavailable
        override suspend fun release(treeUri: String) = Unit
    }

    private class RecordingRepository : BackupDirectorySettingsRepository {
        val uri = MutableStateFlow<String?>(null)
        var savedUri: String? = null
        override val backupDirectoryUri = uri
        override suspend fun setBackupDirectoryUri(uri: String) { savedUri = uri }
        override suspend fun clearBackupDirectoryUri() { savedUri = null }
    }

    private class RecordingAccess : BackupDirectoryAccess {
        var acquired = 0
        override suspend fun acquire(treeUri: String): BackupDirectoryGrantResult {
            acquired++
            return BackupDirectoryGrantResult.Granted(treeUri)
        }
        override suspend fun release(treeUri: String) = Unit
    }
}
