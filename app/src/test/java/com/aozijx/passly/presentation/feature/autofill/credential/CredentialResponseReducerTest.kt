package com.aozijx.passly.presentation.feature.autofill.credential

import android.content.Intent
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.credential.CredentialAuthenticationExceptionMapper
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialResponseReducerTest {

    @Test
    fun `selected password response requires committed selection delivery`() {
        val intent = Intent()
        val receipt = AutofillSelectionReceipt(
            AutofillInteractionId("interaction-1"),
            EntryId("entry-1"),
        )

        val result = CredentialResponseReducer.reduce(
            CredentialResponseUiState.Loading,
            CredentialResponseMutation.Completed(
                CredentialResponseCompletion.CommittedSelection(intent, receipt),
            ),
        ) as CredentialResponseUiState.Complete

        val completion = result.completion as CredentialResponseCompletion.CommittedSelection
        assertSame(intent, completion.resultIntent)
        assertEquals(receipt, completion.receipt)
    }

    @Test
    fun `non selection response completes immediately without a nullable receipt`() {
        val intent = Intent()

        val result = CredentialResponseReducer.reduce(
            CredentialResponseUiState.Loading,
            CredentialResponseMutation.Completed(
                CredentialResponseCompletion.Immediate(intent),
            ),
        ) as CredentialResponseUiState.Complete

        val completion = result.completion as CredentialResponseCompletion.Immediate
        assertSame(intent, completion.resultIntent)
    }

    @Test
    fun `unknown action becomes unrecoverable`() {
        val result = CredentialResponseReducer.reduce(
            CredentialResponseUiState.Loading,
            CredentialResponseMutation.Unrecoverable,
        )

        assertTrue(result is CredentialResponseUiState.Unrecoverable)
    }

    @Test
    fun `cancelled authentication maps to credential cancellation`() {
        val authentication = AuthenticationResult.Cancelled(CancellationReason.USER)

        val getException = CredentialAuthenticationExceptionMapper.toGetException(authentication)
        val createException =
            CredentialAuthenticationExceptionMapper.toCreateException(authentication)

        assertTrue(
            getException is androidx.credentials.exceptions.GetCredentialCancellationException
        )
        assertTrue(
            createException is
                    androidx.credentials.exceptions.CreateCredentialCancellationException
        )
    }

    @Test
    fun `authentication failure does not leak domain details`() {
        val authentication = AuthenticationResult.Failure(
            AuthenticationFailure(
                code = AuthenticationFailureCode.CREDENTIAL_INCORRECT,
            )
        )

        val exception = CredentialAuthenticationExceptionMapper.toGetException(authentication)

        assertTrue(exception is androidx.credentials.exceptions.GetCredentialUnknownException)
        assertTrue("CREDENTIAL_INCORRECT" !in exception.message.orEmpty())
    }
}
