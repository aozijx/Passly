package com.aozijx.passly.feature.autofill.framework

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AutofillFillReducerTest {

    @Test
    fun `loading replaces a previous terminal state`() {
        val result = AutofillFillReducer.reduce(
            AutofillFillUiState.Error("old"),
            AutofillFillMutation.Loading,
        )

        assertSame(AutofillFillUiState.Loading, result)
    }

    @Test
    fun `authentication cancellation completes with a null payload`() {
        val result = AutofillFillReducer.reduce(
            AutofillFillUiState.Loading,
            AutofillFillMutation.Completed(null),
        )

        val completed = result as AutofillFillUiState.Result
        assertNull(completed.payload)
    }

    @Test
    fun `failure exposes the workflow message`() {
        val result = AutofillFillReducer.reduce(
            AutofillFillUiState.Loading,
            AutofillFillMutation.Failed("Entry not found"),
        )

        assertEquals("Entry not found", (result as AutofillFillUiState.Error).message)
    }
}
