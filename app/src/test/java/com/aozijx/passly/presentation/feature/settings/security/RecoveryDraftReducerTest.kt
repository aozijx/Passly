package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryDraftReducerTest {

    @Test
    fun `draft follows authenticated generation sequence`() {
        val authenticating = RecoveryDraftReducer.reduce(
            RecoveryDraftState.Empty,
            RecoveryDraftMutation.AuthenticationStarted,
        )
        val generating = RecoveryDraftReducer.reduce(
            authenticating,
            RecoveryDraftMutation.GenerationStarted,
        )
        val ready = RecoveryDraftReducer.reduce(
            generating,
            RecoveryDraftMutation.DraftReady("generation-1"),
        )
        val committed = RecoveryDraftReducer.reduce(ready, RecoveryDraftMutation.Committed)

        assertEquals(RecoveryDraftState.Authenticating, authenticating)
        assertEquals(RecoveryDraftState.Generating, generating)
        assertEquals(RecoveryDraftState.Ready("generation-1"), ready)
        assertEquals(RecoveryDraftState.Committed, committed)
    }

    @Test
    fun `draft cannot become ready before generation`() {
        val result = RecoveryDraftReducer.reduce(
            RecoveryDraftState.Empty,
            RecoveryDraftMutation.DraftReady("invalid"),
        )

        assertEquals(RecoveryDraftState.Failed, result)
    }

    @Test
    fun `only ready draft can commit`() {
        val result = RecoveryDraftReducer.reduce(
            RecoveryDraftState.Generating,
            RecoveryDraftMutation.Committed,
        )

        assertTrue(result is RecoveryDraftState.Failed)
    }
}
