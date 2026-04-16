package com.aozijx.passly.features.main

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.core.crypto.BiometricHelper
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.features.main.contract.MainEffect
import com.aozijx.passly.features.main.contract.MainIntent
import com.aozijx.passly.features.main.contract.MainUiState
import com.aozijx.passly.features.main.internal.MainAutoLockCoordinator
import com.aozijx.passly.features.main.internal.MainDatabaseInitializer
import com.aozijx.passly.features.main.internal.MainValidationResult
import com.aozijx.passly.features.main.internal.MainValidationSupport
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

    private val validationSupport = MainValidationSupport()
    private val databaseInitializer = MainDatabaseInitializer()
    
    // Coordinator 现在内部封装了 Scheduler，直接传递 scope 即可
    private val autoLockCoordinator = MainAutoLockCoordinator(
        scope = viewModelScope,
        validationSupport = validationSupport
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<MainEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<MainEffect> = _effects.asSharedFlow()

    init {
        observeSettings()
        initializeDatabase()
        observeAutoLock()
    }

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.Authenticate ->
                authenticate(
                    activity = intent.activity,
                    title = intent.title,
                    subtitle = intent.subtitle,
                    onError = intent.onError,
                    onSuccess = intent.onSuccess
                )
            MainIntent.Authorize -> authorize()
            MainIntent.Lock -> lock()
            MainIntent.UpdateInteraction -> autoLockCoordinator.onInteraction(isAuthorized)
            MainIntent.CheckAndLock -> autoLockCoordinator.checkNow(isAuthorized)
            MainIntent.RetryDatabaseInitialization -> initializeDatabase()
            is MainIntent.ExportEmergencyBackup -> exportEmergencyBackup()
            is MainIntent.ExportPlainBackup -> exportPlainBackup(intent.context, intent.dirUri)
            is MainIntent.ExportPlainBackupToUri -> exportPlainBackupToUri(intent.uri)
        }
    }

    val isAuthorized: Boolean
        get() = _uiState.value.isAuthorized

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onError: ((String) -> Unit)? = null,
        onSuccess: () -> Unit
    ) {
        when (val validation = validationSupport.validateAuthenticationRequest(activity, title)) {
            is MainValidationResult.Invalid -> {
                onError?.invoke(validation.message)
                _uiState.update { it.copy(validationMessage = validation.message) }
                emitEffect(MainEffect.ShowError(validation.message))
                return
            }

            MainValidationResult.Valid -> Unit
        }

        BiometricHelper.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onError = { error ->
                val safeError = validationSupport.sanitizeMessage(error)
                onError?.invoke(safeError)
                _uiState.update { it.copy(validationMessage = safeError) }
                emitEffect(MainEffect.ShowError(safeError))
            },
            onSuccess = {
                _uiState.update { it.copy(validationMessage = null) }
                authorize()
                onSuccess()
            }
        )
    }

    fun authorize() {
        _uiState.update { it.copy(isAuthorized = true, validationMessage = null) }
        autoLockCoordinator.onAuthorized()
        emitEffect(MainEffect.NavigateToVault)
    }

    fun lock() {
        if (!isAuthorized) {
            autoLockCoordinator.onLocked()
            return
        }
        _uiState.update { it.copy(isAuthorized = false) }
        autoLockCoordinator.onLocked()
    }

    private fun observeAutoLock() {
        viewModelScope.launch {
            autoLockCoordinator.shouldLock.collect {
                if (isAuthorized) {
                    lock()
                    emitEffect(MainEffect.LockedByTimeout)
                }
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
                val decision = autoLockCoordinator.applyTimeout(lockTimeout, isAuthorized)
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
            // 优先写入用户配置的 SAF 目录
            viewModelScope.launch {
                val targetResult = withContext(Dispatchers.IO) {
                    BackupExportStorageSupport.createNamedExportTarget(
                        context.applicationContext, dirUri, fileName
                    )
                }
                targetResult.fold(
                    onSuccess = { target -> exportPlainBackupToUri(target.fileUri) },
                    onFailure = {
                        // 目录不可写，回退到文件选择器
                        emitEffect(MainEffect.ShowPlainExportPicker(fileName))
                    }
                )
            }
        } else {
            // 未配置目录，通知 UI 弹出文件选择器
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
                emitEffect(MainEffect.ShowError(validationSupport.formatDatabaseError(error)))
            }
        }
    }

    private fun emitEffect(effect: MainEffect) {
        _effects.tryEmit(effect)
    }
}