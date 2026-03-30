package com.example.poop.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poop.MainViewModel
import com.example.poop.R
import com.example.poop.common.base.LoadingMask
import com.example.poop.components.dialog.BackupPasswordDialog
import com.example.poop.components.dialog.DeleteConfirmDialog
import com.example.poop.components.dialog.IconPickerDialog
import com.example.poop.core.AddType
import com.example.poop.features.vault.VaultViewModel
import com.example.poop.features.settings.SettingsViewModel
import com.example.poop.types.autofill.AutoFillDetailDialog
import com.example.poop.types.password.AddPasswordDialog
import com.example.poop.domain.model.PasswordDetailDialog
import com.example.poop.types.totp.AddTwoFADialog
import com.example.poop.domain.model.TwoFADetailDialog

@Composable
fun VaultDialogs(
    activity: FragmentActivity, 
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val categoryAutofill = stringResource(R.string.category_autofill)
    
    // --- 详情对话框 ---
    vaultViewModel.detailItem?.let { item ->
        if (vaultViewModel.showIconPicker) {
            IconPickerDialog(
                onDismiss = { vaultViewModel.showIconPicker = false },
                currentIconName = item.iconName,
                currentCustomPath = item.iconCustomPath,
                onIconSelected = { name -> 
                    vaultViewModel.updateVaultEntry(item.copy(iconName = name, iconCustomPath = null))
                },
                onCustomImageSelected = { uri ->
                    // 这里原本是逻辑存根，实际需要处理图片保存，暂时更新 UI 路径
                    vaultViewModel.updateVaultEntry(item.copy(iconName = null, iconCustomPath = uri.toString()))
                }
            )
        }

        when {
            item.totpSecret != null -> {
                TwoFADetailDialog(
                    activity = activity,
                    item = item,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel
                )
            }
            item.category == categoryAutofill || item.associatedDomain != null || item.associatedAppPackage != null -> {
                AutoFillDetailDialog(
                    item = item,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel,
                    activity = activity
                )
            }
            else -> {
                PasswordDetailDialog(
                    activity = activity,
                    item = item,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel
                )
            }
        }
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
