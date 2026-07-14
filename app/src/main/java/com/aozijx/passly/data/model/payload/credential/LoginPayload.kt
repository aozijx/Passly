package com.aozijx.passly.data.model.payload.credential

import kotlinx.serialization.Serializable

/**
 * 登录凭据 Payload —— 所有账号类型统一使用。
 *
 * 对应 VaultEntry 的 username / password / email / notes。
 * 从 CredentialPayload 根节点提取为子 Payload，避免层级膨胀。
 */
@Serializable
data class LoginPayload(
    val username: String? = null,
    val password: String? = null,
    val email: String? = null,
    val notes: String? = null
)
