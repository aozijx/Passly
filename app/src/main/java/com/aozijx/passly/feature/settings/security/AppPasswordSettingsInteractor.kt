package com.aozijx.passly.feature.settings.security

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

sealed interface AppPasswordManagementAccess {
    data class Authorized(val alreadyEnabled: Boolean) : AppPasswordManagementAccess
    data object Cancelled : AppPasswordManagementAccess
    data class Failed(val failure: AuthenticationFailure) : AppPasswordManagementAccess
}

sealed interface AppPasswordChangeResult {
    data object Completed : AppPasswordChangeResult
    data object Cancelled : AppPasswordChangeResult
    data class Failed(val failure: AuthenticationFailure) : AppPasswordChangeResult
    data class InvalidInput(val reason: AppPasswordInputError) : AppPasswordChangeResult
}

enum class AppPasswordInputError {
    REQUIRED_FIELDS,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
}

sealed interface AppPasswordChangeRequest {
    data class Set(
        val password: CharArray,
        val confirmation: CharArray,
    ) : AppPasswordChangeRequest

    data class Change(
        val currentPassword: CharArray,
        val newPassword: CharArray,
        val confirmation: CharArray,
    ) : AppPasswordChangeRequest

    data object Disable : AppPasswordChangeRequest
}

class AppPasswordSettingsInteractor @Inject constructor(
    private val authenticationMethodAvailability: AuthenticationMethodAvailability,
    private val authenticationMethodProvisioner: AuthenticationMethodProvisioner,
) {
    val isEnabled: Flow<Boolean> = authenticationMethodAvailability.methods
        .map { AuthenticationMethod.APP_PASSWORD in it }
        .distinctUntilChanged()

    suspend fun authorizeManagement(): AppPasswordManagementAccess =
        when (val result = authenticationMethodProvisioner.authorizeAppPasswordManagement()) {
            is AuthenticationResult.Success -> AppPasswordManagementAccess.Authorized(
                alreadyEnabled = AuthenticationMethod.APP_PASSWORD in
                    authenticationMethodAvailability.methods.value,
            )
            is AuthenticationResult.Cancelled -> AppPasswordManagementAccess.Cancelled
            is AuthenticationResult.Failure -> AppPasswordManagementAccess.Failed(result.failure)
        }

    suspend fun execute(request: AppPasswordChangeRequest): AppPasswordChangeResult = try {
        when (request) {
            is AppPasswordChangeRequest.Set -> when {
                !AppPasswordPolicy.DEFAULT.acceptsLength(request.password.size) ->
                    AppPasswordChangeResult.InvalidInput(AppPasswordInputError.PASSWORD_TOO_SHORT)
                !request.password.contentEquals(request.confirmation) ->
                    AppPasswordChangeResult.InvalidInput(AppPasswordInputError.PASSWORD_MISMATCH)
                else -> authenticationMethodProvisioner
                    .setAppPassword(request.password)
                    .toChangeResult()
            }
            is AppPasswordChangeRequest.Change -> when {
                request.currentPassword.isEmpty() || request.newPassword.isEmpty() ->
                    AppPasswordChangeResult.InvalidInput(AppPasswordInputError.REQUIRED_FIELDS)
                !AppPasswordPolicy.DEFAULT.acceptsLength(request.newPassword.size) ->
                    AppPasswordChangeResult.InvalidInput(AppPasswordInputError.PASSWORD_TOO_SHORT)
                !request.newPassword.contentEquals(request.confirmation) ->
                    AppPasswordChangeResult.InvalidInput(AppPasswordInputError.PASSWORD_MISMATCH)
                else -> authenticationMethodProvisioner.changeAppPassword(
                    request.currentPassword,
                    request.newPassword,
                ).toChangeResult()
            }
            AppPasswordChangeRequest.Disable -> authenticationMethodProvisioner
                .disableAppPassword()
                .toChangeResult()
        }
    } finally {
        request.clearSensitiveInput()
    }

}

private fun AppPasswordChangeRequest.clearSensitiveInput() {
    when (this) {
        is AppPasswordChangeRequest.Set -> {
            password.fill('\u0000')
            confirmation.fill('\u0000')
        }
        is AppPasswordChangeRequest.Change -> {
            currentPassword.fill('\u0000')
            newPassword.fill('\u0000')
            confirmation.fill('\u0000')
        }
        AppPasswordChangeRequest.Disable -> Unit
    }
}

private fun AuthenticationResult.toChangeResult(): AppPasswordChangeResult = when (this) {
    is AuthenticationResult.Success -> AppPasswordChangeResult.Completed
    is AuthenticationResult.Cancelled -> AppPasswordChangeResult.Cancelled
    is AuthenticationResult.Failure -> AppPasswordChangeResult.Failed(failure)
}
