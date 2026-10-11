package com.aozijx.passly.core.ui.components.common

import org.junit.Assert.assertEquals
import org.junit.Test

class InputActionButtonTest {
    @Test
    fun submitReadsInputBeforeCollapseClearsIt() {
        var input = "correct-password"
        var submittedInput: String? = null

        performInputActionSubmit(
            enabled = true,
            phase = ActionButtonPhase.Idle,
            hasInput = true,
            onAction = { submittedInput = input },
            dismissInput = { input = "" },
        )

        assertEquals("correct-password", submittedInput)
        assertEquals("", input)
    }

    @Test
    fun resultPhaseCannotSubmitAgainBeforeItIsConsumed() {
        var submissions = 0

        performInputActionSubmit(
            enabled = true,
            phase = ActionButtonPhase.Succeeded,
            hasInput = true,
            onAction = { submissions++ },
            dismissInput = {},
        )

        assertEquals(0, submissions)
    }
}
