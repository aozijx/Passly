package com.aozijx.passly.presentation.feature.settings.permissions

import com.aozijx.passly.core.permission.catalog.ManifestCapability
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionManagementUiMapperTest {
    @Test
    fun `runtime rows cover camera and notifications with exact actions`() {
        val expectedActions = mapOf(
            RuntimePermissionAccess.GRANTED to RuntimePermissionUiAction.OPEN_SETTINGS,
            RuntimePermissionAccess.REQUESTABLE to RuntimePermissionUiAction.REQUEST,
            RuntimePermissionAccess.SETTINGS_REQUIRED to RuntimePermissionUiAction.OPEN_SETTINGS,
            RuntimePermissionAccess.NOT_APPLICABLE to RuntimePermissionUiAction.NONE,
        )

        expectedActions.forEach { (access, expectedAction) ->
            val state = PermissionManagementUiMapper.map(
                runtimeAccess = RuntimePermission.entries.associateWith { access },
                directoryUri = null,
                directoryStatus = PersistedDirectoryAccessStatus.NOT_CONFIGURED,
            )

            assertEquals(RuntimePermission.entries.toList(), state.runtimePermissions.map { it.permission })
            assertTrue(state.runtimePermissions.all { it.access == access })
            assertTrue(state.runtimePermissions.all { it.action == expectedAction })
        }
    }

    @Test
    fun `directory states expose select change and recovery actions`() {
        val expected = mapOf(
            PersistedDirectoryAccessStatus.NOT_CONFIGURED to
                (BackupDirectoryUiAction.SELECT to false),
            PersistedDirectoryAccessStatus.GRANTED to
                (BackupDirectoryUiAction.CHANGE to true),
            PersistedDirectoryAccessStatus.GRANT_MISSING to
                (BackupDirectoryUiAction.REAUTHORIZE to true),
            PersistedDirectoryAccessStatus.UNAVAILABLE to
                (BackupDirectoryUiAction.REAUTHORIZE to true),
        )

        expected.forEach { (status, expectation) ->
            val state = PermissionManagementUiMapper.map(
                runtimeAccess = emptyMap(),
                directoryUri = "content://tree/backup".takeUnless {
                    status == PersistedDirectoryAccessStatus.NOT_CONFIGURED
                },
                directoryStatus = status,
            )

            assertEquals(status, state.backupDirectory.status)
            assertEquals(expectation.first, state.backupDirectory.action)
            assertEquals(expectation.second, state.backupDirectory.canClear)
        }
    }

    @Test
    fun `manifest capabilities are compact informational rows`() {
        val state = PermissionManagementUiMapper.map(
            runtimeAccess = emptyMap(),
            directoryUri = null,
            directoryStatus = PersistedDirectoryAccessStatus.NOT_CONFIGURED,
        )

        assertEquals(ManifestCapability.entries, state.manifestCapabilities.map { it.capability })
        assertEquals(4, state.manifestCapabilities.size)
        assertFalse(state.manifestCapabilities.any { it.interactive })
    }
}
