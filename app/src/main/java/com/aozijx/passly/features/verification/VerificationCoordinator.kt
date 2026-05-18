package com.aozijx.passly.features.verification

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.security.auth.AuthValidationResult
import com.aozijx.passly.core.security.auth.AuthValidationSupport
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.features.common.toUiMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class VerificationCoordinator(
    private val scope: CoroutineScope,
    private val authUseCases: AuthUseCases,
    private val validationSupport: AuthValidationSupport = AuthValidationSupport()
) : VerificationGateway {
    override val isAuthorized: StateFlow<Boolean> = authUseCases.isAuthorized
    override val isAppPasswordEnabled: StateFlow<Boolean> = authUseCases.isAppPasswordEnabled

    private val _authMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authMessage: SharedFlow<String> = _authMessage.asSharedFlow()

    override fun verifyWithBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        when (val validation = validationSupport.validateAuthenticationRequest(activity, title)) {
            is AuthValidationResult.Invalid -> {
                val msg = validationSupport.sanitizeMessage(validation.message)
                _authMessage.tryEmit(msg)
                onResult(
                    AppResult.failure(
                        com.aozijx.passly.core.error.AppError.AuthFailed(
                            validation.message
                        )
                    )
                )
                return
            }
            AuthValidationResult.Valid -> Unit
        }

        scope.launch {
            val result = authUseCases.authenticate(activity, title, subtitle)
            result.onFailure { _authMessage.tryEmit(validationSupport.sanitizeMessage(it.toUiMessage())) }
            onResult(result)
        }
    }

    override fun verifyWithAppPassword(
        password: CharArray,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        scope.launch {
            val result = authUseCases.authenticateWithAppPassword(password)
            result.onFailure { _authMessage.tryEmit(validationSupport.sanitizeMessage(it.toUiMessage())) }
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

    fun lock() = authUseCases.lock()
    fun onUserInteraction() = authUseCases.onUserInteraction()
    fun checkAndLock() = authUseCases.checkAndLock()
    fun updateLockTimeout(timeoutMs: Long) = authUseCases.updateLockTimeout(timeoutMs)

    suspend fun verifyWithBiometricSuspended(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): AppResult<Unit> {
        when (val validation = validationSupport.validateAuthenticationRequest(activity, title)) {
            is AuthValidationResult.Invalid -> {
                val msg = validationSupport.sanitizeMessage(validation.message)
                _authMessage.tryEmit(msg)
                return AppResult.failure(com.aozijx.passly.core.error.AppError.AuthFailed(validation.message))
            }

            AuthValidationResult.Valid -> Unit
        }

        val result = authUseCases.authenticate(activity, title, subtitle)
        result.onFailure { _authMessage.tryEmit(validationSupport.sanitizeMessage(it.toUiMessage())) }
        return result
    }

    private fun launchResult(
        onResult: (AppResult<Unit>) -> Unit,
        block: suspend () -> AppResult<Unit>
    ) {
        scope.launch { onResult(block()) }
    }
}