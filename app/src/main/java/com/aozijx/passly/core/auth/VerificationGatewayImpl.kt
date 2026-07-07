package com.aozijx.passly.core.auth

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.ui.components.toUiMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class VerificationGatewayImpl(
    private val scope: CoroutineScope,
    private val authUseCases: AuthUseCases
) : VerificationGateway {
    override val isAuthorized: StateFlow<Boolean> = authUseCases.isAuthorized
    override val isAppPasswordEnabled: StateFlow<Boolean> = authUseCases.isAppPasswordEnabled

    private val _authMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authMessage: SharedFlow<String> = _authMessage.asSharedFlow()

    override fun verifyWithBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        forceReauth: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        scope.launch {
            val result = if (forceReauth) {
                authUseCases.verifyIdentity(activity, title, subtitle)
            } else {
                authUseCases.authenticate(activity, title, subtitle)
            }
            result.onFailure {
                _authMessage.tryEmit(it.toUiMessage())
            }
            onResult(result)
        }
    }

    override fun verifyWithAppPassword(
        password: CharArray,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        scope.launch {
            val result = authUseCases.authenticateWithAppPassword(password)
            result.onFailure {
                _authMessage.tryEmit(it.toUiMessage())
            }
            onResult(result)
        }
    }

    override fun setAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.setAppPassword(password) }
    }

    override fun bootstrapAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.bootstrapAppPassword(password) }
    }

    override fun changeAppPassword(
        oldPassword: CharArray,
        newPassword: CharArray,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        launchResult(onResult) { authUseCases.changeAppPassword(oldPassword, newPassword) }
    }

    override fun disableAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.disableAppPassword(password) }
    }

    fun rekeyWithInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        launchResult(onResult) {
            authUseCases.rekeyWithInvalidationPolicy(activity, invalidateOnBiometricChange)
        }
    }

    suspend fun lock() = authUseCases.lock()
    fun onUserInteraction() = authUseCases.onUserInteraction()
    suspend fun checkAndLock() = authUseCases.checkAndLock()
    suspend fun updateLockTimeout(timeoutMs: Long) = authUseCases.updateLockTimeout(timeoutMs)

    suspend fun verifyWithBiometricSuspended(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): AppResult<Unit> {
        val result = authUseCases.authenticate(activity, title, subtitle)
        result.onFailure {
            _authMessage.tryEmit(it.toUiMessage())
        }
        return result
    }

    private fun launchResult(
        onResult: (AppResult<Unit>) -> Unit,
        block: suspend () -> AppResult<Unit>
    ) {
        scope.launch { onResult(block()) }
    }
}