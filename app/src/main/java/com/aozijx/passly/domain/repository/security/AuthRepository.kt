package com.aozijx.passly.domain.repository.security

import com.aozijx.passly.core.error.AppResult
import kotlinx.coroutines.flow.StateFlow

/**
 * 认证仓库接口
 */
interface AuthRepository {
    val isAuthorized: StateFlow<Boolean>
    val isAppPasswordEnabled: StateFlow<Boolean>

    suspend fun authenticateWithAppPassword(password: CharArray): AppResult<Unit>

    suspend fun setAppPassword(password: CharArray): AppResult<Unit>

    suspend fun bootstrapAppPassword(password: CharArray): AppResult<Unit>

    suspend fun changeAppPassword(oldPassword: CharArray, newPassword: CharArray): AppResult<Unit>

    suspend fun disableAppPassword(password: CharArray): AppResult<Unit>

    fun onExternalAuthorized()
}
