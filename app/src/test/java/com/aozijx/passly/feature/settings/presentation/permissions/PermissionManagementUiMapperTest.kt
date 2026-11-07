package com.aozijx.passly.feature.settings.presentation.permissions

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
    fun `all requested runtime permissions are listed with their access state`() {
        val state =
            PermissionManagementUiMapper.map(
                runtimeAccess =
                    mapOf(
                        RuntimePermission.CAMERA to RuntimePermissionAccess.REQUESTABLE,
                        RuntimePermission.POST_NOTIFICATIONS to RuntimePermissionAccess.NOT_APPLICABLE,
                    ),
                directoryStatus = PersistedDirectoryAccessStatus.NOT_CONFIGURED,
            )

        assertEquals(RuntimePermission.entries, state.runtimePermissions.map { it.permission })
        assertEquals(
            listOf(RuntimePermissionAccess.REQUESTABLE, RuntimePermissionAccess.NOT_APPLICABLE),
            state.runtimePermissions.map { it.access },
        )
    }

    @Test
    fun `directory states remain informational`() {
        PersistedDirectoryAccessStatus.entries.forEach { status ->
            val state =
                PermissionManagementUiMapper.map(
                    runtimeAccess = emptyMap(),
                    directoryStatus = status,
                )

            assertEquals(status, state.backupDirectory.status)
        }
    }

    @Test
    fun `manifest capabilities are compact informational rows`() {
        val state =
            PermissionManagementUiMapper.map(
                runtimeAccess = emptyMap(),
                directoryStatus = PersistedDirectoryAccessStatus.NOT_CONFIGURED,
            )

        assertEquals(ManifestCapability.entries, state.manifestCapabilities.map { it.capability })
        assertEquals(4, state.manifestCapabilities.size)
        assertFalse(state.manifestCapabilities.any { it.interactive })
    }
}
