package com.aozijx.passly.testing

import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner

internal class RecordingAuthenticationMethodProvisioner(
    private val setPassword: suspend (CharArray) -> AuthenticationResult,
) : AuthenticationMethodProvisioner {
    var calls = 0
        private set
    val submitted = mutableListOf<CharArray>()

    override suspend fun authorizeAppPasswordManagement() = unused()

    override suspend fun setAppPassword(password: CharArray): AuthenticationResult {
        calls++
        submitted += password
        return setPassword(password)
    }

    override suspend fun changeAppPassword(currentPassword: CharArray, newPassword: CharArray) = unused()

    override suspend fun disableAppPassword() = unused()

    override suspend fun disableBiometric() = unused()

    override suspend fun rotateBiometricPolicy(invalidateOnEnrollment: Boolean) = unused()

    override suspend fun hasRecoveryCode() = false

    override suspend fun checkRecoveryCode(code: CharArray) = false

    private fun unused(): Nothing = error("Not used in this test")
}
