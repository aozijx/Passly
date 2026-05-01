package com.aozijx.passly.features.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.R
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.features.backup.ui.BackupPathSettingsConfig
import com.aozijx.passly.features.settings.components.dialogs.AppPasswordActionDialog
import com.aozijx.passly.features.settings.components.dialogs.AppPasswordChangeDialog
import com.aozijx.passly.features.settings.components.dialogs.AppPasswordDisableDialog
import com.aozijx.passly.features.settings.components.dialogs.AppPasswordSetDialog
import com.aozijx.passly.features.settings.components.dialogs.LockTimeoutDialog
import com.aozijx.passly.features.settings.components.dialogs.SwipeActionSelectDialog
import com.aozijx.passly.features.settings.components.sections.AppearanceCustomizationSettingsSection
import com.aozijx.passly.features.settings.components.sections.BackupRestoreSettingsSection
import com.aozijx.passly.features.settings.components.sections.DataSettingsSection
import com.aozijx.passly.features.settings.components.sections.ImmersiveExperienceSettingsSection
import com.aozijx.passly.features.settings.components.sections.InteractionHabitsSettingsSection
import com.aozijx.passly.features.settings.components.sections.SecurityPrivacySettingsSection
import com.aozijx.passly.features.settings.components.sections.VaultTabsSettingsSection

private enum class AppPasswordAction {
    SET,
    CHANGE,
    DISABLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAppPasswordEnabled by viewModel.isAppPasswordEnabled.collectAsStateWithLifecycle()

    val lockTimeout = uiState.lockTimeout
    val isInvalidateKeyOnBioChange = uiState.isInvalidateKeyOnBioChange
    val isSwipeEnabled = uiState.isSwipeEnabled
    val swipeLeftAction = uiState.swipeLeftAction
    val swipeRightAction = uiState.swipeRightAction
    val isStatusBarAutoHide = uiState.isStatusBarAutoHide
    val isTopBarCollapsible = uiState.isTopBarCollapsible
    val isTabBarCollapsible = uiState.isTabBarCollapsible
    val isSecureContentEnabled = uiState.isSecureContentEnabled
    val isPasswordPreferredAuthFirst = uiState.isPasswordPreferredAuthFirst
    val isFlipToLockEnabled = uiState.isFlipToLockEnabled
    val isFlipExitAndClearStackEnabled = uiState.isFlipExitAndClearStackEnabled
    val cardStyle = uiState.cardStyle
    val cardStyleByEntryType = uiState.cardStyleByEntryType
    val autofillUiMode = uiState.autofillUiMode
    val backupDirectoryUri = uiState.backupDirectoryUri
    val lastBackupExportFileName = uiState.lastBackupExportFileName
    val visibleVaultTabs = uiState.visibleVaultTabs
    val isAutoDownloadIcons = uiState.isAutoDownloadIcons

    val availableCardStyles = remember { VaultCardStyle.styleConfig.perTypeStyles }
    val effectiveCardStyle = VaultCardStyle.normalizeGlobalStyle(cardStyle)
    val passwordSelectedStyle =
        cardStyleByEntryType[EntryType.PASSWORD.value] ?: VaultCardStyle.DEFAULT
    val totpSelectedStyle = cardStyleByEntryType[EntryType.TOTP.value] ?: VaultCardStyle.DEFAULT
    val context = LocalContext.current

    LaunchedEffect(cardStyle) {
        if (cardStyle != effectiveCardStyle) {
            viewModel.setCardStyle(effectiveCardStyle)
        }
    }
    LaunchedEffect(viewModel.backup.backupMessage) {
        viewModel.backup.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.backup.clearBackupMessage()
        }
    }

    var showLeftActionDialog by remember { mutableStateOf(false) }
    var showRightActionDialog by remember { mutableStateOf(false) }
    var showLockTimeoutDialog by remember { mutableStateOf(false) }
    var showClearBackupDirConfirmDialog by remember { mutableStateOf(false) }
    var showAppPasswordActionDialog by remember { mutableStateOf(false) }
    var showSetAppPasswordDialog by remember { mutableStateOf(false) }
    var showChangeAppPasswordDialog by remember { mutableStateOf(false) }
    var showDisableAppPasswordDialog by remember { mutableStateOf(false) }
    var appPasswordCurrent by remember { mutableStateOf("") }
    var appPasswordNew by remember { mutableStateOf("") }
    var appPasswordConfirm by remember { mutableStateOf("") }

    fun clearAppPasswordInputs() {
        appPasswordCurrent = ""
        appPasswordNew = ""
        appPasswordConfirm = ""
    }

    fun handleAppPasswordAction(action: AppPasswordAction) {
        when (action) {
            AppPasswordAction.SET -> {
                if (appPasswordNew != appPasswordConfirm) {
                    Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                    return
                }
                val newPasswordChars = appPasswordNew.toCharArray()
                viewModel.setAppPassword(newPasswordChars) { result ->
                    try {
                        result.onSuccess {
                            Toast.makeText(context, "应用密码已启用", Toast.LENGTH_SHORT).show()
                            showSetAppPasswordDialog = false
                            clearAppPasswordInputs()
                        }.onFailure { e ->
                            Toast.makeText(context, e.message ?: "设置失败", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } finally {
                        newPasswordChars.fill('\u0000')
                    }
                }
            }

            AppPasswordAction.CHANGE -> {
                if (appPasswordNew != appPasswordConfirm) {
                    Toast.makeText(context, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show()
                    return
                }
                val oldPasswordChars = appPasswordCurrent.toCharArray()
                val newPasswordChars = appPasswordNew.toCharArray()
                viewModel.changeAppPassword(
                    oldPassword = oldPasswordChars,
                    newPassword = newPasswordChars
                ) { result ->
                    try {
                        result.onSuccess {
                            Toast.makeText(context, "应用密码已更新", Toast.LENGTH_SHORT).show()
                            showChangeAppPasswordDialog = false
                            clearAppPasswordInputs()
                        }.onFailure { e ->
                            Toast.makeText(context, e.message ?: "修改失败", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } finally {
                        oldPasswordChars.fill('\u0000')
                        newPasswordChars.fill('\u0000')
                    }
                }
            }

            AppPasswordAction.DISABLE -> {
                val currentPasswordChars = appPasswordCurrent.toCharArray()
                viewModel.disableAppPassword(currentPasswordChars) { result ->
                    try {
                        result.onSuccess {
                            Toast.makeText(context, "已关闭应用密码", Toast.LENGTH_SHORT).show()
                            showDisableAppPasswordDialog = false
                            clearAppPasswordInputs()
                        }.onFailure { e ->
                            Toast.makeText(context, e.message ?: "关闭失败", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } finally {
                        currentPasswordChars.fill('\u0000')
                    }
                }
            }
        }
    }

    val backupPathLabel =
        remember(backupDirectoryUri) { BackupPathSettingsConfig.displayValue(backupDirectoryUri) }
    val lastExportFileLabel = remember(lastBackupExportFileName) {
        BackupPathSettingsConfig.displayRecentFileName(lastBackupExportFileName)
    }
    val backupPathPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching<Unit> {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            val appDirectoryTreeUri =
                BackupExportStorageSupport.ensureAppDirectoryTreeUri(context, uri).getOrElse {
                    Toast.makeText(context, "目录初始化失败，请重新选择", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }
            viewModel.setBackupDirectoryUri(appDirectoryTreeUri.toString())
        }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
            LargeTopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                ImmersiveExperienceSettingsSection(
                    isStatusBarAutoHide = isStatusBarAutoHide,
                    isTopBarCollapsible = isTopBarCollapsible,
                    isTabBarCollapsible = isTabBarCollapsible,
                    onStatusBarAutoHideChange = viewModel::setStatusBarAutoHide,
                    onTopBarCollapsibleChange = viewModel::setTopBarCollapsible,
                    onTabBarCollapsibleChange = viewModel::setTabBarCollapsible
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                SecurityPrivacySettingsSection(
                    lockTimeout = lockTimeout,
                    isAppPasswordEnabled = isAppPasswordEnabled,
                    isPasswordPreferredAuthFirst = isPasswordPreferredAuthFirst,
                    isInvalidateKeyOnBioChange = isInvalidateKeyOnBioChange,
                    isSecureContentEnabled = isSecureContentEnabled,
                    isFlipToLockEnabled = isFlipToLockEnabled,
                    isFlipExitAndClearStackEnabled = isFlipExitAndClearStackEnabled,
                    onLockTimeoutClick = { showLockTimeoutDialog = true },
                    onAppPasswordClick = {
                        if (isAppPasswordEnabled) {
                            showAppPasswordActionDialog = true
                        } else {
                            val activity = context as? FragmentActivity
                            if (activity == null) {
                                Toast.makeText(context, "当前页面不支持验证", Toast.LENGTH_SHORT)
                                    .show()
                                return@SecurityPrivacySettingsSection
                            }
                            viewModel.verifyBeforeSetAppPassword(
                                activity = activity,
                                title = context.getString(R.string.vault_auth_decrypt_title),
                                subtitle = context.getString(R.string.settings_auth_before_set_app_password)
                            ) { result ->
                                result.onSuccess {
                                    showSetAppPasswordDialog = true
                                }.onFailure { e ->
                                    Toast.makeText(
                                        context,
                                        e.message ?: context.getString(R.string.vault_auth_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    onPasswordPreferredAuthFirstChange = viewModel::setPasswordPreferredAuthFirst,
                    onInvalidateKeyOnBioChangeToggle = { enabled ->
                        val activity =
                            context as? FragmentActivity ?: return@SecurityPrivacySettingsSection
                        viewModel.switchKeyInvalidationPolicy(activity, enabled) { result ->
                            result.onFailure { e ->
                                Toast.makeText(
                                    context,
                                    "切换失败: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onSecureContentEnabledChange = viewModel::setSecureContentEnabled,
                    onFlipToLockEnabledChange = viewModel::setFlipToLockEnabled,
                    onFlipExitAndClearStackEnabledChange = viewModel::setFlipExitAndClearStackEnabled
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                InteractionHabitsSettingsSection(
                    isSwipeEnabled = isSwipeEnabled,
                    swipeLeftAction = swipeLeftAction,
                    swipeRightAction = swipeRightAction,
                    autofillUiMode = autofillUiMode,
                    onSwipeEnabledChange = viewModel::setSwipeEnabled,
                    onLeftSwipeActionClick = { showLeftActionDialog = true },
                    onRightSwipeActionClick = { showRightActionDialog = true },
                    onToggleAutofillUiMode = { viewModel.toggleAutofillUiMode(autofillUiMode) })
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                VaultTabsSettingsSection(
                    visibleVaultTabs = visibleVaultTabs,
                    onVisibleVaultTabsChange = viewModel::setVisibleVaultTabs
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                DataSettingsSection(
                    isAutoDownloadIcons = isAutoDownloadIcons,
                    onAutoDownloadIconsChange = viewModel::setAutoDownloadIcons
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                BackupRestoreSettingsSection(
                    pathLabel = backupPathLabel,
                    recentExportFileName = lastExportFileLabel,
                    onPickPath = { backupPathPicker.launch(BackupExportStorageSupport.defaultDocumentsTreeUri()) },
                    onTestWrite = {
                        viewModel.testBackupDirectoryWritePermission(
                            backupDirectoryUri
                        )
                    },
                    onClearPath = if (backupDirectoryUri.isNullOrBlank()) {
                        null
                    } else {
                        { showClearBackupDirConfirmDialog = true }
                    })
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                AppearanceCustomizationSettingsSection(
                    availableStyles = availableCardStyles,
                    passwordSelectedStyle = passwordSelectedStyle,
                    totpSelectedStyle = totpSelectedStyle,
                    onPasswordStyleSelected = {
                        viewModel.setCardStyleForEntryType(EntryType.PASSWORD.value, it)
                    },
                    onTotpStyleSelected = {
                        viewModel.setCardStyleForEntryType(EntryType.TOTP.value, it)
                    })
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showRightActionDialog) {
        SwipeActionSelectDialog(
            "选择右滑动作",
            swipeRightAction,
            { viewModel.setSwipeRightAction(it); showRightActionDialog = false },
            { showRightActionDialog = false })
    }
    if (showLeftActionDialog) {
        SwipeActionSelectDialog(
            "选择左滑动作",
            swipeLeftAction,
            { viewModel.setSwipeLeftAction(it); showLeftActionDialog = false },
            { showLeftActionDialog = false })
    }
    if (showLockTimeoutDialog) {
        LockTimeoutDialog(currentTimeoutMs = lockTimeout, onTimeoutSelected = {
            viewModel.setLockTimeout(it)
            showLockTimeoutDialog = false
        }, onDismiss = { showLockTimeoutDialog = false })
    }

    if (showClearBackupDirConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearBackupDirConfirmDialog = false },
            title = { Text("清除备份目录") },
            text = { Text("只会清除目录配置，不会删除已导出的备份文件。") },
            confirmButton = {
                TextButton(onClick = {
                    if (!backupDirectoryUri.isNullOrBlank()) {
                        runCatching<Unit> {
                            val uri = backupDirectoryUri.toUri()
                            val flags =
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            context.contentResolver.releasePersistableUriPermission(uri, flags)
                        }
                    }
                    viewModel.clearBackupDirectoryUri()
                    showClearBackupDirConfirmDialog = false
                }) {
                    Text("清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearBackupDirConfirmDialog = false }) {
                    Text("取消")
                }
            })
    }

    if (showAppPasswordActionDialog) {
        AppPasswordActionDialog(
            onDismiss = { showAppPasswordActionDialog = false },
            onChangePassword = {
                showAppPasswordActionDialog = false
                showChangeAppPasswordDialog = true
            },
            onDisablePassword = {
                showAppPasswordActionDialog = false
                showDisableAppPasswordDialog = true
            }
        )
    }

    if (showSetAppPasswordDialog) {
        AppPasswordSetDialog(
            newPassword = appPasswordNew,
            confirmPassword = appPasswordConfirm,
            onNewPasswordChange = { appPasswordNew = it },
            onConfirmPasswordChange = { appPasswordConfirm = it },
            onConfirm = { handleAppPasswordAction(AppPasswordAction.SET) },
            onDismiss = {
                showSetAppPasswordDialog = false
                clearAppPasswordInputs()
            }
        )
    }

    if (showChangeAppPasswordDialog) {
        AppPasswordChangeDialog(
            currentPassword = appPasswordCurrent,
            newPassword = appPasswordNew,
            confirmPassword = appPasswordConfirm,
            onCurrentPasswordChange = { appPasswordCurrent = it },
            onNewPasswordChange = { appPasswordNew = it },
            onConfirmPasswordChange = { appPasswordConfirm = it },
            onConfirm = { handleAppPasswordAction(AppPasswordAction.CHANGE) },
            onDismiss = {
                showChangeAppPasswordDialog = false
                clearAppPasswordInputs()
            }
        )
    }

    if (showDisableAppPasswordDialog) {
        AppPasswordDisableDialog(
            currentPassword = appPasswordCurrent,
            onCurrentPasswordChange = { appPasswordCurrent = it },
            onConfirm = { handleAppPasswordAction(AppPasswordAction.DISABLE) },
            onDismiss = {
                showDisableAppPasswordDialog = false
                clearAppPasswordInputs()
            }
        )
    }
}