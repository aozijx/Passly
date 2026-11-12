package com.aozijx.passly.feature.autofill.presentation.legacy

import com.aozijx.passly.domain.autofill.model.AutofillCredentialField
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
                AutofillFillUiState.Error,
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
    fun `result delivery clears result and error terminal states`() {
        val terminalStates =
            listOf(
                "result" to AutofillFillUiState.Result(AutofillFillResult.Cancelled),
                "error" to AutofillFillUiState.Error,
            )
        val actual =
            terminalStates.map { (name, state) ->
                name to AutofillFillReducer.reduce(state, AutofillFillMutation.ResultDelivered)
            }

        assertEquals(
            terminalStates.map { it.first to AutofillFillUiState.Initial },
            actual,
        )
    }

    @Test
    fun `selection terminal retains semantic credential and delivery receipt`() {
        val payload =
            AutofillCredentialPayload(
                entryId = EntryId("entry-1"),
                fields =
                    mapOf(
                        AutofillCredentialField.USERNAME to "alice",
                        AutofillCredentialField.PASSWORD to "secret",
                        AutofillCredentialField.OTP to "123456",
                    ),
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
                AutofillFillMutation.Failed,
            )

        assertEquals(AutofillFillUiState.Error, result)
    }
}
