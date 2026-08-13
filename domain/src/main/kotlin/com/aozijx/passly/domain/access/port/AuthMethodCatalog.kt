package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import kotlinx.coroutines.flow.StateFlow

/**
 * 认证方式目录。
 *
 * 查询当前可用的认证方式及对应认证器。
 * 实现层负责从持久化存储加载各方式的注册状态。
 */
interface AuthMethodCatalog {

    /** 各方式的可用性 */
    val methods: StateFlow<AuthenticationMethods>

    /** 获取指定方式的认证器，不可用时返回 null */
    fun getAuthenticator(method: AuthenticationMethod): Authenticator?

    /** 所有已注册的认证器 */
    val authenticators: Map<AuthenticationMethod, Authenticator>

    /** 刷新可用性 */
    suspend fun refresh()
}
