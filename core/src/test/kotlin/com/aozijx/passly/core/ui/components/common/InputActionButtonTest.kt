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
            progress = false,
            hasInput = true,
            onAction = { submittedInput = input },
            dismissInput = { input = "" },
        )

        assertEquals("correct-password", submittedInput)
        assertEquals("", input)
    }
}
