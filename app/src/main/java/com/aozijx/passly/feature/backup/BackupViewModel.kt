package com.aozijx.passly.feature.backup

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.BackupFailed
import com.aozijx.passly.core.error.ErrorLayer
import com.aozijx.passly.core.error.backup.BackupException
import com.aozijx.passly.core.error.fromThrowable
import com.aozijx.passly.core.util.PlainExportTokenManager
import com.aozijx.passly.domain.usecase.backup.BackupUseCases
import com.aozijx.passly.domain.usecase.settings.DeviceSettingsUseCases
import com.aozijx.passly.feature.backup.contract.BackupEffect
import com.aozijx.passly.feature.backup.contract.BackupIntent
import com.aozijx.passly.feature.backup.contract.BackupOperationStatus
import com.aozijx.passly.feature.backup.contract.BackupUiState
import com.aozijx.passly.feature.backup.storage.BackupExportStorageSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val deviceSettingsUseCases: DeviceSettingsUseCases,
    private val backupUseCases: BackupUseCases,
    private val storageSupport: BackupExportStorageSupport,
    private val plainExportTokenManager: PlainExportTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _effect = Channel<BackupEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: BackupIntent) {
        when (intent) {
            is BackupIntent.CheckDirectoryPermission -> checkDirectoryPermission(intent.uri)
            is BackupIntent.SetBackupDirectoryUri -> setBackupDirectoryUri(intent.uri)
            BackupIntent.ClearBackupDirectoryUri -> clearBackupDirectoryUri()
            is BackupIntent.StartExport -> startExport(
                intent.uri,
                intent.fileNameHint,
                intent.allowFallback
            )

            is BackupIntent.StartImport -> startImport(intent.uri)
            is BackupIntent.UpdatePassword -> updatePassword(intent.password)
            is BackupIntent.UpdateImportMode -> updateImportMode(intent.mode)
            is BackupIntent.UpdateIncludeImages -> updateIncludeImages(intent.include)
            BackupIntent.DismissPasswordDialog -> dismissPasswordDialog()
            BackupIntent.ResetBackupStatus -> resetBackupStatus()
            is BackupIntent.TryStartExportInConfiguredDirectory -> tryStartExportInConfiguredDirectory(
                intent.directoryUri
            )

            BackupIntent.ProcessBackupAction -> processBackupAction()
            BackupIntent.ExecuteBackup -> executeBackup()
            is BackupIntent.ExportPlainBackup -> exportPlainBackup(intent.dirUri)
            is BackupIntent.ExportPlainBackupToUri -> exportPlainBackupToUri(intent.uri)
            BackupIntent.ExportEmergencyBackup -> exportEmergencyBackup()
            BackupIntent.IssuePlainExportToken -> plainExportTokenManager.issueToken()
        }
    }

    // --- 目录权限 ---

    private fun checkDirectoryPermission(uri: String?) {
        if (uri.isNullOrBlank()) {
            _uiState.update { it.copy(status = BackupOperationStatus.Failure, error = null) }
            _effect.trySend(BackupEffect.ShowError(BackupFailed("")))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(status = BackupOperationStatus.Loading) }
            val result = backupUseCases.checkDirectoryWritable(uri)
            _uiState.update { state ->
                state.copy(
                    status = when (result) {
                        is AppResult.Success -> BackupOperationStatus.Success(BackupOperationStatus.OperationType.PERMISSION_CHECK)
                        is AppResult.Failure -> BackupOperationStatus.Failure
                    },
                    error = (result as? AppResult.Failure)?.error
                )
            }
        }
    }

    private fun setBackupDirectoryUri(uri: String) {
        viewModelScope.launch { deviceSettingsUseCases.setBackupDirectoryUri(uri) }
    }

    private fun clearBackupDirectoryUri() {
        viewModelScope.launch { deviceSettingsUseCases.clearBackupDirectoryUri() }
    }

    // --- 导出/导入流程 ---

    private fun startExport(uri: Uri, fileNameHint: String?, allowFallback: Boolean) {
        _uiState.update {
            it.copy(
                backupUri = uri,
                isExporting = true,
                showPasswordDialog = true,
                pendingExportFileName = fileNameHint ?: storageSupport.buildBackupFileName(),
                pendingExportAllowFallback = allowFallback
            )
        }
    }

    private fun startImport(uri: Uri) {
        _uiState.update {
            it.copy(
                backupUri = uri,
                isExporting = false,
                showPasswordDialog = true,
                pendingExportFileName = null,
                pendingExportAllowFallback = false
            )
        }
    }

    private fun updatePassword(password: String) {
        _uiState.update { it.copy(backupPassword = password) }
    }

    private fun updateImportMode(mode: com.aozijx.passly.domain.model.backup.ImportMode) {
        _uiState.update { it.copy(importMode = mode) }
    }

    private fun updateIncludeImages(include: Boolean) {
        _uiState.update { it.copy(includeImages = include) }
    }

    private fun dismissPasswordDialog() {
        _uiState.update {
            it.copy(
                showPasswordDialog = false,
                backupPassword = "",
                backupUri = null
            )
        }
    }

    private fun resetBackupStatus() {
        _uiState.update { it.copy(status = BackupOperationStatus.Idle, error = null) }
    }

    private fun tryStartExportInConfiguredDirectory(directoryUri: String?) {
        if (directoryUri.isNullOrBlank()) return
        startExport(
            directoryUri.toUri(),
            fileNameHint = storageSupport.buildBackupFileName(),
            allowFallback = true
        )
    }

    // --- 核心备份/恢复操作 ---

    /// 第一步：验证状态，通过 Effect 请求认证
    private fun processBackupAction() {
        val currentState = _uiState.value
        if (currentState.backupUri == null) return
        _effect.trySend(BackupEffect.RequestAuth)
    }

    /// 第二步：认证成功后执行实际备份/恢复
    private fun executeBackup() {
        val currentState = _uiState.value
        val targetUri = currentState.backupUri ?: return
        val password = currentState.backupPassword.toCharArray()

        viewModelScope.launch {
            _uiState.update { it.copy(status = BackupOperationStatus.Loading) }
            try {
                val finalUri = resolveFinalUri(currentState, targetUri)
                val outcome = performBackupOperation(currentState, finalUri, password)

                when (outcome) {
                    is AppResult.Success -> handleSuccess(currentState, finalUri)
                    is AppResult.Failure -> handleFailure(outcome.error, currentState, finalUri)
                }
                dismissPasswordDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(status = BackupOperationStatus.Failure, error = null) }
                _effect.trySend(BackupEffect.ShowError(AppError.fromThrowable(e, ErrorLayer.UI)))
            } finally {
                password.fill('\u0000')
            }
        }
    }

    private suspend fun resolveFinalUri(currentState: BackupUiState, targetUri: Uri): Uri {
        if (!currentState.isExporting || !currentState.pendingExportAllowFallback) return targetUri
        val createResult = storageSupport.createNamedExportTarget(
            targetUri.toString(),
            currentState.pendingExportFileName ?: storageSupport.buildBackupFileName()
        )
        if (createResult.isFailure) throw BackupException.StoragePermissionDenied()
        return createResult.getOrThrow().fileUri
    }

    private suspend fun performBackupOperation(
        currentState: BackupUiState,
        finalUri: Uri,
        password: CharArray
    ): AppResult<Unit> {
        return if (currentState.isExporting) {
            backupUseCases.exportBackup(finalUri.toString(), password, currentState.includeImages)
        } else {
            backupUseCases.importBackup(finalUri.toString(), password, currentState.importMode)
        }
    }

    private suspend fun handleSuccess(oldState: BackupUiState, finalUri: Uri) {
        if (oldState.isExporting) {
            oldState.pendingExportFileName?.let {
                deviceSettingsUseCases.setLastBackupExportFileName(it)
            }
        } else {
            _effect.send(BackupEffect.StartImportSyncService)
        }
        val type = if (oldState.isExporting) BackupOperationStatus.OperationType.EXPORT
        else BackupOperationStatus.OperationType.IMPORT
        _uiState.update { it.copy(status = BackupOperationStatus.Success(type), error = null) }
    }

    private fun handleFailure(error: AppError, oldState: BackupUiState, finalUri: Uri) {
        if (oldState.isExporting && oldState.pendingExportAllowFallback) {
            storageSupport.deleteDocument(finalUri)
        }
        _uiState.update { it.copy(status = BackupOperationStatus.Failure, error = error) }
        _effect.trySend(BackupEffect.ShowError(error))
    }

    // --- 明文导出 ---

    private fun exportPlainBackup(dirUri: String?) {
        if (!plainExportTokenManager.isTokenValid()) {
            _uiState.update { it.copy(status = BackupOperationStatus.Failure, error = null) }
            return
        }

        val fileName = "Passly_Plain_Backup_${System.currentTimeMillis()}.json"
        if (!dirUri.isNullOrBlank()) {
            viewModelScope.launch {
                _uiState.update { it.copy(status = BackupOperationStatus.Loading) }
                val targetResult = withContext(Dispatchers.IO) {
                    storageSupport.createNamedExportTarget(dirUri, fileName)
                }
                targetResult.fold(
                    onSuccess = { exportPlainBackupToUri(it.fileUri) },
                    onFailure = {
                        _uiState.update { it.copy(status = BackupOperationStatus.Idle) }
                        _effect.send(BackupEffect.ShowPlainExportPicker(fileName))
                    }
                )
            }
        } else {
            viewModelScope.launch { _effect.send(BackupEffect.ShowPlainExportPicker(fileName)) }
        }
    }

    private fun exportPlainBackupToUri(uri: Uri) {
        if (!plainExportTokenManager.consumeToken()) {
            _uiState.update { it.copy(status = BackupOperationStatus.Failure, error = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(status = BackupOperationStatus.Loading) }
            val result = backupUseCases.exportPlainBackup(uri.toString())
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            status = BackupOperationStatus.Success(
                                BackupOperationStatus.OperationType.PLAIN_EXPORT
                            ), error = null
                        )
                    }
                }

                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            status = BackupOperationStatus.Failure,
                            error = result.error
                        )
                    }
                    _effect.trySend(BackupEffect.ShowError(result.error))
                }
            }
        }
    }

    // --- 紧急备份 ---

    private fun exportEmergencyBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = BackupOperationStatus.Loading) }
            val result = backupUseCases.exportEmergencyBackup()
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            emergencyBackupFile = result.data,
                            status = BackupOperationStatus.Success(BackupOperationStatus.OperationType.EMERGENCY_EXPORT),
                            error = null
                        )
                    }
                }

                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            status = BackupOperationStatus.Failure,
                            error = result.error
                        )
                    }
                    _effect.trySend(BackupEffect.ShowError(result.error))
                }
            }
        }
    }

    fun buildBackupFileName(): String = storageSupport.buildBackupFileName()
}