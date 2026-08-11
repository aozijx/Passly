package com.aozijx.passly.feature.autofill.credential

import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationFailureCode
import com.aozijx.passly.domain.authentication.AuthenticationResult
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
        val authentication = AuthenticationResult.Cancelled(byUser = true)

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
                authCode = AuthenticationFailureCode.CREDENTIAL_INCORRECT,
                correlationId = "sensitive-correlation",
            )
        )

        val exception = CredentialAuthenticationExceptionMapper.toGetException(authentication)

        assertTrue(exception is androidx.credentials.exceptions.GetCredentialUnknownException)
        assertTrue("sensitive-correlation" !in exception.message.orEmpty())
    }
}
