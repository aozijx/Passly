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
    fun `runtime permissions that do not apply to this Android version are omitted`() {
        val state = PermissionManagementUiMapper.map(
            runtimeAccess = mapOf(
                RuntimePermission.CAMERA to RuntimePermissionAccess.GRANTED,
                RuntimePermission.POST_NOTIFICATIONS to RuntimePermissionAccess.NOT_APPLICABLE,
            ),
            directoryUri = null,
            directoryStatus = PersistedDirectoryAccessStatus.NOT_CONFIGURED,
        )

        assertEquals(listOf(RuntimePermission.CAMERA), state.runtimePermissions.map { it.permission })
    }

    @Test
    fun `directory states remain informational`() {
        PersistedDirectoryAccessStatus.entries.forEach { status ->
            val state = PermissionManagementUiMapper.map(
                runtimeAccess = emptyMap(),
                directoryUri = "content://tree/backup".takeUnless {
                    status == PersistedDirectoryAccessStatus.NOT_CONFIGURED
                },
                directoryStatus = status,
            )

            assertEquals(status, state.backupDirectory.status)
        }
    }

    @Test
    fun `configured directory exposes a readable label without leaking the full uri`() {
        val state = PermissionManagementUiMapper.map(
            runtimeAccess = emptyMap(),
            directoryUri = "content://provider/tree/primary%3ABackups",
            directoryStatus = PersistedDirectoryAccessStatus.GRANTED,
        )

        assertEquals("Backups", state.backupDirectory.label)
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
