package com.aozijx.passly.domain.authentication

interface AuthenticationMethodProvisioner {
    suspend fun setAppPassword(password: CharArray): AuthenticationResult
    suspend fun changeAppPassword(newPassword: CharArray): AuthenticationResult
    suspend fun disableAppPassword(): AuthenticationResult
    suspend fun hasRecoveryCode(): Boolean
    suspend fun verifyRecoveryCode(code: CharArray): Boolean
}
