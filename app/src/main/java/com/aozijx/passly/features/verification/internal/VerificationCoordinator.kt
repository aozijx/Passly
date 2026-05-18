package com.aozijx.passly.features.verification.internal

import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.auth.validation.AuthRequestValidator
import com.aozijx.passly.core.auth.validation.AuthRequestValidator.AuthRequestValidationResult
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.features.common.toUiMessage
import com.aozijx.passly.features.verification.contract.VerificationGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class VerificationCoordinator(
    private val scope: CoroutineScope,
    private val authUseCases: AuthUseCases,
    private val requestValidator: AuthRequestValidator = AuthRequestValidator()
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
        when (val validation = requestValidator.validateRequest(activity, title)) {
            is AuthRequestValidationResult.Invalid -> {
                val msg = requestValidator.sanitizeMessage(validation.message)
                _authMessage.tryEmit(msg)
                onResult(
                    AppResult.failure(
                        AppError.AuthFailed(
                            validation.message
                        )
                    )
                )
                return
            }

            AuthRequestValidationResult.Valid -> Unit
        }

        scope.launch {
            val result = authUseCases.authenticate(activity, title, subtitle)
            result.onFailure { _authMessage.tryEmit(requestValidator.sanitizeMessage(it.toUiMessage())) }
            onResult(result)
        }
    }

    override fun verifyWithAppPassword(
        password: CharArray,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        scope.launch {
            val result = authUseCases.authenticateWithAppPassword(password)
            result.onFailure { _authMessage.tryEmit(requestValidator.sanitizeMessage(it.toUiMessage())) }
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
        when (val validation = requestValidator.validateRequest(activity, title)) {
            is AuthRequestValidationResult.Invalid -> {
                val msg = requestValidator.sanitizeMessage(validation.message)
                _authMessage.tryEmit(msg)
                return AppResult.failure(AppError.AuthFailed(validation.message))
            }

            AuthRequestValidationResult.Valid -> Unit
        }

        val result = authUseCases.authenticate(activity, title, subtitle)
        result.onFailure { _authMessage.tryEmit(requestValidator.sanitizeMessage(it.toUiMessage())) }
        return result
    }

    private fun launchResult(
        onResult: (AppResult<Unit>) -> Unit,
        block: suspend () -> AppResult<Unit>
    ) {
        scope.launch { onResult(block()) }
    }
}