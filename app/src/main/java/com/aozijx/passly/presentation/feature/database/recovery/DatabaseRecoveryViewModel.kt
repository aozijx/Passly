package com.aozijx.passly.presentation.feature.database.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.app.database.DatabaseLifecycleUseCases
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.database.recovery.DatabaseRecoveryGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseRecoveryViewModel @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authenticationManager: AuthenticationManager,
    private val databaseRecoveryGateway: DatabaseRecoveryGateway,
    private val databaseLifecycleUseCases: DatabaseLifecycleUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DatabaseRecoveryUiState())
    val uiState: StateFlow<DatabaseRecoveryUiState> = _uiState.asStateFlow()

    init {
        loadRecoveryPackages()
    }

    fun onAction(action: DatabaseRecoveryUiAction) {
        when (action) {
            DatabaseRecoveryUiAction.RefreshRecoveryPackages -> loadRecoveryPackages()
            is DatabaseRecoveryUiAction.ScanRecoveryPackage ->
                scanRecoveryPackage(action.packageId)
            is DatabaseRecoveryUiAction.ToggleRecoveryType ->
                mutate(DatabaseRecoveryMutation.RecoveryTypeToggled(action.entryType))
            is DatabaseRecoveryUiAction.RestoreRecoveryPackage ->
                restoreRecoveryPackage(action.packageId)
            is DatabaseRecoveryUiAction.DeleteRecoveryPackage ->
                deleteRecoveryPackage(action.packageId)
            DatabaseRecoveryUiAction.ClearDatabase -> clearDatabase()
            DatabaseRecoveryUiAction.ClearRecoveryResult ->
                mutate(DatabaseRecoveryMutation.RecoveryResultCleared)
        }
    }

    private fun clearDatabase() {
        if (_uiState.value.isClearingDatabase) return
        viewModelScope.launch {
            if (!requireRecoveryAccess()) return@launch
            mutate(DatabaseRecoveryMutation.DatabaseClearStarted)
            when (
                authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.CLEAR_DATABASE),
                )
            ) {
                is AuthenticationResult.Success -> {
                    val outcome = databaseLifecycleUseCases.clearAndReinitialize()
                    if (outcome.success) {
                        mutate(DatabaseRecoveryMutation.DatabaseClearCompleted)
                        refreshRecoveryPackagesAfterOperation()
                    } else {
                        mutate(DatabaseRecoveryMutation.RecoveryOperationFailed("清除数据库失败"))
                    }
                }
                is AuthenticationResult.Cancelled ->
                    mutate(DatabaseRecoveryMutation.RecoveryResultCleared)
                is AuthenticationResult.Failure ->
                    mutate(DatabaseRecoveryMutation.RecoveryOperationFailed("身份验证失败，数据库未清除"))
            }
        }
    }

    private fun loadRecoveryPackages() {
        viewModelScope.launch {
            if (!requireRecoveryAccess()) return@launch
            runCatching { databaseRecoveryGateway.packages() }
                .onSuccess {
                    mutate(DatabaseRecoveryMutation.RecoveryPackagesLoaded(it))
                }
                .onFailure {
                    mutate(
                        DatabaseRecoveryMutation.RecoveryOperationFailed(
                            it.toUiMessage("无法读取数据库恢复包"),
                        ),
                    )
                }
        }
    }

    private fun scanRecoveryPackage(packageId: String) {
        if (_uiState.value.isRecoveryBusy) return
        viewModelScope.launch {
            if (!requireRecoveryAccess()) return@launch
            mutate(DatabaseRecoveryMutation.RecoveryOperationStarted(packageId))
            runCatching { databaseRecoveryGateway.scan(packageId) }
                .onSuccess {
                    mutate(DatabaseRecoveryMutation.RecoveryScanCompleted(it))
                    refreshRecoveryPackagesAfterOperation()
                }
                .onFailure {
                    mutate(
                        DatabaseRecoveryMutation.RecoveryOperationFailed(
                            it.toUiMessage("恢复包预检失败"),
                        ),
                    )
                    refreshRecoveryPackagesAfterOperation()
                }
        }
    }

    private fun restoreRecoveryPackage(packageId: String) {
        val state = _uiState.value
        if (state.isRecoveryBusy || state.recoveryScan?.packageId != packageId ||
            state.selectedRecoveryTypes.isEmpty()
        ) return
        viewModelScope.launch {
            if (!requireRecoveryAccess()) return@launch
            when (
                authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.RESTORE_DATABASE),
                )
            ) {
                is AuthenticationResult.Success -> {
                    if (!requireRecoveryAccess()) return@launch
                    mutate(DatabaseRecoveryMutation.RecoveryOperationStarted(packageId))
                    runCatching {
                        databaseRecoveryGateway.recover(packageId, state.selectedRecoveryTypes)
                    }.onSuccess {
                        mutate(DatabaseRecoveryMutation.RecoveryRestoreCompleted(it))
                        refreshRecoveryPackagesAfterOperation()
                    }.onFailure {
                        mutate(
                            DatabaseRecoveryMutation.RecoveryOperationFailed(
                                it.toUiMessage("数据库恢复失败"),
                            ),
                        )
                    }
                }
                is AuthenticationResult.Cancelled -> Unit
                is AuthenticationResult.Failure -> mutate(
                    DatabaseRecoveryMutation.RecoveryOperationFailed("身份验证失败"),
                )
            }
        }
    }

    private fun deleteRecoveryPackage(packageId: String) {
        if (_uiState.value.isRecoveryBusy) return
        viewModelScope.launch {
            if (!requireRecoveryAccess()) return@launch
            when (
                authenticationManager.authenticate(
                    AuthenticationRequest(AuthenticationPurpose.RESTORE_DATABASE),
                )
            ) {
                is AuthenticationResult.Success -> {
                    mutate(DatabaseRecoveryMutation.RecoveryOperationStarted(packageId))
                    runCatching { databaseRecoveryGateway.delete(packageId) }
                        .onSuccess {
                            mutate(DatabaseRecoveryMutation.RecoveryResultCleared)
                            refreshRecoveryPackagesAfterOperation()
                        }
                        .onFailure {
                            mutate(
                                DatabaseRecoveryMutation.RecoveryOperationFailed(
                                    it.toUiMessage("无法删除数据库恢复包"),
                                ),
                            )
                        }
                }
                is AuthenticationResult.Cancelled -> Unit
                is AuthenticationResult.Failure -> mutate(
                    DatabaseRecoveryMutation.RecoveryOperationFailed("身份验证失败"),
                )
            }
        }
    }

    private suspend fun refreshRecoveryPackagesAfterOperation() {
        runCatching { databaseRecoveryGateway.packages() }
            .onSuccess { mutate(DatabaseRecoveryMutation.RecoveryPackagesLoaded(it)) }
    }

    private fun requireRecoveryAccess(): Boolean {
        if (secureSessionAccessState.hasFullSecureSessionAccess()) return true
        mutate(
            DatabaseRecoveryMutation.RecoveryOperationFailed(
                "当前会话不能访问数据库恢复包",
            ),
        )
        return false
    }

    private fun mutate(mutation: DatabaseRecoveryMutation) {
        _uiState.value = DatabaseRecoveryReducer.reduce(_uiState.value, mutation)
    }
}
