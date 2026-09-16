package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.AuthenticationResult

interface AuthenticationMethodProvisioner {
    /** Authorizes entry into app-password management with its exact security purpose. */
    suspend fun authorizeAppPasswordManagement(): AuthenticationResult
    /**
     * 调用方将 [password] 的所有权转移给实现；返回前数组会被清零。
     *
     * 在恢复模式中调用只允许重建应用密码材料，不能把恢复会话提升为完整安全会话。
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

    /**
     * 通过新密钥的系统强生物识别 CryptoObject 验证并更新生物识别策略。
     * 调用方不应在此之前重复要求应用密码或其他认证。
     */
    suspend fun rotateBiometricPolicy(invalidateOnEnrollment: Boolean): AuthenticationResult
    suspend fun hasRecoveryCode(): Boolean

    /**
     * 检查给定的恢复码是否有效。此方法仅验证恢复码，不进行认证操作，
     * 不会产生认证成功状态。用于设置页验证恢复码是否正确。
     */
    suspend fun checkRecoveryCode(code: CharArray): Boolean
}
