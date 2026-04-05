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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.common.ui.VaultCardStyle
import com.aozijx.passly.features.settings.components.dialogs.LockTimeoutDialog
import com.aozijx.passly.features.settings.components.dialogs.SwipeActionSelectDialog
import com.aozijx.passly.features.settings.components.sections.AppearanceCustomizationSettingsSection
import com.aozijx.passly.features.settings.components.sections.BackupRestoreSettingsSection
import com.aozijx.passly.features.settings.components.sections.ImmersiveExperienceSettingsSection
import com.aozijx.passly.features.settings.components.sections.InteractionHabitsSettingsSection
import com.aozijx.passly.features.settings.components.sections.SecurityPrivacySettingsSection
import com.aozijx.passly.features.settings.internal.BackupPathSettingsConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lockTimeout = uiState.lockTimeout
    val isSwipeEnabled = uiState.isSwipeEnabled
    val swipeLeftAction = uiState.swipeLeftAction
    val swipeRightAction = uiState.swipeRightAction
    val isStatusBarAutoHide = uiState.isStatusBarAutoHide
    val isTopBarCollapsible = uiState.isTopBarCollapsible
    val isTabBarCollapsible = uiState.isTabBarCollapsible
    val isSecureContentEnabled = uiState.isSecureContentEnabled
    val isFlipToLockEnabled = uiState.isFlipToLockEnabled
    val isFlipExitAndClearStackEnabled = uiState.isFlipExitAndClearStackEnabled
    val cardStyle = uiState.cardStyle
    val cardStyleByEntryType = uiState.cardStyleByEntryType
    val autofillUiMode = uiState.autofillUiMode
    val backupDirectoryUri = uiState.backupDirectoryUri
    val lastBackupExportFileName = uiState.lastBackupExportFileName

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
    LaunchedEffect(viewModel.backupMessage) {
        viewModel.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearBackupMessage()
        }
    }

    var showLeftActionDialog by remember { mutableStateOf(false) }
    var showRightActionDialog by remember { mutableStateOf(false) }
    var showLockTimeoutDialog by remember { mutableStateOf(false) }
    var showClearBackupDirConfirmDialog by remember { mutableStateOf(false) }

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
                    isSecureContentEnabled = isSecureContentEnabled,
                    isFlipToLockEnabled = isFlipToLockEnabled,
                    isFlipExitAndClearStackEnabled = isFlipExitAndClearStackEnabled,
                    onLockTimeoutClick = { showLockTimeoutDialog = true },
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
                BackupRestoreSettingsSection(
                    pathLabel = backupPathLabel,
                    recentExportFileName = lastExportFileLabel,
                    onPickPath = { backupPathPicker.launch(BackupExportStorageSupport.defaultDocumentsTreeUri()) },
                    onTestWrite = {
                        viewModel.testBackupDirectoryWritePermission(
                            context, backupDirectoryUri
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

    if (showLeftActionDialog) {
        SwipeActionSelectDialog(
            "选择左滑动作",
            swipeLeftAction,
            { viewModel.setSwipeLeftAction(it); showLeftActionDialog = false },
            { showLeftActionDialog = false })
    }
    if (showRightActionDialog) {
        SwipeActionSelectDialog(
            "选择右滑动作",
            swipeRightAction,
            { viewModel.setSwipeRightAction(it); showRightActionDialog = false },
            { showRightActionDialog = false })
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
}
