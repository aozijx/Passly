package com.aozijx.passly.features.vault.components

import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.MainViewModel
import com.aozijx.passly.core.designsystem.model.AddType
import com.aozijx.passly.features.detail.DetailCardDialog
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.VaultViewModel
import com.aozijx.passly.features.vault.dialogs.BackupPasswordDialog
import com.aozijx.passly.features.vault.dialogs.DeleteConfirmDialog
import com.aozijx.passly.features.vault.dialogs.IconPickerDialog

@Composable
fun VaultDialogs(
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val detailCoordinator = vaultViewModel.detailCoordinatorState

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
                            iconName = name, iconCustomPath = null
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
            activity = activity,
            mainViewModel = mainViewModel,
            vaultViewModel = vaultViewModel,
            onDismiss = { vaultViewModel.dismissDetail() })
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
            onDismiss = { vaultViewModel.itemToDelete = null })
    }

    if (settingsViewModel.showBackupPasswordDialog) {
        BackupPasswordDialog(
            activity = activity,
            mainViewModel = mainViewModel,
            settingsViewModel = settingsViewModel
        )
    }
}




