package com.aozijx.passly.feature.autofill.credential

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.CancellationReason
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialResponseReducerTest {

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
