package com.aozijx.passly.feature.vault.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.feature.backup.BackupViewModel
import com.aozijx.passly.feature.backup.contract.BackupIntent
import com.aozijx.passly.feature.detail.DetailCardDialog
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.scanner.VaultScanner
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.dialogs.DeleteConfirmDialog
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.ui.components.BackupPasswordDialog
import com.aozijx.passly.ui.components.BackupPasswordDialogState

// --- 详情对话框宿主 ---
@Composable
fun DetailDialogHost(
    uiState: VaultUiState,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel
) {
    val detailCoordinator = uiState.detailCoordinatorState
    detailCoordinator.request?.let { request ->
        val item = request.entry
        if (detailCoordinator.isIconPickerVisible) {
            IconPickerDialog(
                onDismiss = { vaultViewModel.hideDetailIconPicker() },
                currentIconName = item.iconName,
                currentCustomPath = item.iconCustomPath,
                onIconSelected = { name ->
                    vaultViewModel.updateVaultEntry(
                        item.copy(
                            metadata = item.metadata.copy(icon = name)
                        )
                    )
                },
                onCustomImageSelected = { uri ->
                    vaultViewModel.saveCustomIcon(item, uri)
                }
            )
        }

        DetailCardDialog(
            initialEntry = item,
            launchMode = request.launchMode,
            mainViewModel = mainViewModel,
            totpState = uiState.totpStates[item.id],
            onDismiss = { vaultViewModel.dismissDetail() },
            onUpdateVaultEntry = { vaultViewModel.updateVaultEntry(it) },
            onShowIconPicker = { vaultViewModel.showDetailIconPicker() },
            onAutoUnlockTotp = { vaultViewModel.autoUnlockTotp(it.id) },
            onGenerateHotpCode = { vaultViewModel.generateHotpCode(it) }
        )
    }
}

// --- 添加对话框宿主 ---
@Composable
fun AddDialogHost(
    vaultViewModel: VaultViewModel,
    onUpdateInteraction: () -> Unit
) {
    when (vaultViewModel.addType) {
        AddType.PASSWORD -> AddPasswordDialog(
            viewModel = vaultViewModel,
            onUpdateInteraction = onUpdateInteraction
        )

        AddType.TOTP -> AddTwoFADialog(
            viewModel = vaultViewModel,
            onUpdateInteraction = onUpdateInteraction
        )

        AddType.SCAN -> VaultScanner(
            onSaveEntry = { vaultViewModel.addItem(it) },
            onDismiss = { vaultViewModel.setAddType(null) }
        )

        AddType.BANK_CARD,
        AddType.WIFI,
        AddType.SSH_KEY,
        AddType.ID_CARD,
        AddType.SEED_PHRASE,
        AddType.PASSKEY,
        AddType.RECOVERY_CODE -> {
            val type = vaultViewModel.addType ?: return
            AddGenericEntryDialog(
                viewModel = vaultViewModel,
                addType = type,
                onUpdateInteraction = onUpdateInteraction
            )
        }

        else -> {}
    }
}

// --- 删除确认对话框宿主 ---
@Composable
fun DeleteDialogHost(
    vaultViewModel: VaultViewModel,
    mainViewModel: MainViewModel
) {
    vaultViewModel.itemToDelete?.let { item ->
        DeleteConfirmDialog(
            item = item,
            mainViewModel = mainViewModel,
            onConfirm = { vaultViewModel.confirmDelete() },
            onDismiss = { vaultViewModel.setItemToDelete(null) }
        )
    }
}

// --- 备份密码对话框宿主 ---
@Composable
fun BackupDialogHost(
    backupViewModel: BackupViewModel
) {
    val backupState by backupViewModel.uiState.collectAsStateWithLifecycle()

    if (backupState.showPasswordDialog) {
        BackupPasswordDialog(
            state = BackupPasswordDialogState(
                isExporting = backupState.isExporting,
                importMode = backupState.importMode,
                includeImages = backupState.includeImages,
                backupPassword = backupState.backupPassword
            ),
            onDismiss = { backupViewModel.onIntent(BackupIntent.DismissPasswordDialog) },
            onConfirm = { backupViewModel.onIntent(BackupIntent.ProcessBackupAction) },
            onImportModeChange = { backupViewModel.onIntent(BackupIntent.UpdateImportMode(it)) },
            onIncludeImagesChange = { backupViewModel.onIntent(BackupIntent.UpdateIncludeImages(it)) },
            onPasswordChange = { backupViewModel.onIntent(BackupIntent.UpdatePassword(it)) }
        )
    }
}

// --- 统一入口：保持向后兼容 ---
@Composable
fun VaultDialogs(
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    backupViewModel: BackupViewModel,
    onUpdateInteraction: () -> Unit
) {
    val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()

    DetailDialogHost(
        uiState = uiState,
        mainViewModel = mainViewModel,
        vaultViewModel = vaultViewModel
    )

    AddDialogHost(
        vaultViewModel = vaultViewModel,
        onUpdateInteraction = onUpdateInteraction
    )

    DeleteDialogHost(
        vaultViewModel = vaultViewModel,
        mainViewModel = mainViewModel
    )

    BackupDialogHost(
        backupViewModel = backupViewModel
    )
}
