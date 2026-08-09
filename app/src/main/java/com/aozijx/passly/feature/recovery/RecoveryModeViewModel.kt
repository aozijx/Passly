package com.aozijx.passly.feature.recovery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.core.backup.BackupStorageSupport
import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.backup.model.BackupExportOptions
import com.aozijx.passly.domain.backup.model.BackupExportRequest
import com.aozijx.passly.domain.backup.model.BackupFormats
import com.aozijx.passly.domain.backup.service.VaultBackupService
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.recovery.contract.RecoveryModeEffect
import com.aozijx.passly.feature.recovery.contract.RecoveryModeIntent
import com.aozijx.passly.feature.recovery.contract.RecoveryModeUiState
import com.aozijx.passly.security.MemoryCleaner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryModeViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val methodProvisioner: AuthenticationMethodProvisioner,
    private val backupService: VaultBackupService,
    private val storageSupport: BackupStorageSupport,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoveryModeUiState())
    val uiState: StateFlow<RecoveryModeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<RecoveryModeEffect>()
    val effect = _effect.asSharedFlow()

    fun onIntent(intent: RecoveryModeIntent) {
        when (intent) {
            RecoveryModeIntent.SetPasswordClicked -> showSetPasswordDialog()
            is RecoveryModeIntent.NewPasswordChanged ->
                _uiState.update { it.copy(newPassword = intent.value, passwordSetupError = null) }
            is RecoveryModeIntent.ConfirmPasswordChanged ->
                _uiState.update {
                    it.copy(confirmPassword = intent.value, passwordSetupError = null)
                }
            RecoveryModeIntent.SubmitNewPassword -> submitNewPassword()
            RecoveryModeIntent.ReconfigureBiometricClicked -> reconfigureBiometric()
            RecoveryModeIntent.ExportClicked -> prepareExport()
            is RecoveryModeIntent.ExportPasswordChanged ->
                _uiState.update { it.copy(exportPassword = intent.value, exportError = null) }
            is RecoveryModeIntent.IncludeIconsChanged ->
                _uiState.update { it.copy(includeIcons = intent.include) }
            is RecoveryModeIntent.IncludeAttachmentsChanged ->
                _uiState.update { it.copy(includeAttachments = intent.include) }
            is RecoveryModeIntent.IncludeDeletedChanged ->
                _uiState.update { it.copy(includeDeleted = intent.include) }
            RecoveryModeIntent.SubmitExport -> submitExport()
            is RecoveryModeIntent.ExportTargetPicked -> handleExportTarget(intent.uri)
            RecoveryModeIntent.ExitClicked -> exitRecovery()
            RecoveryModeIntent.DismissSheet -> dismissSheet()
        }
    }

    fun buildExportFileName(): String = storageSupport.buildBackupFileName("passly")

    // --- Set Password ---

    private fun showSetPasswordDialog() {
        if (!ensureRecoveryMode()) return
        _uiState.update { it.copy(showSetPasswordDialog = true, passwordSetupError = null) }
    }

    private fun submitNewPassword() {
        val state = _uiState.value
        if (!ensureRecoveryMode()) return
        val password = state.newPassword.toCharArray()
        val confirm = state.confirmPassword.toCharArray()

        if (password.isEmpty() || !password.contentEquals(confirm)) {
            MemoryCleaner.wipeCharArray(password)
            MemoryCleaner.wipeCharArray(confirm)
            _uiState.update { it.copy(passwordSetupError = "密码不匹配或为空") }
            return
        }

        _uiState.update { it.copy(isSettingPassword = true, passwordSetupError = null) }
        viewModelScope.launch {
            try {
                when (val result = methodProvisioner.setAppPassword(password)) {
                    is AuthenticationResult.Success ->
                        _uiState.update {
                            it.copy(
                                showSetPasswordDialog = false,
                                newPassword = "",
                                confirmPassword = "",
                                isSettingPassword = false,
                                passwordSetupError = null
                            )
                        }.also {
                            _effect.emit(RecoveryModeEffect.PasswordResetCompleted)
                        }

                    is AuthenticationResult.Cancelled ->
                        _uiState.update { it.copy(isSettingPassword = false) }

                    is AuthenticationResult.Failure ->
                        _uiState.update {
                            it.copy(
                                isSettingPassword = false,
                                passwordSetupError = "设置密码失败"
                            )
                        }
                }
            } finally {
                MemoryCleaner.wipeCharArray(password)
                MemoryCleaner.wipeCharArray(confirm)
            }
        }
    }

    // --- Biometric ---

    private fun reconfigureBiometric() {
        if (!ensureRecoveryMode()) return
        _uiState.update { it.copy(biometricResult = null, isReconfiguringBiometric = true) }
        viewModelScope.launch {
            val result = methodProvisioner.rotateBiometricPolicy(invalidateOnEnrollment = true)
            _uiState.update {
                it.copy(
                    biometricResult = result is AuthenticationResult.Success,
                    isReconfiguringBiometric = false
                )
            }
        }
    }

    // --- Export ---

    private fun prepareExport() {
        if (!ensureRecoveryMode()) return
        _uiState.update {
            it.copy(
                showExportOptions = true,
                exportError = null
            )
        }
    }

    private fun submitExport() {
        val state = _uiState.value
        if (!ensureRecoveryMode()) return
        if (!state.canSubmitExport) return
        viewModelScope.launch {
            _effect.emit(RecoveryModeEffect.PickExportTarget(buildExportFileName()))
        }
    }

    private fun handleExportTarget(uri: Uri?) {
        if (!ensureRecoveryMode()) return
        if (uri == null) {
            dismissSheet()
            return
        }
        _uiState.update { it.copy(isExporting = true, showExportOptions = false) }
        viewModelScope.launch {
            try {
                val authResult = authenticationManager.authenticate(
                    AuthenticationRequest(purpose = AuthenticationPurpose.RECOVERY_EXPORT)
                )
                if (authResult !is AuthenticationResult.Success) {
                    _uiState.update { it.copy(isExporting = false) }
                    return@launch
                }

                val state = _uiState.value
                val password = state.exportPassword.toCharArray()
                try {
                    when (val result = backupService.export(state.toExportRequest(uri, password))) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isExporting = false,
                                    exportPassword = "",
                                    exportError = null
                                )
                            }
                            _effect.emit(RecoveryModeEffect.ExportCompleted)
                        }

                        is AppResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isExporting = false,
                                    exportError = result.error.toUiMessage("备份导出失败")
                                )
                            }
                        }
                    }
                } finally {
                    password.fill('\u0000')
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportError = AppError.fromThrowable(e).toUiMessage("备份导出失败")
                    )
                }
            }
        }
    }

    private fun RecoveryModeUiState.toExportRequest(
        uri: Uri,
        password: CharArray
    ): BackupExportRequest = BackupExportRequest(
        targetUri = uri.toString(),
        format = BackupFormats.PASSLY_ENCRYPTED,
        password = password,
        options = BackupExportOptions(
            includeIcons = includeIcons,
            includeAttachments = includeAttachments,
            includeDeleted = includeDeleted,
            includedEntryTypes = EntryType.entries.toSet()
        )
    )

    private fun exitRecovery() {
        viewModelScope.launch {
            _effect.emit(RecoveryModeEffect.ExitRecovery)
        }
    }

    private fun dismissSheet() {
        _uiState.update {
            it.copy(
                showExportOptions = false,
                exportError = null
            )
        }
    }

    private fun ensureRecoveryMode(): Boolean {
        val recoveryMode = authenticationManager.state.value is AuthenticationState.RecoveryMode
        if (recoveryMode) return true
        _uiState.update {
            it.copy(
                showSetPasswordDialog = false,
                showExportOptions = false,
                isSettingPassword = false,
                isReconfiguringBiometric = false,
                isExporting = false,
                passwordSetupError = "当前不在恢复模式",
                exportError = "当前不在恢复模式"
            )
        }
        return false
    }
}
