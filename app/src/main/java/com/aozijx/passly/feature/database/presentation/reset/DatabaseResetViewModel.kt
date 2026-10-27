package com.aozijx.passly.feature.database.presentation.reset

import com.aozijx.passly.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.feature.database.reset.DatabaseResetResult
import com.aozijx.passly.feature.database.reset.ResetDatabaseUseCase
import com.aozijx.passly.presentation.shared.error.toUiErrorMessage
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
        if (_uiState.value.phase == DatabaseResetPhase.Resetting) return
        mutate(DatabaseResetMutation.ResetStarted)
        viewModelScope.launch {
            when (val result = resetDatabase()) {
                DatabaseResetResult.Completed -> mutate(DatabaseResetMutation.ResetCompleted)
                DatabaseResetResult.Cancelled -> mutate(DatabaseResetMutation.ResetCancelled)
                DatabaseResetResult.SessionRestricted -> mutate(
                    DatabaseResetMutation.ResetFailed(DatabaseResetFailure.SessionRestricted),
                )
                DatabaseResetResult.AuthorizationDenied -> mutate(
                    DatabaseResetMutation.ResetFailed(DatabaseResetFailure.AuthorizationDenied),
                )
                is DatabaseResetResult.Failed -> mutate(
                    DatabaseResetMutation.ResetFailed(
                        DatabaseResetFailure.Unexpected(
                            result.error.toUiErrorMessage(R.string.database_reset_error_failed),
                        ),
                    ),
                )
            }
        }
    }

    private fun mutate(mutation: DatabaseResetMutation) {
        _uiState.value = DatabaseResetReducer.reduce(_uiState.value, mutation)
    }
}
