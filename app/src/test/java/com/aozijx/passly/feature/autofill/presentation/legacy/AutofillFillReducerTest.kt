package com.aozijx.passly.feature.autofill.presentation.legacy

import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.legacy.LegacyAutofillResultTarget
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AutofillFillReducerTest {
    @Test
    fun `loading replaces a previous terminal state`() {
        val result =
            AutofillFillReducer.reduce(
                AutofillFillUiState.Error("old"),
                AutofillFillMutation.Loading,
            )

        assertSame(AutofillFillUiState.Loading, result)
    }

    @Test
    fun `authentication cancellation completes explicitly`() {
        val result =
            AutofillFillReducer.reduce(
                AutofillFillUiState.Loading,
                AutofillFillMutation.Completed(AutofillFillResult.Cancelled),
            )

        val completed = result as AutofillFillUiState.Result
        assertSame(AutofillFillResult.Cancelled, completed.result)
    }

    @Test
    fun `successful platform delivery clears the retained terminal result`() {
        val terminal =
            AutofillFillUiState.Result(
                AutofillFillResult.Cancelled,
            )

        val cleared = AutofillFillReducer.reduce(terminal, AutofillFillMutation.ResultDelivered)

        assertSame(AutofillFillUiState.Initial, cleared)
    }

    @Test
    fun `successful cancellation delivery clears an error terminal state`() {
        val cleared =
            AutofillFillReducer.reduce(
                AutofillFillUiState.Error("failed"),
                AutofillFillMutation.ResultDelivered,
            )

        assertSame(AutofillFillUiState.Initial, cleared)
    }

    @Test
    fun `selection terminal retains semantic credential and delivery receipt`() {
        val payload =
            AutofillCredentialPayload(
                entryId = EntryId("entry-1"),
                username = "alice",
                password = "secret",
                otpCode = "123456",
            )
        val receipt = AutofillSelectionReceipt(AutofillInteractionId("interaction-1"), payload.entryId)
        val selected =
            AutofillFillResult.SelectedCredential(
                payload = payload,
                receipt = receipt,
                target = LegacyAutofillResultTarget.DATASET,
            )

        val state =
            AutofillFillReducer.reduce(
                AutofillFillUiState.Loading,
                AutofillFillMutation.Completed(selected),
            ) as AutofillFillUiState.Result

        assertSame(selected, state.result)
    }

    @Test
    fun `failure exposes the workflow message`() {
        val result =
            AutofillFillReducer.reduce(
                AutofillFillUiState.Loading,
                AutofillFillMutation.Failed("Entry not found"),
            )

        assertEquals("Entry not found", (result as AutofillFillUiState.Error).message)
    }

    @Test
    fun `non candidate workflow states render an explicit hidden display state`() {
        val states =
            listOf(
                AutofillFillUiState.Initial,
                AutofillFillUiState.Loading,
                AutofillFillUiState.Result(AutofillFillResult.Cancelled),
                AutofillFillUiState.Error("failed"),
            )

        states.forEach { state ->
            assertSame(AutofillFillDisplayState.Hidden, state.toDisplayState())
        }
    }

    @Test
    fun `candidate workflow state exposes candidates through display state`() {
        val display = AutofillFillUiState.ShowCandidates(emptyList()).toDisplayState()

        assertEquals(
            AutofillFillDisplayState.Candidates(emptyList()),
            display,
        )
    }
}
