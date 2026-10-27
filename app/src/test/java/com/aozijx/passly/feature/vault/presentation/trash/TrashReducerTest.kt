package com.aozijx.passly.feature.vault.presentation.trash

import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TrashReducerTest {
    @Test
    fun `entry operation clears error and owns busy entry`() {
        val started = TrashReducer.reduce(
            TrashUiState(operation = TrashOperationState.Failed(UiErrorMessage(R.string.trash_error_command))),
            TrashMutation.EntryActionStarted("entry-1"),
        )
        val finished = TrashReducer.reduce(started, TrashMutation.EntryActionFinished)

        assertEquals(TrashOperationState.Entry("entry-1"), started.operation)
        assertSame(TrashOperationState.Idle, finished.operation)
    }

    @Test
    fun `load failure is explicit instead of an empty loaded result`() {
        val result = TrashReducer.reduce(TrashUiState(), TrashMutation.LoadFailed(UiErrorMessage(R.string.trash_error_load)))

        assertEquals(TrashContentState.Failed(UiErrorMessage(R.string.trash_error_load)), result.content)
    }

    @Test
    fun `dismissing an operation error cannot turn a failed load into an empty list`() {
        val state = TrashUiState(content = TrashContentState.Failed(UiErrorMessage(R.string.trash_error_load)))
        val result = TrashReducer.reduce(state, TrashMutation.ErrorCleared)
        assertEquals(state.content, result.content)
    }
}
