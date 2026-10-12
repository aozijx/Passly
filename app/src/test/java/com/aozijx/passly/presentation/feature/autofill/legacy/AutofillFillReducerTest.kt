package com.aozijx.passly.presentation.feature.autofill.legacy

import org.junit.Assert.assertEquals
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
    fun `authentication cancellation completes explicitly`() {
        val result = AutofillFillReducer.reduce(
            AutofillFillUiState.Loading,
            AutofillFillMutation.Completed(AutofillFillResult.Cancelled),
        )

        val completed = result as AutofillFillUiState.Result
        assertSame(AutofillFillResult.Cancelled, completed.result)
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
