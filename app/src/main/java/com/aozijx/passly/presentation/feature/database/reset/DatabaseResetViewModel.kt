package com.aozijx.passly.presentation.feature.database.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.database.reset.DatabaseResetGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseResetViewModel @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authenticationManager: AuthenticationManager,
    private val databaseResetGateway: DatabaseResetGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DatabaseResetUiState())
    val uiState: StateFlow<DatabaseResetUiState> = _uiState.asStateFlow()

    fun onAction(action: DatabaseResetUiAction) {
        when (action) {
            DatabaseResetUiAction.Reset -> reset()
        }
    }

    private fun reset() {
        if (_uiState.value.isResetting) return
        viewModelScope.launch {
            if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
                mutate(DatabaseResetMutation.ResetFailed("当前会话不能清空 Passly 数据"))
                return@launch
            }
            mutate(DatabaseResetMutation.ResetStarted)
            when (authenticationManager.authenticate(
                AuthenticationRequest(AuthenticationPurpose.CLEAR_DATABASE),
            )) {
                is AuthenticationResult.Success -> {
                    when (val result = databaseResetGateway.reset()) {
                        is AppResult.Failure -> mutate(
                            DatabaseResetMutation.ResetFailed(
                                result.error.toUiMessage("清空 Passly 数据失败"),
                            ),
                        )
                        is AppResult.Success ->
                            mutate(DatabaseResetMutation.ResetCompleted)
                    }
                }
                is AuthenticationResult.Cancelled ->
                    mutate(DatabaseResetMutation.ResetCancelled)
                is AuthenticationResult.Failure ->
                    mutate(DatabaseResetMutation.ResetFailed("身份验证失败，未清空 Passly 数据"))
            }
        }
    }

    private fun mutate(mutation: DatabaseResetMutation) {
        _uiState.value = DatabaseResetReducer.reduce(_uiState.value, mutation)
    }
}
