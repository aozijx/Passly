package com.aozijx.passly.presentation.feature.vault.trash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TrashReducerTest {
    @Test
    fun `entry operation clears error and owns busy entry`() {
        val started = TrashReducer.reduce(
            TrashUiState(operation = TrashOperationState.Failed("old")),
            TrashMutation.EntryActionStarted("entry-1"),
        )
        val finished = TrashReducer.reduce(started, TrashMutation.EntryActionFinished)

        assertEquals(TrashOperationState.Entry("entry-1"), started.operation)
        assertSame(TrashOperationState.Idle, finished.operation)
    }

    @Test
    fun `load failure keeps current entries`() {
        val result = TrashReducer.reduce(TrashUiState(), TrashMutation.LoadFailed("failed"))

        assertEquals(TrashContentState.Failed("failed"), result.content)
    }
}
