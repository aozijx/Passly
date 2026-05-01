package com.aozijx.passly.domain.usecase.auth

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.domain.repository.auth.AuthRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * 认证业务用例门面类。
 */
class AuthUseCases(private val repository: AuthRepository) {

    val isAuthorized: StateFlow<Boolean> = repository.isAuthorized
    val isAppPasswordEnabled: StateFlow<Boolean> = repository.isAppPasswordEnabled

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): Result<Unit> = repository.authenticate(activity, title, subtitle)

    suspend fun verifyIdentity(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): Result<Unit> = repository.verifyIdentity(activity, title, subtitle)

    suspend fun authenticateWithAppPassword(password: CharArray): Result<Unit> =
        repository.authenticateWithAppPassword(password)

    suspend fun setAppPassword(password: CharArray): Result<Unit> =
        repository.setAppPassword(password)

    suspend fun bootstrapAppPassword(password: CharArray): Result<Unit> =
        repository.bootstrapAppPassword(password)

    suspend fun changeAppPassword(oldPassword: CharArray, newPassword: CharArray): Result<Unit> =
        repository.changeAppPassword(oldPassword, newPassword)

    suspend fun disableAppPassword(password: CharArray): Result<Unit> =
        repository.disableAppPassword(password)

    fun onExternalAuthorized() = repository.onExternalAuthorized()

    fun lock() = repository.lock()

    fun onUserInteraction() = repository.onUserInteraction()

    fun checkAndLock() = repository.checkAndLock()

    fun updateLockTimeout(timeoutMs: Long) = repository.updateLockTimeout(timeoutMs)

    suspend fun rekeyWithInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean
    ): Result<Unit> = repository.rekeyWithInvalidationPolicy(activity, invalidateOnBiometricChange)
}