package com.aozijx.passly.feature.settings.backup

import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupDirectoryStatusInteractorTest {
    @Test
    fun `exposes configured directory and its inspected access status`() = runTest {
        val configuredUri = "content://provider/tree/backup"
        val interactor = BackupDirectoryStatusInteractor(
            settingsSource = FakeSettingsSource(configuredUri),
            statusReader = BackupDirectoryStatusReader { uri ->
                if (uri == configuredUri) PersistedDirectoryAccessStatus.GRANTED
                else PersistedDirectoryAccessStatus.NOT_CONFIGURED
            },
        )

        val uri = interactor.directoryUri.first()

        assertEquals(configuredUri, uri)
        assertEquals(PersistedDirectoryAccessStatus.GRANTED, interactor.inspect(uri))
    }

    private class FakeSettingsSource(initialUri: String?) : BackupDirectorySettingsSource {
        override val backupDirectoryUri: Flow<String?> = MutableStateFlow(initialUri)
    }
}
