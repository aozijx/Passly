package com.aozijx.passly.presentation.feature.settings.backup

import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import com.aozijx.passly.feature.settings.backup.BackupDirectoryAccess
import com.aozijx.passly.feature.settings.backup.BackupDirectoryGrantResult
import com.aozijx.passly.feature.settings.backup.BackupDirectorySettingsInteractor
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DataManagementSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `remains loading until persisted backup preferences arrive`() = runTest {
        val viewModel = DataManagementSettingsViewModel(
            BackupDirectorySettingsInteractor(EmptyRepository, NoOpAccess),
        )

        assertEquals(DataManagementSettingsUiState.Loading, viewModel.uiState.value)
    }

    private object EmptyRepository : BackupDirectorySettingsRepository {
        override val backupDirectoryUri = emptyFlow<String?>()
        override suspend fun setBackupDirectoryUri(uri: String) = Unit
        override suspend fun clearBackupDirectoryUri() = Unit
    }

    private object NoOpAccess : BackupDirectoryAccess {
        override fun inspect(treeUri: String?) = PersistedDirectoryAccessStatus.GRANT_MISSING
        override suspend fun acquire(treeUri: String) = BackupDirectoryGrantResult.Unavailable
        override suspend fun release(treeUri: String) = Unit
    }
}
