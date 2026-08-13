package com.aozijx.passly.domain.autofill.usecase

import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.policy.CredentialScopeMatcher
import com.aozijx.passly.domain.autofill.repository.CredentialServiceRepository
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed interface PasswordCredentialResult {
    data class Success(
        val username: String,
        val password: String,
    ) : PasswordCredentialResult

    data object NotFound : PasswordCredentialResult
    data class NotAuthorized(
        val authentication: AuthenticationResult,
    ) : PasswordCredentialResult
}

sealed interface CreatePasswordCredentialResult {
    data object Success : CreatePasswordCredentialResult
    data object NotSaved : CreatePasswordCredentialResult
    data class NotAuthorized(
        val authentication: AuthenticationResult,
    ) : CreatePasswordCredentialResult
}

/**
 * Credential Manager phase-two selection.
 *
 * The selected entry ID is authoritative only after it is checked against the
 * calling package/domain and the current autofill policy.
 */
class CredentialResponseUseCases @Inject constructor(
    private val credentialRepository: CredentialServiceRepository,
    private val settingsRepository: AppSettingsRepository,
    private val authenticationManager: AuthenticationManager,
    private val vaultAccessState: SecureSessionAccessState,
    private val autofillUseCases: AutofillUseCases,
) {
    suspend fun resolvePasswordCredential(
        entryId: String,
        packageName: String,
        webDomain: String?,
        allowedUserIds: Set<String> = emptySet(),
    ): PasswordCredentialResult {
        val policy = settingsRepository.settings.first().interaction.autofill
        if (!policy.enabled || !policy.credentialManagerEnabled) return PasswordCredentialResult.NotFound

        if (policy.requireAuthentication || !vaultAccessState.hasFullSecureSessionAccess()) {
            val authentication = authenticationManager.authenticate(
                AuthenticationRequest(AuthenticationPurpose.AUTOFILL)
            )
            if (authentication !is AuthenticationResult.Success) {
                return PasswordCredentialResult.NotAuthorized(authentication)
            }
        }

        val selected = credentialRepository.getById(entryId)
            ?: return PasswordCredentialResult.NotFound
        if (
            !policy.allowUnmatchedSuggestions &&
            !CredentialScopeMatcher.matches(selected, packageName, webDomain)
        ) {
            return PasswordCredentialResult.NotFound
        }

        val password = selected.secret.login?.password.orEmpty()
        if (
            selected.username.isBlank() ||
            password.isBlank() ||
            (allowedUserIds.isNotEmpty() && selected.username !in allowedUserIds)
        ) {
            return PasswordCredentialResult.NotFound
        }
        autofillUseCases.recordUsage(selected.id)
        return PasswordCredentialResult.Success(selected.username, password)
    }

    suspend fun createPasswordCredential(
        packageName: String,
        username: String,
        password: String,
    ): CreatePasswordCredentialResult {
        val policy = settingsRepository.settings.first().interaction.autofill
        if (
            !policy.enabled ||
            !policy.credentialManagerEnabled ||
            packageName.isBlank() ||
            username.isBlank() ||
            password.isBlank()
        ) {
            return CreatePasswordCredentialResult.NotSaved
        }

        if (policy.requireAuthentication || !vaultAccessState.hasFullSecureSessionAccess()) {
            val authentication = authenticationManager.authenticate(
                AuthenticationRequest(AuthenticationPurpose.AUTOFILL)
            )
            if (authentication !is AuthenticationResult.Success) {
                return CreatePasswordCredentialResult.NotAuthorized(authentication)
            }
        }

        val saved = credentialRepository.save(
            packageName = packageName,
            webDomain = null,
            pageTitle = null,
            usernameValue = username,
            passwordValue = password,
        )
        return if (saved) {
            CreatePasswordCredentialResult.Success
        } else {
            CreatePasswordCredentialResult.NotSaved
        }
    }
}
