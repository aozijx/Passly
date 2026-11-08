package com.aozijx.passly.feature.settings.presentation.permissions

import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsSource
import com.aozijx.passly.feature.settings.backup.BackupDirectoryStatusReader
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionManagementViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `failed backup directory read is visible and retry re-subscribes`() =
        runTest {
            var starts = 0
            val directoryInspectionCompleted = CompletableDeferred<Unit>()
            val source =
                object : BackupDirectorySettingsSource {
                    override val backupDirectoryUri =
                        flow {
                            starts++
                            if (starts == 1) throw IOException("read failure")
                            emit(null)
                            awaitCancellation()
                        }
                }
            val viewModel =
                PermissionManagementViewModel(
                    directorySettings = source,
                    directoryStatusReader =
                        BackupDirectoryStatusReader {
                            directoryInspectionCompleted.complete(Unit)
                            PersistedDirectoryAccessStatus.NOT_CONFIGURED
                        },
                )
            runCurrent()

            assertEquals(
                RecoverableReadState.Failed(ReadFailure.IO),
                viewModel.uiState.value.backupDirectoryReadState,
            )
            assertEquals(PersistedDirectoryAccessStatus.UNAVAILABLE, viewModel.uiState.value.backupDirectory.status)
            viewModel.retryBackupDirectoryRead()
            runCurrent()
            directoryInspectionCompleted.await()
            viewModel.uiState.first { it.backupDirectoryReadState is RecoverableReadState.Ready }

            assertEquals(2, starts)
            assertEquals(
                RecoverableReadState.Ready(null),
                viewModel.uiState.value.backupDirectoryReadState,
            )
        }
}
