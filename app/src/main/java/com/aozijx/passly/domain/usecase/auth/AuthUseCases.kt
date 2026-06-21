package com.aozijx.passly.domain.usecase.auth

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.repository.auth.AuthRepository
import kotlinx.coroutines.flow.StateFlow

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthUseCases @Inject constructor(private val repository: AuthRepository) {
    val isAuthorized: StateFlow<Boolean> = repository.isAuthorized
    val isAppPasswordEnabled: StateFlow<Boolean> = repository.isAppPasswordEnabled

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): AppResult<Unit> = repository.authenticate(activity, title, subtitle)

    suspend fun authenticateWithAppPassword(password: CharArray): AppResult<Unit> =
        repository.authenticateWithAppPassword(password)

    suspend fun setAppPassword(password: CharArray): AppResult<Unit> =
        repository.setAppPassword(password)

    suspend fun bootstrapAppPassword(password: CharArray): AppResult<Unit> =
        repository.bootstrapAppPassword(password)

    suspend fun changeAppPassword(
        oldPassword: CharArray,
        newPassword: CharArray
    ): AppResult<Unit> = repository.changeAppPassword(oldPassword, newPassword)

    suspend fun disableAppPassword(password: CharArray): AppResult<Unit> =
        repository.disableAppPassword(password)

    suspend fun verifyIdentity(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): AppResult<Unit> = repository.verifyIdentity(activity, title, subtitle)

    suspend fun rekeyWithInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean
    ): AppResult<Unit> =
        repository.rekeyWithInvalidationPolicy(activity, invalidateOnBiometricChange)

    fun lock() = repository.lock()
    fun onUserInteraction() = repository.onUserInteraction()
    fun checkAndLock() = repository.checkAndLock()
    fun updateLockTimeout(timeoutMs: Long) = repository.updateLockTimeout(timeoutMs)
}