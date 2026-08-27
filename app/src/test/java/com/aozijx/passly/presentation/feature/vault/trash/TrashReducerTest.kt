package com.aozijx.passly.presentation.feature.vault.trash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TrashReducerTest {
    @Test
    fun `entry operation clears error and owns busy entry`() {
        val started = TrashReducer.reduce(TrashUiState(error = "old"), TrashMutation.EntryActionStarted("entry-1"))
        val finished = TrashReducer.reduce(started, TrashMutation.EntryActionFinished)

        assertEquals("entry-1", started.activeEntryId)
        assertNull(started.error)
        assertNull(finished.activeEntryId)
    }

    @Test
    fun `load failure keeps current entries`() {
        val result = TrashReducer.reduce(TrashUiState(isLoading = true), TrashMutation.LoadFailed("failed"))

        assertFalse(result.isLoading)
        assertEquals("failed", result.error)
    }
}
