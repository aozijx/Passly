package com.aozijx.passly.feature.shell.presentation

import com.aozijx.passly.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.DatabaseSessionFailureState
import com.aozijx.passly.domain.access.port.DatabaseSessionRecovery
import com.aozijx.passly.domain.access.port.DatabaseSessionRetryResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource
import com.aozijx.passly.presentation.shared.error.toUiErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val appearanceSettingsRepository: AppearanceSettingsRepository,
    private val interfaceSettingsRepository: InterfaceSettingsRepository,
    private val securitySettingsSource: SecuritySettingsSource,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val sessionLockController: SessionLockController,
    private val databaseSessionFailureState: DatabaseSessionFailureState,
    private val databaseSessionRecovery: DatabaseSessionRecovery,
) : ViewModel() {
    private sealed interface RetryWork {
        data object Idle : RetryWork
        class Active(val ticket: Any, val job: Job) : RetryWork
        data object Destroyed : RetryWork
    }

    private var retryWork: RetryWork = RetryWork.Idle

    private val _uiState = MutableStateFlow(AppShellUiState())
    val uiState: StateFlow<AppShellUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AppShellEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()


    init {
        observeSettings()
        observeAuthStates()
        observeDatabaseFailures()
    }

    fun onAction(action: AppShellUiAction) {
        if (retryWork == RetryWork.Destroyed) return
        when (action) {
            AppShellUiAction.ExitRecovery -> lock(
                SessionLockRequest(LockReason.RECOVERY_EXIT, LockTrigger.RECOVERY_EXIT),
            )
            AppShellUiAction.RetryDatabaseSession -> retryDatabaseSession()
        }
    }

    private fun lock(request: SessionLockRequest) {
        viewModelScope.launch { sessionLockController.lock(request) }
    }

    private fun observeAuthStates() {
        viewModelScope.launch {
            secureSessionAccessState.authenticationState
                .map { state -> state.toAppShellSessionMode() }
                .distinctUntilChanged()
                .collect { mode -> mutate(AppShellMutation.SessionChanged(mode)) }
        }

    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                appearanceSettingsRepository.appearance,
                interfaceSettingsRepository.interfaceSettings,
                securitySettingsSource.security,
            ) { appearance, interfaceSettings, securitySettings ->
                AppShellMutation.SettingsChanged(
                    appearance = appearance,
                    interfaceSettings = interfaceSettings,
                    securitySettings = securitySettings,
                )
            }
                .distinctUntilChanged()
                .collect(::mutate)
        }
    }

    private fun retryDatabaseSession() {
        if (retryWork != RetryWork.Idle || databaseSessionFailureState.databaseFailure.value == null) return
        val ticket = Any()
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val result = databaseSessionRecovery.retry()
                currentCoroutineContext().ensureActive()
                if (!isCurrentRetry(ticket)) return@launch
                when (result) {
                    DatabaseSessionRetryResult.Ready,
                    DatabaseSessionRetryResult.Unavailable -> Unit
                    is DatabaseSessionRetryResult.Failed -> if (databaseSessionFailureState.databaseFailure.value != null) {
                        emitEffect(AppShellEffect.ShowError(
                            result.cause.toUiErrorMessage(R.string.database_session_retry_failed),
                        ))
                    }
                }
            } finally {
                if (isCurrentRetry(ticket)) {
                    retryWork = RetryWork.Idle
                    mutate(AppShellMutation.DatabaseRetryFinished)
                }
            }
        }
        retryWork = RetryWork.Active(ticket, job)
        mutate(AppShellMutation.DatabaseRetryStarted)
        job.start()
    }

    private fun isCurrentRetry(ticket: Any): Boolean = (retryWork as? RetryWork.Active)?.ticket === ticket

    private fun observeDatabaseFailures() {
        viewModelScope.launch {
            databaseSessionFailureState.databaseFailure.collect { error ->
                mutate(
                    if (error == null) {
                        AppShellMutation.DatabaseAvailable
                    } else {
                        AppShellMutation.DatabaseFailed(error,
                            if (retryWork is RetryWork.Active) DatabaseRetryPhase.Retrying else DatabaseRetryPhase.Idle)
                    },
                )
            }
        }
    }

    private fun emitEffect(effect: AppShellEffect) {
        _effects.trySend(effect)
    }

    private fun mutate(mutation: AppShellMutation) {
        if (retryWork == RetryWork.Destroyed) return
        _uiState.update { state -> AppShellReducer.reduce(state, mutation) }
    }

    override fun onCleared() {
        val previous = retryWork
        retryWork = RetryWork.Destroyed
        if (previous is RetryWork.Active) previous.job.cancel()
        _uiState.update { state -> AppShellReducer.reduce(state, AppShellMutation.DatabaseRetryFinished) }
    }

}
