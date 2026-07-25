package com.aozijx.passly.domain.auth.port

import com.aozijx.passly.domain.auth.policy.AuthMethodType
import kotlinx.coroutines.flow.StateFlow

/**
 * 认证方式目录。
 *
 * 查询当前可用的认证方式及对应认证器。
 * 实现层负责从持久化存储加载各方式的注册状态。
 */
interface AuthMethodCatalog {

    /** 各方式的可用性 */
    val availability: StateFlow<AuthMethodAvailability>

    /** 获取指定方式的认证器，不可用时返回 null */
    fun getAuthenticator(type: AuthMethodType): Authenticator?

    /** 所有已注册的认证器 */
    val authenticators: Map<AuthMethodType, Authenticator>

    /** 刷新可用性 */
    suspend fun refresh()
}

data class AuthMethodAvailability(
    val biometric: Boolean = false,
    val appPassword: Boolean = false,
    val recoveryCode: Boolean = false
) {
    fun isAvailable(type: AuthMethodType): Boolean = when (type) {
        AuthMethodType.BIOMETRIC -> biometric
        AuthMethodType.APP_PASSWORD -> appPassword
        AuthMethodType.RECOVERY_CODE -> recoveryCode
    }
}
