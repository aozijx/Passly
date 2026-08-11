package com.aozijx.passly.domain.auth.port

/**
 * 认证方式管理接口。
 *
 * 外部调用者通过此接口配置认证方式。
 * 实现位于 [com.aozijx.passly.security] 层。
 */
interface AuthMethodAdministration {

    /** 设置应用密码 */
    suspend fun setAppPassword(password: CharArray)

    /** 更改应用密码 */
    suspend fun changeAppPassword(oldPassword: CharArray, newPassword: CharArray)

    /** 禁用应用密码 */
    suspend fun disableAppPassword()

    /** 启用/配置生物识别 */
    suspend fun configureBiometric(invalidateOnEnrollment: Boolean)

    /** 禁用生物识别 */
    suspend fun disableBiometric()

    /** 是否有恢复码 */
    suspend fun hasRecoveryCode(): Boolean

    /** 检查恢复码是否有效（不产生认证状态） */
    suspend fun checkRecoveryCode(code: CharArray): Boolean
}
