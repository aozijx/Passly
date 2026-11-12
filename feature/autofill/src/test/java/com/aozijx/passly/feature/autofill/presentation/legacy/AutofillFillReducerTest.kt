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
    fun `late loading cannot replace a terminal state`() {
        val result =
            AutofillFillReducer.reduce(
                AutofillFillUiState.Result(AutofillFillResult.Failed(AutofillFillFailure.INTERNAL)),
                AutofillFillMutation.Loading,
            )

        assertEquals(AutofillFillUiState.Result(AutofillFillResult.Failed(AutofillFillFailure.INTERNAL)), result)
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
    fun `result delivery clears result and failure terminal states`() {
        val terminalStates =
            listOf(
                "result" to AutofillFillUiState.Result(AutofillFillResult.Cancelled),
                "failure" to AutofillFillUiState.Result(AutofillFillResult.Failed(AutofillFillFailure.INTERNAL)),
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
    fun `failure retains its typed reason`() {
        val result =
            AutofillFillReducer.reduce(
                AutofillFillUiState.Loading,
                AutofillFillMutation.Completed(AutofillFillResult.Failed(AutofillFillFailure.DISCOVERY)),
            )

        assertEquals(
            AutofillFillUiState.Result(AutofillFillResult.Failed(AutofillFillFailure.DISCOVERY)),
            result,
        )
    }

    @Test
    fun `late candidate result cannot reopen a terminal failure`() {
        val failed = AutofillFillUiState.Result(AutofillFillResult.Failed(AutofillFillFailure.SELECTION))

        val result =
            AutofillFillReducer.reduce(
                failed,
                AutofillFillMutation.CandidatesAvailable(emptyList()),
            )

        assertSame(failed, result)
    }

    @Test
    fun `failure completes from a visible intermediate state`() {
        val result =
            AutofillFillReducer.reduce(
                AutofillFillUiState.ShowCandidates(emptyList()),
                AutofillFillMutation.Completed(AutofillFillResult.Failed(AutofillFillFailure.SELECTION)),
            )

        assertEquals(
            AutofillFillUiState.Result(AutofillFillResult.Failed(AutofillFillFailure.SELECTION)),
            result,
        )
    }

    @Test
    fun `late failure cannot cancel a completed selection`() {
        val completed = AutofillFillUiState.Result(AutofillFillResult.Cancelled)

        val result =
            AutofillFillReducer.reduce(
                completed,
                AutofillFillMutation.Completed(AutofillFillResult.Failed(AutofillFillFailure.INTERNAL)),
            )

        assertSame(completed, result)
    }
}
