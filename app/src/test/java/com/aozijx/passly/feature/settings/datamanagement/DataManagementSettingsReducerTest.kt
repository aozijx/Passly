package com.aozijx.passly.feature.settings.datamanagement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataManagementSettingsReducerTest {

    @Test
    fun `settings updates preserve trash workflow`() {
        val result = DataManagementSettingsReducer.reduce(
            DataManagementSettingsUiState(isEmptyingTrash = true),
            DataManagementSettingsMutation.SettingsChanged(
                autoDownloadIcons = false,
                directoryUri = "content://backup",
            ),
        )

        assertFalse(result.isAutoDownloadIcons)
        assertEquals("content://backup", result.directoryUri)
        assertTrue(result.isEmptyingTrash)
    }

    @Test
    fun `entry operation clears error and owns busy entry`() {
        val started = DataManagementSettingsReducer.reduce(
            DataManagementSettingsUiState(trashError = "old"),
            DataManagementSettingsMutation.TrashEntryActionStarted("entry-1"),
        )
        val finished = DataManagementSettingsReducer.reduce(
            started,
            DataManagementSettingsMutation.TrashEntryActionFinished,
        )

        assertEquals("entry-1", started.activeTrashEntryId)
        assertNull(started.trashError)
        assertNull(finished.activeTrashEntryId)
    }

    @Test
    fun `trash failure keeps current entries`() {
        val result = DataManagementSettingsReducer.reduce(
            DataManagementSettingsUiState(isTrashLoading = true),
            DataManagementSettingsMutation.TrashLoadFailed("failed"),
        )

        assertFalse(result.isTrashLoading)
        assertEquals("failed", result.trashError)
    }
}
