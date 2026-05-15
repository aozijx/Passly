package com.aozijx.passly.features.auth

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.security.auth.AuthValidationResult
import com.aozijx.passly.core.security.auth.AuthValidationSupport
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.features.auth.ui.AuthScreenAuthGateway
import com.aozijx.passly.features.auth.ui.SettingsAuthGateway
import com.aozijx.passly.features.common.toUiMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AuthCoordinator(
    private val scope: CoroutineScope,
    private val authUseCases: AuthUseCases,
    private val validationSupport: AuthValidationSupport = AuthValidationSupport()
) : AuthScreenAuthGateway, SettingsAuthGateway {
    val isAuthorized: StateFlow<Boolean> = authUseCases.isAuthorized
    override val isAppPasswordEnabled: StateFlow<Boolean> = authUseCases.isAppPasswordEnabled

    private val _authMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authMessage: SharedFlow<String> = _authMessage.asSharedFlow()

    override fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)?
    ) {
        when (val validation = validationSupport.validateAuthenticationRequest(activity, title)) {
            is AuthValidationResult.Invalid -> {
                emitAuthError(validation.message, onError)
                return
            }
            AuthValidationResult.Valid -> Unit
        }

        scope.launch {
            authUseCases.authenticate(activity, title, subtitle)
                .onSuccess { onSuccess() }
                .onFailure { emitAuthError(it.toUiMessage(), onError) }
        }
    }

    override fun authenticateWithAppPassword(
        password: CharArray, onSuccess: () -> Unit, onError: ((String) -> Unit)?
    ) {
        scope.launch {
            authUseCases.authenticateWithAppPassword(password)
                .onSuccess { onSuccess() }
                .onFailure { emitAuthError(it.toUiMessage(), onError) }
        }
    }

    override fun setAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.setAppPassword(password) }
    }

    override fun bootstrapAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.bootstrapAppPassword(password) }
    }

    override fun changeAppPassword(
        oldPassword: CharArray, newPassword: CharArray, onResult: (AppResult<Unit>) -> Unit
    ) {
        launchResult(onResult) { authUseCases.changeAppPassword(oldPassword, newPassword) }
    }

    override fun disableAppPassword(password: CharArray, onResult: (AppResult<Unit>) -> Unit) {
        launchResult(onResult) { authUseCases.disableAppPassword(password) }
    }

    override fun verifyIdentity(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        launchResult(onResult) { authUseCases.verifyIdentity(activity, title, subtitle) }
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

    private fun emitAuthError(message: String?, onError: ((String) -> Unit)?) {
        val resolved = validationSupport.sanitizeMessage(message)
        _authMessage.tryEmit(resolved)
        onError?.invoke(resolved)
    }

    private fun launchResult(
        onResult: (AppResult<Unit>) -> Unit, block: suspend () -> AppResult<Unit>
    ) {
        scope.launch { onResult(block()) }
    }

    fun lock() = authUseCases.lock()
    fun onUserInteraction() = authUseCases.onUserInteraction()
    fun checkAndLock() = authUseCases.checkAndLock()
    fun updateLockTimeout(timeoutMs: Long) = authUseCases.updateLockTimeout(timeoutMs)
}