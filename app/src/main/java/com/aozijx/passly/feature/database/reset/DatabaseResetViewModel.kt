package com.aozijx.passly.feature.database.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.presentation.shared.error.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseResetViewModel @Inject constructor(
    private val resetDatabase: ResetDatabaseUseCase,
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
        mutate(DatabaseResetMutation.ResetStarted)
        viewModelScope.launch {
            when (val result = resetDatabase()) {
                DatabaseResetResult.Completed -> mutate(DatabaseResetMutation.ResetCompleted)
                DatabaseResetResult.Cancelled -> mutate(DatabaseResetMutation.ResetCancelled)
                DatabaseResetResult.SessionRestricted -> mutate(
                    DatabaseResetMutation.ResetFailed("当前会话不能清空 Passly 数据"),
                )
                DatabaseResetResult.AuthorizationDenied -> mutate(
                    DatabaseResetMutation.ResetFailed("身份验证失败，未清空 Passly 数据"),
                )
                is DatabaseResetResult.Failed -> mutate(
                    DatabaseResetMutation.ResetFailed(
                        result.error.toUiMessage("清空 Passly 数据失败"),
                    ),
                )
            }
        }
    }

    private fun mutate(mutation: DatabaseResetMutation) {
        _uiState.value = DatabaseResetReducer.reduce(_uiState.value, mutation)
    }
}
