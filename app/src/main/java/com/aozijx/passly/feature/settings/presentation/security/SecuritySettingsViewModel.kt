package com.aozijx.passly.feature.settings.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.BiometricConfiguration
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.presentation.shared.error.toUiErrorMessage
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import com.aozijx.passly.feature.settings.security.SecurityAuthenticationSettingsInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val authenticationSettings: SecurityAuthenticationSettingsInteractor,
    private val settingsRepository: SecuritySettingsRepository
) : ViewModel() {
    private enum class AuthenticationWork { Idle, Running, Destroyed }
    private data class PageError(val owner: Any, val message: UiErrorMessage)

    private val authenticationWork = MutableStateFlow(AuthenticationWork.Idle)
    private val errorChannel = Channel<PageError>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()
        .filter { activePageOwner === it.owner }
        .map { it.message }
    private var activePageOwner: Any? = null

    val uiState: StateFlow<SecuritySettingsUiState> = combine(
        settingsRepository.security,
        authenticationSettings.isBiometricEnabled,
        authenticationWork,
        authenticationSettings.biometricConfiguration,
    ) { security, isBiometricEnabled, work, configuration ->
        SecuritySettingsUiState.Ready(
            lockTimeout = security.lockTimeout,
            isInvalidateKeyOnBioChange = configuration.invalidateOnEnrollmentForNextKey,
            isLockOnBackground = security.isLockOnBackground,
            isBiometricEnabled = isBiometricEnabled && configuration is BiometricConfiguration.Configured,
            isAuthenticationChangeInProgress = work == AuthenticationWork.Running,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SecuritySettingsUiState.Loading,
    )

    fun onAction(action: SecuritySettingsAction) {
        when (action) {
            is SecuritySettingsAction.SetLockTimeout -> saveSetting {
                settingsRepository.setLockTimeout(action.timeoutMs)
            }

            is SecuritySettingsAction.ToggleLockOnBackground -> saveSetting {
                settingsRepository.setLockOnBackground(action.enabled)
            }

            is SecuritySettingsAction.SetBiometricEnabled ->
                setBiometricEnabled(action.enabled)

            is SecuritySettingsAction.SetInvalidateKeyOnBiometricChange ->
                setKeyInvalidationPolicy(action.enabled)

        }
    }

    private fun setKeyInvalidationPolicy(enabled: Boolean) {
        val settings = uiState.value as? SecuritySettingsUiState.Ready ?: return
        if (!settings.isBiometricEnabled) return
        changeAuthentication {
            authenticationSettings.setKeyInvalidationPolicy(enabled)
        }
    }

    private fun setBiometricEnabled(enabled: Boolean) {
        if (uiState.value !is SecuritySettingsUiState.Ready) return
        changeAuthentication {
            authenticationSettings.setBiometricEnabled(
                enabled = enabled,
            )
        }
    }

    private fun changeAuthentication(change: suspend () -> AuthenticationResult) {
        val owner = activePageOwner ?: return
        if (authenticationWork.value != AuthenticationWork.Idle) return
        authenticationWork.value = AuthenticationWork.Running
        viewModelScope.launch {
            try {
                val result = change()
                currentCoroutineContext().ensureActive()
                when (result) {
                    is AuthenticationResult.Success, is AuthenticationResult.Cancelled -> Unit
                    is AuthenticationResult.Failure -> emitError(owner,
                        UiErrorMessage(when (result.failure.code) {
                            AuthenticationFailureCode.LAST_METHOD_REQUIRED -> R.string.settings_auth_last_method_required
                            AuthenticationFailureCode.SESSION_MODE_RESTRICTED -> R.string.settings_security_session_restricted
                            else -> R.string.settings_auth_operation_failed
                        }),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                emitError(owner, error.toUiErrorMessage(R.string.settings_auth_operation_failed))
            } finally {
                if (authenticationWork.value == AuthenticationWork.Running) {
                    authenticationWork.value = AuthenticationWork.Idle
                }
            }
        }
    }

    private fun saveSetting(write: suspend () -> Unit) {
        val owner = activePageOwner ?: return
        viewModelScope.launch {
            writePreferenceWithFeedback(
                failureRes = R.string.settings_security_save_failed,
                onFailure = { emitError(owner, it) },
                write = write,
            )
        }
    }

    fun onPageEntered(): Any {
        val owner = Any()
        activePageOwner = owner
        return owner
    }

    fun onPageExited(owner: Any) {
        if (activePageOwner === owner) activePageOwner = null
    }

    private fun emitError(owner: Any, error: UiErrorMessage) {
        if (activePageOwner === owner) errorChannel.trySend(PageError(owner, error))
    }

    override fun onCleared() {
        authenticationWork.value = AuthenticationWork.Destroyed
        super.onCleared()
    }
}
