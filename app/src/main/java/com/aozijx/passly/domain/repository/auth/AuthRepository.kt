package com.aozijx.passly.domain.repository.auth

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.error.AppResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val isAuthorized: StateFlow<Boolean>

    val isAppPasswordEnabled: StateFlow<Boolean>

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): AppResult<Unit>

    suspend fun verifyIdentity(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): AppResult<Unit>

    suspend fun authenticateWithAppPassword(password: CharArray): AppResult<Unit>

    suspend fun setAppPassword(password: CharArray): AppResult<Unit>

    suspend fun bootstrapAppPassword(password: CharArray): AppResult<Unit>

    suspend fun changeAppPassword(oldPassword: CharArray, newPassword: CharArray): AppResult<Unit>

    suspend fun disableAppPassword(password: CharArray): AppResult<Unit>

    fun onExternalAuthorized()

    fun lock()

    fun onUserInteraction()

    fun checkAndLock()

    fun updateLockTimeout(timeoutMs: Long)

    suspend fun rekeyWithInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean
    ): AppResult<Unit>
}