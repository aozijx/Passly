package com.aozijx.passly.feature.settings.presentation.backup

import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import com.aozijx.passly.feature.settings.backup.BackupDirectoryAccess
import com.aozijx.passly.feature.settings.backup.BackupDirectoryGrantResult
import com.aozijx.passly.feature.settings.backup.BackupDirectorySettingsInteractor
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
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
    fun `directory read failure is visible and retry re-subscribes`() =
        runTest {
            var starts = 0
            val repository =
                object : BackupDirectorySettingsRepository {
                    override val backupDirectoryUri: Flow<String?> =
                        flow {
                            starts++
                            if (starts == 1) throw IOException("read failure")
                            emit(null)
                            awaitCancellation()
                        }

                    override suspend fun setBackupDirectoryUri(uri: String) = Unit

                    override suspend fun clearBackupDirectoryUri() = Unit
                }
            val viewModel =
                DataManagementSettingsViewModel(
                    directorySource = repository,
                    directorySettings = BackupDirectorySettingsInteractor(repository, NoOpAccess),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            assertEquals(true, viewModel.uiState.value is DataManagementSettingsUiState.Failed)
            viewModel.onAction(DataManagementSettingsUiAction.RetryRead)
            runCurrent()

            assertEquals(2, starts)
            assertEquals(DataManagementSettingsUiState.Ready(null), viewModel.uiState.value)
        }

    @Test
    fun `remains loading until persisted backup preferences arrive`() =
        runTest {
            val viewModel =
                DataManagementSettingsViewModel(
                    directorySource = EmptyRepository,
                    directorySettings = BackupDirectorySettingsInteractor(EmptyRepository, NoOpAccess),
                )

            assertEquals(DataManagementSettingsUiState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `late directory result from an exited page is ignored`() =
        runTest {
            val repository = RecordingRepository()
            val access = RecordingAccess()
            val viewModel =
                DataManagementSettingsViewModel(
                    directorySource = repository,
                    directorySettings = BackupDirectorySettingsInteractor(repository, access),
                )
            val oldOwner = viewModel.onPageEntered()
            runCurrent()
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
        override val backupDirectoryUri = flow<String?> { awaitCancellation() }

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

        override suspend fun setBackupDirectoryUri(uri: String) {
            savedUri = uri
        }

        override suspend fun clearBackupDirectoryUri() {
            savedUri = null
        }
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
