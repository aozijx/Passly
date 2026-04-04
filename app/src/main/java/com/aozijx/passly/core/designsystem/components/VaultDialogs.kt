package com.aozijx.passly.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.MainViewModel
import com.aozijx.passly.core.common.ui.AddType
import com.aozijx.passly.features.detail.DetailCardDialog
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.VaultViewModel
import com.aozijx.passly.features.vault.components.AddPasswordDialog
import com.aozijx.passly.features.vault.components.AddTwoFADialog

@Composable
fun VaultDialogs(
    activity: FragmentActivity, 
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    // --- 详情对话框 ---
    vaultViewModel.detailItem?.let { item ->
        if (vaultViewModel.showIconPicker) {
            IconPickerDialog(
                onDismiss = { vaultViewModel.showIconPicker = false },
                currentIconName = item.iconName,
                currentCustomPath = item.iconCustomPath,
                onIconSelected = { name ->
                    vaultViewModel.updateVaultEntry(
                        item.copy(
                            iconName = name,
                            iconCustomPath = null
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
            activity = activity,
            mainViewModel = mainViewModel,
            vaultViewModel = vaultViewModel,
            onDismiss = { vaultViewModel.dismissDetail() }
        )
    }

    // --- 添加对话框 ---
    when (vaultViewModel.addType) {
        AddType.PASSWORD -> AddPasswordDialog(viewModel = vaultViewModel)
        AddType.TOTP -> AddTwoFADialog(viewModel = vaultViewModel)
        else -> {} 
    }

    // --- 全局确认/反馈对话框 ---
    vaultViewModel.itemToDelete?.let { item ->
        DeleteConfirmDialog(
            activity = activity,
            item = item,
            mainViewModel = mainViewModel,
            onConfirm = { vaultViewModel.confirmDelete() },
            onDismiss = { vaultViewModel.itemToDelete = null }
        )
    }

    if (settingsViewModel.showBackupPasswordDialog) {
        BackupPasswordDialog(
            activity = activity,
            mainViewModel = mainViewModel,
            settingsViewModel = settingsViewModel
        )
    }
}



