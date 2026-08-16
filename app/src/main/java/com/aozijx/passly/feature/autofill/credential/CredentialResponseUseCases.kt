package com.aozijx.passly.feature.autofill.credential

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.data.repository.autofill.CredentialServiceRepository
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.feature.autofill.shared.AutofillUseCases
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
            !selected.matchesScope(packageName, webDomain)
        ) {
            return PasswordCredentialResult.NotFound
        }

        val password = selected.secret.login?.password.orEmpty()
        if (
            selected.profile.username.isBlank() ||
            password.isBlank() ||
            (allowedUserIds.isNotEmpty() && selected.profile.username !in allowedUserIds)
        ) {
            return PasswordCredentialResult.NotFound
        }
        autofillUseCases.recordUsage(selected.id.value)
        return PasswordCredentialResult.Success(selected.profile.username, password)
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

private fun Entry.matchesScope(packageName: String, webDomain: String?): Boolean {
    val normalizedPackage = packageName.trim().lowercase()
    if (profile.associations.applicationIds.any { it.trim().lowercase() == normalizedPackage }) {
        return true
    }
    val normalizedDomain = webDomain?.trim()?.lowercase()?.removeSuffix(".") ?: return false
    return buildSet {
        addAll(profile.associations.domains)
        profile.associations.primaryUrl?.let(::add)
    }.any { candidate ->
        candidate.trim().lowercase().removePrefix("https://").removePrefix("http://")
            .substringBefore('/').substringBefore(':').removeSuffix(".") == normalizedDomain
    }
}
