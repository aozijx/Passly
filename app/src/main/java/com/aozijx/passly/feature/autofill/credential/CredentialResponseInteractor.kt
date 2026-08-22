package com.aozijx.passly.feature.autofill.credential

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.AutofillScope
import com.aozijx.passly.domain.autofill.port.AutofillGrantStore
import com.aozijx.passly.domain.autofill.port.CredentialServiceRepository
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.feature.autofill.shared.RecordAutofillUsageUseCase
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
class CredentialResponseInteractor @Inject constructor(
    private val credentialRepository: CredentialServiceRepository,
    private val settingsRepository: AppSettingsRepository,
    private val authenticationManager: AuthenticationManager,
    private val vaultAccessState: SecureSessionAccessState,
    private val recordAutofillUsage: RecordAutofillUsageUseCase,
    private val grantStore: AutofillGrantStore,
) {
    suspend fun resolvePasswordCredential(
        entryId: String,
        packageName: String,
        webDomain: String?,
        allowedUserIds: Set<String> = emptySet(),
    ): PasswordCredentialResult {
        val policy = settingsRepository.settings.first().interaction.autofill
        if (!policy.enabled || !policy.credentialManagerEnabled) return PasswordCredentialResult.NotFound

        // "填充前验证"开启时，仅当本次交互还没有短期授权（如刚完成解锁动作）
        // 才要求重新认证，避免"解锁 → 选择条目"连续弹两次认证。
        val grantActive = grantStore.isGranted(
            AutofillGrantContext(packageName = packageName, webDomain = webDomain)
        )
        if ((policy.requireAuthentication && !grantActive) ||
            !vaultAccessState.hasFullSecureSessionAccess()
        ) {
            val authentication = authenticationManager.authenticate(
                AuthenticationRequest(AuthenticationPurpose.AUTOFILL)
            )
            if (authentication !is AuthenticationResult.Success) {
                return PasswordCredentialResult.NotAuthorized(authentication)
            }
            grantStore.grant(
                AutofillGrantContext(packageName = packageName, webDomain = webDomain)
            )
        }

        val selected = credentialRepository.getById(entryId)
            ?: return PasswordCredentialResult.NotFound
        if (
            !policy.allowUnmatchedSuggestions &&
            !AutofillScope.matches(selected, packageName, webDomain)
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
        recordAutofillUsage(selected.id.value)
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

        // 保存是用户在系统确认后的动作，本身不需要"填充前验证"策略；
        // 只需确保会话可写。vault 锁定（未解锁）时仍然认证以解锁会话，
        // 已解锁则直接写入，避免保存流程被多余的认证弹窗打断。
        if (!vaultAccessState.hasFullSecureSessionAccess()) {
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
