package com.aozijx.passly.ui.features.verification

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.crypto.memory.MemoryCleaner
import com.aozijx.passly.core.crypto.memory.SecureString
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.ui.features.common.toUiMessage
import com.aozijx.passly.ui.features.verification.contract.VerificationGateway
import com.aozijx.passly.ui.features.verification.contract.VerificationUiState
import com.aozijx.passly.ui.features.verification.internal.VerificationCoordinator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VerificationViewModel(
    application: Application,
    authUseCases: AuthUseCases
) : AndroidViewModel(application) {

    val gateway: VerificationGateway by lazy {
        VerificationCoordinator(viewModelScope, authUseCases)
    }

    val isAppPasswordEnabled: StateFlow<Boolean> = gateway.isAppPasswordEnabled

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private val _passwordSetEvent = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val passwordSetEvent: SharedFlow<Boolean> = _passwordSetEvent.asSharedFlow()

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(appPassword = SecureString.fromString(value)) }

    fun onPasswordConfirmChange(value: String) =
        _uiState.update { it.copy(appPasswordConfirm = SecureString.fromString(value)) }

    fun onShowPasswordInput() = _uiState.update { it.copy(showPasswordInput = true) }

    fun onShowSetPasswordDialog() = _uiState.update { it.copy(showSetPasswordDialog = true) }

    fun onDismissSetPasswordDialog() {
        val current = _uiState.value
        MemoryCleaner.wipe(listOf(current.appPassword, current.appPasswordConfirm))
        _uiState.update {
            it.copy(
                showSetPasswordDialog = false,
                appPassword = SecureString.EMPTY,
                appPasswordConfirm = SecureString.EMPTY
            )
        }
    }

    fun verifyWithBiometric(activity: FragmentActivity, title: String, subtitle: String) {
        if (_uiState.value.authInProgress) return
        _uiState.update { it.copy(authInProgress = true) }
        gateway.verifyWithBiometric(activity, title, subtitle) { result ->
            _uiState.update { it.copy(authInProgress = false) }
            if (result is AppResult.Failure) {
                _errorEvent.tryEmit(result.error.toUiMessage())
            }
        }
    }

    fun verifyWithAppPassword() {
        val current = _uiState.value
        if (current.authInProgress) return
        _uiState.update { it.copy(authInProgress = true) }
        gateway.verifyWithAppPassword(current.appPassword.toCharArray()) { result ->
            when (result) {
                is AppResult.Success ->
                    _uiState.update {
                        it.copy(
                            authInProgress = false,
                            appPassword = SecureString.EMPTY,
                            showPasswordInput = false
                        )
                    }

                is AppResult.Failure -> {
                    _uiState.update { it.copy(authInProgress = false) }
                    _errorEvent.tryEmit(result.error.toUiMessage())
                }
            }
        }
    }

    fun bootstrapAppPassword(validation: () -> Boolean, onComplete: (Boolean) -> Unit) {
        if (_uiState.value.authInProgress) return
        if (!validation()) return
        _uiState.update { it.copy(authInProgress = true) }
        val password = _uiState.value.appPassword.toCharArray()
        gateway.bootstrapAppPassword(password) { result ->
            MemoryCleaner.wipeCharArray(password)
            _uiState.update { it.copy(authInProgress = false) }
            val success = result.isSuccess
            if (!success) {
                _errorEvent.tryEmit(
                    (result as? AppResult.Failure)?.error?.toUiMessage() ?: ""
                )
            }
            _passwordSetEvent.tryEmit(success)
            onComplete(success)
        }
    }
}