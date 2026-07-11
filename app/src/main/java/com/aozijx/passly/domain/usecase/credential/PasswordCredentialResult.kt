package com.aozijx.passly.domain.usecase.credential

sealed class PasswordCredentialResult {
    data class Success(
        val username: String,
        val password: String,
    ) : PasswordCredentialResult()

    data object NotFound : PasswordCredentialResult()
}
