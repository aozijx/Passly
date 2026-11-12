package com.aozijx.passly.feature.settings.presentation.backup

import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import com.aozijx.passly.feature.settings.backup.BackupDirectoryAccess
import com.aozijx.passly.feature.settings.backup.BackupDirectoryGrantResult
import com.aozijx.passly.feature.settings.backup.BackupDirectorySettingsInteractor
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.RecordingTestLogger
import java.io.IOException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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

    private fun createInteractor(
        repository: BackupDirectorySettingsRepository,
        access: BackupDirectoryAccess,
    ) = BackupDirectorySettingsInteractor(repository, access, RecordingTestLogger())

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
                    directorySettings = createInteractor(repository, NoOpAccess),
                    logger = RecordingTestLogger(),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            assertEquals(
                RecoverableReadState.Failed(ReadFailure.IO),
                viewModel.uiState.value,
            )
            viewModel.onAction(DataManagementSettingsUiAction.RetryRead)
            runCurrent()

            assertEquals(2, starts)
            assertEquals(
                RecoverableReadState.Ready(BackupDirectoryUiState.Unconfigured),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `remains loading until persisted backup preferences arrive`() =
        runTest {
            val viewModel =
                DataManagementSettingsViewModel(
                    directorySource = EmptyRepository,
                    directorySettings = createInteractor(EmptyRepository, NoOpAccess),
                    logger = RecordingTestLogger(),
                )

            assertEquals(RecoverableReadState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `late directory result from an exited page is ignored`() =
        runTest {
            val repository = RecordingRepository()
            val access = RecordingAccess()
            val viewModel =
                DataManagementSettingsViewModel(
                    directorySource = repository,
                    directorySettings = createInteractor(repository, access),
                    logger = RecordingTestLogger(),
                )
            val oldOwner = viewModel.onPageEntered()
            backgroundScope.launch { viewModel.uiState.collect {} }
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

    @Test
    fun `selection exception becomes a page failure effect and retains current uri`() =
        runTest {
            var subscriptions = 0
            val repository =
                object : BackupDirectorySettingsRepository {
                    override val backupDirectoryUri: Flow<String?> =
                        flow {
                            subscriptions++
                            if (subscriptions == 2) throw IOException("settings read failed")
                            emit("content://tree/current")
                            awaitCancellation()
                        }

                    override suspend fun setBackupDirectoryUri(uri: String) = Unit

                    override suspend fun clearBackupDirectoryUri() = Unit
                }
            val access = RecordingAccess()
            val logger = RecordingTestLogger()
            val viewModel =
                DataManagementSettingsViewModel(
                    directorySource = repository,
                    directorySettings = createInteractor(repository, access),
                    logger = logger,
                )
            val owner = viewModel.onPageEntered()
            val effects = mutableListOf<DataManagementSettingsEffect>()
            backgroundScope.launch { viewModel.effects.collect(effects::add) }
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()
            assertEquals(
                RecoverableReadState.Ready(BackupDirectoryUiState.Configured("content://tree/current")),
                viewModel.uiState.value,
            )

            val requestId = viewModel.beginDirectorySelection(owner)
            viewModel.onAction(
                DataManagementSettingsUiAction.BackupDirectoryPicked(
                    uri = "content://tree/new",
                    pageOwner = owner,
                    requestId = requireNotNull(requestId),
                ),
            )
            runCurrent()

            assertEquals(listOf(DataManagementSettingsEffect.BackupDirectoryOperationFailed), effects)
            assertEquals(
                RecoverableReadState.Ready(BackupDirectoryUiState.Configured("content://tree/current")),
                viewModel.uiState.value,
            )
            assertEquals(0, access.acquired)
            assertEquals(1, logger.events.size)
            assertEquals(Events.Backup.Error, logger.events.single().event)
            assertEquals(Reasons.Application.Storage.StorageFailure, logger.events.single().reason)
            assertEquals(
                IOException::class.java,
                logger.events
                    .single()
                    .throwable
                    ?.javaClass,
            )
        }

    @Test
    fun `clear exception becomes a page failure effect and retains current uri`() =
        runTest {
            val uri = MutableStateFlow<String?>("content://tree/current")
            val repository =
                object : BackupDirectorySettingsRepository {
                    override val backupDirectoryUri: Flow<String?> = uri

                    override suspend fun setBackupDirectoryUri(uri: String) = Unit

                    override suspend fun clearBackupDirectoryUri(): Unit = throw IOException("settings clear failed")
                }
            val logger = RecordingTestLogger()
            val viewModel =
                DataManagementSettingsViewModel(
                    directorySource = repository,
                    directorySettings = createInteractor(repository, NoOpAccess),
                    logger = logger,
                )
            viewModel.onPageEntered()
            val effects = mutableListOf<DataManagementSettingsEffect>()
            backgroundScope.launch { viewModel.effects.collect(effects::add) }
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.onAction(DataManagementSettingsUiAction.ClearBackupDirectory)
            runCurrent()

            assertEquals(listOf(DataManagementSettingsEffect.BackupDirectoryOperationFailed), effects)
            assertEquals(
                RecoverableReadState.Ready(BackupDirectoryUiState.Configured("content://tree/current")),
                viewModel.uiState.value,
            )
            assertEquals(1, logger.events.size)
            assertEquals(Events.Backup.Error, logger.events.single().event)
            assertEquals(Reasons.Application.Storage.StorageFailure, logger.events.single().reason)
            assertEquals(
                IOException::class.java,
                logger.events
                    .single()
                    .throwable
                    ?.javaClass,
            )
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
