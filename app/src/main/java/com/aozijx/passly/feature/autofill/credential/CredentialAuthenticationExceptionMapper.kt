package com.aozijx.passly.feature.autofill.credential

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.autofill.model.AutofillAuthorizationOutcome

internal object CredentialAuthenticationExceptionMapper {
    fun toGetException(authorization: AutofillAuthorizationOutcome): GetCredentialException =
        when (authorization) {
            is AutofillAuthorizationOutcome.Cancelled ->
                GetCredentialCancellationException("Credential access was cancelled")
            is AutofillAuthorizationOutcome.Failed ->
                GetCredentialUnknownException("Credential authentication failed")
            is AutofillAuthorizationOutcome.Rejected ->
                GetCredentialUnknownException("Credential interaction is no longer available")
            is AutofillAuthorizationOutcome.Authorized ->
                GetCredentialUnknownException("Unexpected authorization result")
        }

    fun toCreateException(authorization: AutofillAuthorizationOutcome): CreateCredentialException =
        when (authorization) {
            is AutofillAuthorizationOutcome.Cancelled ->
                CreateCredentialCancellationException("Credential creation was cancelled")
            is AutofillAuthorizationOutcome.Failed ->
                CreateCredentialUnknownException("Credential authentication failed")
            is AutofillAuthorizationOutcome.Rejected ->
                CreateCredentialUnknownException("Credential interaction is no longer available")
            is AutofillAuthorizationOutcome.Authorized ->
                CreateCredentialUnknownException("Unexpected authorization result")
        }

    fun toGetException(authentication: AuthenticationResult): GetCredentialException =
        when (authentication) {
            is AuthenticationResult.Cancelled ->
                GetCredentialCancellationException("Credential access was cancelled")
            is AuthenticationResult.Failure ->
                GetCredentialUnknownException("Credential authentication failed")
            is AuthenticationResult.Success ->
                GetCredentialUnknownException("Unexpected authentication result")
        }

    fun toCreateException(authentication: AuthenticationResult): CreateCredentialException =
        when (authentication) {
            is AuthenticationResult.Cancelled ->
                CreateCredentialCancellationException("Credential creation was cancelled")
            is AuthenticationResult.Failure ->
                CreateCredentialUnknownException("Credential authentication failed")
            is AuthenticationResult.Success ->
                CreateCredentialUnknownException("Unexpected authentication result")
        }
}
