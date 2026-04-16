package com.aozijx.passly.features.main

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.features.auth.AuthCoordinator
import com.aozijx.passly.features.auth.internal.AuthValidationSupport
import com.aozijx.passly.features.main.contract.MainEffect
import com.aozijx.passly.features.main.contract.MainIntent
import com.aozijx.passly.features.main.contract.MainUiState
import com.aozijx.passly.features.main.internal.MainDatabaseInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = getApplication<Application>()
    private val systemSettingsUseCases = AppContainer.domain.systemSettingsUseCases
    private val securitySettingsUseCases = AppContainer.domain.securitySettingsUseCases
    private val backupUseCases = AppContainer.domain.backupUseCases

    private val validationSupport = AuthValidationSupport()
    private val databaseInitializer = MainDatabaseInitializer()

    // 核心重构：引入 AuthCoordinator 接管认证与自动锁定逻辑
    val auth = AuthCoordinator(scope = viewModelScope, validationSupport = validationSupport)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<MainEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<MainEffect> = _effects.asSharedFlow()

    init {
        observeSettings()
        initializeDatabase()
        observeAuthStates()
    }

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.Authenticate ->
                auth.authenticate(
                    activity = intent.activity,
                    title = intent.title,
                    subtitle = intent.subtitle,
                    onError = intent.onError,
                    onSuccess = intent.onSuccess
                )
            MainIntent.Authorize -> auth.authorize()
            MainIntent.Lock -> auth.lock()
            MainIntent.UpdateInteraction -> auth.onUserInteraction()
            MainIntent.CheckAndLock -> auth.checkAndLock()
            MainIntent.RetryDatabaseInitialization -> initializeDatabase()
            is MainIntent.ExportEmergencyBackup -> exportEmergencyBackup()
            is MainIntent.ExportPlainBackup -> exportPlainBackup(intent.context, intent.dirUri)
            is MainIntent.ExportPlainBackupToUri -> exportPlainBackupToUri(intent.uri)
        }
    }

    val isAuthorized: Boolean
        get() = auth.isAuthorized.value

    /**
     * 保持对外的 authenticate 兼容性接口，内部转发给 auth 协调器。
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onError: ((String) -> Unit)? = null,
        onSuccess: () -> Unit
    ) {
        auth.authenticate(activity, title, subtitle, onSuccess, onError)
    }

    private fun observeAuthStates() {
        // 同步授权状态到 UI
        viewModelScope.launch {
            auth.isAuthorized.collect { authorized ->
                _uiState.update { it.copy(isAuthorized = authorized) }
                if (authorized) {
                    emitEffect(MainEffect.NavigateToVault)
                }
            }
        }

        // 监听来自 Auth 模块的消息
        viewModelScope.launch {
            auth.authMessage.collect { message ->
                _uiState.update { it.copy(validationMessage = message) }
                emitEffect(MainEffect.ShowError(message))
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            systemSettingsUseCases.isDarkMode.collect { isDarkMode ->
                _uiState.update { it.copy(isDarkMode = isDarkMode) }
            }
        }

        viewModelScope.launch {
            systemSettingsUseCases.isDynamicColor.collect { isDynamicColor ->
                _uiState.update { it.copy(isDynamicColor = isDynamicColor) }
            }
        }

        viewModelScope.launch {
            securitySettingsUseCases.lockTimeout.collect { lockTimeout ->
                val decision = auth.applyTimeout(lockTimeout)
                _uiState.update {
                    it.copy(
                        lockTimeoutMs = decision.timeoutMs,
                        validationMessage = if (decision.timeoutAdjusted) {
                            "自动锁定时长过小，已自动调整为 5 秒"
                        } else {
                            it.validationMessage
                        }
                    )
                }

                if (decision.timeoutAdjusted) {
                    emitEffect(MainEffect.ShowToast("自动锁定时长已调整为最短 5 秒"))
                }
            }
        }
    }

    fun exportEmergencyBackup() {
        viewModelScope.launch {
            val exportResult = withContext(Dispatchers.IO) {
                backupUseCases.exportEmergencyBackup()
            }

            exportResult.fold(
                onSuccess = { file ->
                    _uiState.update { it.copy(emergencyBackupFile = file) }
                    emitEffect(MainEffect.ShowToast("紧急备份已导出: ${file.name}"))
                },
                onFailure = { e ->
                    val message = validationSupport.sanitizeMessage(e.message)
                    emitEffect(MainEffect.ShowError("紧急备份导出失败: $message"))
                }
            )
        }
    }

    fun exportPlainBackup(context: Context, dirUri: String? = null) {
        if (!isAuthorized) {
            emitEffect(MainEffect.ShowError("请先完成解锁验证后再导出明文备份"))
            return
        }

        val fileName = "Passly_Plain_Backup_${System.currentTimeMillis()}.json"
        if (!dirUri.isNullOrBlank()) {
            viewModelScope.launch {
                val targetResult = withContext(Dispatchers.IO) {
                    BackupExportStorageSupport.createNamedExportTarget(
                        context.applicationContext, dirUri, fileName
                    )
                }
                targetResult.fold(
                    onSuccess = { target -> exportPlainBackupToUri(target.fileUri) },
                    onFailure = {
                        emitEffect(MainEffect.ShowPlainExportPicker(fileName))
                    }
                )
            }
        } else {
            emitEffect(MainEffect.ShowPlainExportPicker(fileName))
        }
    }

    fun exportPlainBackupToUri(uri: Uri) {
        if (!isAuthorized) {
            emitEffect(MainEffect.ShowError("请先完成解锁验证后再导出明助备份"))
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                backupUseCases.exportPlainBackup(uri)
            }
            result.fold(
                onSuccess = { emitEffect(MainEffect.ShowToast("明文备份已导出")) },
                onFailure = { e ->
                    val message = validationSupport.sanitizeMessage(e.message)
                    emitEffect(MainEffect.ShowError("明文备份导出失败: $message"))
                }
            )
        }
    }

    private fun initializeDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDatabaseInitializing = true, databaseError = null) }
            val initResult = databaseInitializer.initialize(appContext)
            _uiState.update {
                it.copy(
                    isDatabaseInitializing = false,
                    databaseError = initResult.error
                )
            }

            initResult.error?.let { error ->
                emitEffect(MainEffect.ShowError(formatDatabaseError(error)))
            }
        }
    }

    private fun formatDatabaseError(error: Throwable): String {
        return "数据库错误: ${validationSupport.sanitizeMessage(error.message)}"
    }

    private fun emitEffect(effect: MainEffect) {
        _effects.tryEmit(effect)
    }
}