package com.aozijx.passly.domain.authentication

interface AuthenticationMethodProvisioner {
    /**
     * 调用方将 [password] 的所有权转移给实现；返回前数组会被清零。
     */
    suspend fun setAppPassword(password: CharArray): AuthenticationResult

    /**
     * 验证 [currentPassword] 后再更新密码。两个数组的所有权都会被转移并清零。
     */
    suspend fun changeAppPassword(
        currentPassword: CharArray,
        newPassword: CharArray
    ): AuthenticationResult

    suspend fun disableAppPassword(): AuthenticationResult
    suspend fun disableBiometric(): AuthenticationResult
    suspend fun rotateBiometricPolicy(invalidateOnEnrollment: Boolean): AuthenticationResult
    suspend fun hasRecoveryCode(): Boolean
    suspend fun verifyRecoveryCode(code: CharArray): Boolean
}
