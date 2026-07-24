package com.aozijx.passly.domain.autofill.usecase

import com.aozijx.passly.core.autofill.dispatcher.FillRequestDispatcher
import com.aozijx.passly.core.autofill.model.FieldDescriptor
import com.aozijx.passly.core.autofill.model.InternalFillRequest
import com.aozijx.passly.di.Strict
import javax.inject.Inject

sealed interface PasswordCredentialResult {
    data class Success(
        val username: String,
        val password: String
    ) : PasswordCredentialResult

    data object NotFound : PasswordCredentialResult
}

class CredentialResponseUseCases @Inject constructor(
    @param:Strict private val dispatcher: FillRequestDispatcher,
) {
    fun resolvePasswordCredential(
        packageName: String,
        webDomain: String?,
    ): PasswordCredentialResult {
        val request = InternalFillRequest(
            parentPackage = packageName,
            webDomain = webDomain,
            fields = listOf(
                FieldDescriptor(viewId = "credential_username", autofillHints = listOf("USERNAME")),
                FieldDescriptor(viewId = "credential_password", autofillHints = listOf("PASSWORD")),
            ),
        )
        val response = dispatcher.dispatch(request)
        val entry = response.candidates.firstOrNull() ?: return PasswordCredentialResult.NotFound
        return PasswordCredentialResult.Success(entry.username, entry.password)
    }
}
