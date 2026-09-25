package com.aozijx.passly.feature.settings.security

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
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

    suspend fun set(password: CharArray): AppPasswordChangeResult =
        authenticationMethodProvisioner.setAppPassword(password).toChangeResult()

    suspend fun change(
        currentPassword: CharArray,
        newPassword: CharArray,
    ): AppPasswordChangeResult = authenticationMethodProvisioner
        .changeAppPassword(currentPassword, newPassword)
        .toChangeResult()

    suspend fun disable(): AppPasswordChangeResult =
        authenticationMethodProvisioner.disableAppPassword().toChangeResult()

}

private fun AuthenticationResult.toChangeResult(): AppPasswordChangeResult = when (this) {
    is AuthenticationResult.Success -> AppPasswordChangeResult.Completed
    is AuthenticationResult.Cancelled -> AppPasswordChangeResult.Cancelled
    is AuthenticationResult.Failure -> AppPasswordChangeResult.Failed(failure)
}
