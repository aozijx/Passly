package com.aozijx.passly.presentation.feature.settings.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryDraftReducerTest {
    @Test
    fun `draft follows authorized creation sequence`() {
        val creating = RecoveryDraftReducer.reduce(
            RecoveryDraftState.Empty,
            RecoveryDraftMutation.CreationStarted,
        )
        val ready = RecoveryDraftReducer.reduce(
            creating,
            RecoveryDraftMutation.DraftReady("generation-1"),
        )
        val committed = RecoveryDraftReducer.reduce(ready, RecoveryDraftMutation.Committed)

        assertEquals(RecoveryDraftState.Creating, creating)
        assertEquals(RecoveryDraftState.Ready("generation-1"), ready)
        assertEquals(RecoveryDraftState.Committed, committed)
    }

    @Test
    fun `draft cannot become ready before creation`() {
        val result = RecoveryDraftReducer.reduce(
            RecoveryDraftState.Empty,
            RecoveryDraftMutation.DraftReady("invalid"),
        )

        assertEquals(RecoveryDraftState.Failed, result)
    }

    @Test
    fun `only ready draft can commit`() {
        val result = RecoveryDraftReducer.reduce(
            RecoveryDraftState.Creating,
            RecoveryDraftMutation.Committed,
        )

        assertTrue(result is RecoveryDraftState.Failed)
    }
}
