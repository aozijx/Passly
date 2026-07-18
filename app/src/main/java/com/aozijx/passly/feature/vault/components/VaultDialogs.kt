package com.aozijx.passly.feature.vault.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.feature.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.feature.backup.BackupCoordinator
import com.aozijx.passly.feature.backup.components.BackupPasswordDialog
import com.aozijx.passly.feature.detail.DetailCardDialog
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.dialogs.DeleteConfirmDialog
import com.aozijx.passly.feature.vault.model.AddType

@Composable
fun VaultDialogs(
    mainViewModel: MainViewModel,
    launcher: BiometricPromptLauncher,
    vaultViewModel: VaultViewModel,
    backupCoordinator: BackupCoordinator,
    onUpdateInteraction: () -> Unit
) {
    val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val detailCoordinator = uiState.detailCoordinatorState

    // --- 详情对话框 ---
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
                })
        }

        DetailCardDialog(
            initialEntry = item,
            launchMode = request.launchMode,
            biometricLauncher = launcher,
            mainViewModel = mainViewModel,
            vaultViewModel = vaultViewModel,
            onDismiss = { vaultViewModel.dismissDetail() })
    }

    // --- 添加对话框 ---
    when (vaultViewModel.addType) {
        AddType.PASSWORD -> AddPasswordDialog(
            viewModel = vaultViewModel,
            onUpdateInteraction = onUpdateInteraction
        )

        AddType.TOTP -> AddTwoFADialog(
            viewModel = vaultViewModel,
            onUpdateInteraction = onUpdateInteraction
        )
        AddType.BANK_CARD,
        AddType.WIFI,
        AddType.SSH_KEY,
        AddType.ID_CARD,
        AddType.SEED_PHRASE,
        AddType.PASSKEY,
        AddType.RECOVERY_CODE -> {
            val type = vaultViewModel.addType ?: return@VaultDialogs
            AddGenericEntryDialog(
                viewModel = vaultViewModel,
                addType = type,
                onUpdateInteraction = onUpdateInteraction
            )
        }

        null -> Unit
        else -> {}
    }

    // --- 全局确认/反馈对话框 ---
    vaultViewModel.itemToDelete?.let { item ->
        DeleteConfirmDialog(
            launcher = launcher,
            item = item,
            mainViewModel = mainViewModel,
            onConfirm = { vaultViewModel.confirmDelete() },
            onDismiss = { vaultViewModel.setItemToDelete(null) })
    }

    // --- 备份对话框 ---
    if (backupCoordinator.state.showPasswordDialog) {
        BackupPasswordDialog(
            backupCoordinator = backupCoordinator,
            onAuthRequired = { title, subtitle, onSuccess ->
                mainViewModel.requestAuth(
                    onSuccess = onSuccess
                )
            }
        )
    }
}
