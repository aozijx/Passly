package com.aozijx.passly.features.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.core.backup.BackupImportMode
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.common.ui.VaultCardStyle
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.features.settings.internal.BackupActionSupport
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val lockTimeout: Long = 60000L,
    val isStatusBarAutoHide: Boolean = true,
    val isTopBarCollapsible: Boolean = true,
    val isTabBarCollapsible: Boolean = true,
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = false,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val cardStyle: VaultCardStyle = VaultCardStyle.styleConfig.globalDefaultStyle,
    val cardStyleByEntryType: Map<Int, VaultCardStyle> = mapOf(-1 to VaultCardStyle.styleConfig.globalDefaultStyle),
    val autofillUiMode: AutofillUiMode = AutofillUiMode.SYSTEM_INLINE,
    val isSwipeEnabled: Boolean = true,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
    val backupDirectoryUri: String? = null,
    val lastBackupExportFileName: String? = null
)

private data class CoreSettingsFlowState(
    val lockTimeout: Long,
    val isStatusBarAutoHide: Boolean,
    val isTopBarCollapsible: Boolean,
    val isTabBarCollapsible: Boolean,
    val isSecureContentEnabled: Boolean
)

private data class InteractionSettingsFlowState(
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean,
    val cardStyle: VaultCardStyle,
    val cardStyleByEntryType: Map<Int, VaultCardStyle>,
    val autofillUiMode: AutofillUiMode,
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: SwipeActionType
)

private data class SecurityAndStyleFlowState(
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean,
    val cardStyle: VaultCardStyle,
    val cardStyleByEntryType: Map<Int, VaultCardStyle>
)

private data class AutofillAndSwipeFlowState(
    val autofillUiMode: AutofillUiMode,
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: SwipeActionType
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsUseCases = AppContainer.settingsUseCases
    private val backupActionSupport = BackupActionSupport()

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
        settingsUseCases.lockTimeout,
        settingsUseCases.isStatusBarAutoHide,
        settingsUseCases.isTopBarCollapsible,
        settingsUseCases.isTabBarCollapsible,
        settingsUseCases.isSecureContentEnabled
    ) { lockTimeout, isStatusBarAutoHide, isTopBarCollapsible, isTabBarCollapsible, isSecureContentEnabled ->
        CoreSettingsFlowState(
            lockTimeout = lockTimeout,
            isStatusBarAutoHide = isStatusBarAutoHide,
            isTopBarCollapsible = isTopBarCollapsible,
            isTabBarCollapsible = isTabBarCollapsible,
            isSecureContentEnabled = isSecureContentEnabled
        )
    },
        combine(
            settingsUseCases.isFlipToLockEnabled,
            settingsUseCases.isFlipExitAndClearStackEnabled,
            settingsUseCases.cardStyle,
            settingsUseCases.cardStyleByEntryType
        ) { isFlipToLockEnabled, isFlipExitAndClearStackEnabled, cardStyle, cardStyleByEntryType ->
            SecurityAndStyleFlowState(
                isFlipToLockEnabled = isFlipToLockEnabled,
                isFlipExitAndClearStackEnabled = isFlipExitAndClearStackEnabled,
                cardStyle = cardStyle,
                cardStyleByEntryType = cardStyleByEntryType
            )
        }.combine(
            combine(
            settingsUseCases.autofillUiMode,
            settingsUseCases.isSwipeEnabled,
            settingsUseCases.swipeLeftAction
        ) { autofillUiMode, isSwipeEnabled, swipeLeftAction ->
            AutofillAndSwipeFlowState(
                autofillUiMode = autofillUiMode,
                isSwipeEnabled = isSwipeEnabled,
                swipeLeftAction = swipeLeftAction
            )
        }) { securityAndStyle, autofillAndSwipe ->
            InteractionSettingsFlowState(
                isFlipToLockEnabled = securityAndStyle.isFlipToLockEnabled,
                isFlipExitAndClearStackEnabled = securityAndStyle.isFlipExitAndClearStackEnabled,
                cardStyle = securityAndStyle.cardStyle,
                cardStyleByEntryType = securityAndStyle.cardStyleByEntryType,
                autofillUiMode = autofillAndSwipe.autofillUiMode,
                isSwipeEnabled = autofillAndSwipe.isSwipeEnabled,
                swipeLeftAction = autofillAndSwipe.swipeLeftAction
            )
        },
        settingsUseCases.swipeRightAction,
        settingsUseCases.backupDirectoryUri,
        settingsUseCases.lastBackupExportFileName
    ) { core, interaction, swipeRightAction, backupDirectoryUri, lastBackupExportFileName ->
        SettingsUiState(
            lockTimeout = core.lockTimeout,
            isStatusBarAutoHide = core.isStatusBarAutoHide,
            isTopBarCollapsible = core.isTopBarCollapsible,
            isTabBarCollapsible = core.isTabBarCollapsible,
            isSecureContentEnabled = core.isSecureContentEnabled,
            isFlipToLockEnabled = interaction.isFlipToLockEnabled,
            isFlipExitAndClearStackEnabled = interaction.isFlipExitAndClearStackEnabled,
            cardStyle = interaction.cardStyle,
            cardStyleByEntryType = interaction.cardStyleByEntryType,
            autofillUiMode = interaction.autofillUiMode,
            isSwipeEnabled = interaction.isSwipeEnabled,
            swipeLeftAction = interaction.swipeLeftAction,
            swipeRightAction = swipeRightAction,
            backupDirectoryUri = backupDirectoryUri,
            lastBackupExportFileName = lastBackupExportFileName
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    // --- Setter 方法 ---
    fun setStatusBarAutoHide(autoHide: Boolean) =
        viewModelScope.launch { settingsUseCases.setStatusBarAutoHide(autoHide) }

    fun setTopBarCollapsible(collapsible: Boolean) =
        viewModelScope.launch { settingsUseCases.setTopBarCollapsible(collapsible) }

    fun setTabBarCollapsible(collapsible: Boolean) =
        viewModelScope.launch { settingsUseCases.setTabBarCollapsible(collapsible) }

    fun setSecureContentEnabled(enabled: Boolean) =
        viewModelScope.launch { settingsUseCases.setSecureContentEnabled(enabled) }

    fun setFlipToLockEnabled(enabled: Boolean) =
        viewModelScope.launch { settingsUseCases.setFlipToLockEnabled(enabled) }

    fun setFlipExitAndClearStackEnabled(enabled: Boolean) =
        viewModelScope.launch { settingsUseCases.setFlipExitAndClearStackEnabled(enabled) }

    fun setLockTimeout(timeoutMs: Long) =
        viewModelScope.launch { settingsUseCases.setLockTimeout(timeoutMs.coerceAtLeast(5000L)) }

    fun setCardStyle(style: VaultCardStyle) =
        viewModelScope.launch { settingsUseCases.setCardStyle(style) }

    fun setCardStyleForEntryType(entryTypeValue: Int, style: VaultCardStyle) =
        viewModelScope.launch {
            settingsUseCases.setCardStyleForEntryType(entryTypeValue, style)
        }

    fun toggleAutofillUiMode(currentMode: AutofillUiMode) = viewModelScope.launch {
        val nextMode = when (currentMode) {
            AutofillUiMode.SYSTEM_INLINE -> AutofillUiMode.BOTTOM_SHEET
            AutofillUiMode.BOTTOM_SHEET -> AutofillUiMode.SYSTEM_INLINE
        }
        settingsUseCases.setAutofillUiMode(nextMode)
    }

    fun setSwipeEnabled(enabled: Boolean) =
        viewModelScope.launch { settingsUseCases.setSwipeEnabled(enabled) }

    fun setSwipeLeftAction(action: SwipeActionType) =
        viewModelScope.launch { settingsUseCases.setSwipeLeftAction(action) }

    fun setSwipeRightAction(action: SwipeActionType) =
        viewModelScope.launch { settingsUseCases.setSwipeRightAction(action) }

    // --- 备份/恢复相关逻辑 ---
    var backupMessage by mutableStateOf<String?>(null); private set
    var isExporting by mutableStateOf(false)
    var showBackupPasswordDialog by mutableStateOf(false)
    var backupUri by mutableStateOf<Uri?>(null)
    var backupPassword by mutableStateOf("")
    var importMode by mutableStateOf(BackupImportMode.OVERWRITE)
    var includeImagesInBackup by mutableStateOf(true) // 新增：是否包含图片
    var backupExportFallbackFileName by mutableStateOf<String?>(null)
        private set

    private var pendingExportFileName: String? = null
    private var pendingExportAllowFallback: Boolean = false

    private fun text(@StringRes resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }

    fun startExport(uri: Uri, fileNameHint: String? = null, allowFallback: Boolean = false) {
        backupUri = uri
        isExporting = true
        showBackupPasswordDialog = true
        pendingExportFileName = fileNameHint
        pendingExportAllowFallback = allowFallback
    }

    fun startImport(uri: Uri) {
        backupUri = uri
        isExporting = false
        showBackupPasswordDialog = true
        pendingExportFileName = null
        pendingExportAllowFallback = false
    }

    fun nextBackupFileName(): String = BackupExportStorageSupport.buildBackupFileName()

    fun tryStartExportInConfiguredDirectory(directoryUri: String?): Boolean {
        if (directoryUri.isNullOrBlank()) return false
        // 核心修改：不再在这里创建文件，而是直接开始流程，传入目录 URI
        startExport(directoryUri.toUri(), fileNameHint = nextBackupFileName(), allowFallback = true)
        return true
    }

    fun consumeBackupExportFallbackFileName() {
        backupExportFallbackFileName = null
    }

    fun dismissBackupPasswordDialog() {
        showBackupPasswordDialog = false
        backupPassword = ""
        backupUri = null
    }

    fun processBackupAction(context: Context) {
        val targetUri = backupUri ?: return
        val password = backupPassword.toCharArray()
        val exportingNow = isExporting
        val exportFileName = pendingExportFileName
        val allowFallback = pendingExportAllowFallback
        val includeImages = includeImagesInBackup

        viewModelScope.launch {
            try {
                // 如果是导出，且传入的是目录，则现在创建正式文件
                val finalUri = if (exportingNow && allowFallback) {
                    val createResult = BackupExportStorageSupport.createNamedExportTarget(
                        context, targetUri.toString(), exportFileName ?: nextBackupFileName()
                    )
                    if (createResult.isFailure) {
                        backupMessage = text(R.string.backup_error_create_file_failed)
                        return@launch
                    }
                    createResult.getOrThrow().fileUri
                } else {
                    targetUri
                }

                val outcome = backupActionSupport.runBackupAction(
                    context = context,
                    uri = finalUri,
                    password = password,
                    isExporting = exportingNow,
                    importMode = importMode,
                    includeImages = includeImages
                )

                // 如果导出失败，且是我们刚刚创建的文件，尝试清理掉这个空文件
                if (exportingNow && outcome.isFailure && finalUri != targetUri) {
                    val deleted = BackupExportStorageSupport.deleteDocument(context, finalUri)
                    if (deleted) {
                        Toast.makeText(
                            context,
                            text(R.string.backup_export_failed_cleanup_done),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                if (exportingNow) {
                    if (!outcome.isFailure && !exportFileName.isNullOrBlank()) {
                        settingsUseCases.setLastBackupExportFileName(exportFileName)
                    } else if (allowFallback) {
                        backupExportFallbackFileName = exportFileName ?: nextBackupFileName()
                    }
                }
                backupMessage = outcome.message
                dismissBackupPasswordDialog()
                pendingExportFileName = null
                pendingExportAllowFallback = false
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun clearBackupMessage() {
        backupMessage = null
    }

    // --- 备份路径设置 ---
    fun setBackupDirectoryUri(uri: String) =
        viewModelScope.launch { settingsUseCases.setBackupDirectoryUri(uri) }

    fun clearBackupDirectoryUri() =
        viewModelScope.launch { settingsUseCases.clearBackupDirectoryUri() }

    fun testBackupDirectoryWritePermission(context: Context, directoryUri: String?) {
        if (directoryUri.isNullOrBlank()) {
            backupMessage = text(R.string.backup_directory_set_first)
            return
        }
        val result = BackupExportStorageSupport.testWritePermission(context, directoryUri)
        backupMessage = if (result.isSuccess) {
            text(R.string.backup_directory_permission_ok)
        } else {
            text(R.string.backup_directory_permission_failed)
        }
    }
}
