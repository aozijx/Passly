package com.aozijx.passly.domain.usecase.auth

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.domain.repository.auth.AuthRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * 认证业务用例门面类。
 */
class AuthUseCases(private val repository: AuthRepository) {

    val isAuthorized: StateFlow<Boolean> = repository.isAuthorized

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): Result<Unit> = repository.authenticate(activity, title, subtitle)

    fun onExternalAuthorized() = repository.onExternalAuthorized()

    fun lock() = repository.lock()

    fun onUserInteraction() = repository.onUserInteraction()

    fun checkAndLock() = repository.checkAndLock()

    fun updateLockTimeout(timeoutMs: Long) = repository.updateLockTimeout(timeoutMs)
}