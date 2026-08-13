package com.aozijx.passly.domain.auth.port

import com.aozijx.passly.domain.authentication.AuthMethodAvailability
import kotlinx.coroutines.flow.StateFlow

/**
 * 认证方式可用性查询接口。
 */
interface AuthAvailability {
    val methods: StateFlow<AuthMethodAvailability>
}
