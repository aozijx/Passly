package com.aozijx.passly.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.MainViewModel
import com.aozijx.passly.R
import com.aozijx.passly.core.common.AddType
import com.aozijx.passly.features.detail.TwoFADetailDialog
import com.aozijx.passly.features.vault.VaultViewModel
import com.aozijx.passly.features.vault.components.AddPasswordDialog
import com.aozijx.passly.features.vault.components.AddTwoFADialog
import com.example.passly.features.detail.AutoFillDetailDialog
import com.example.passly.features.detail.PasswordDetailDialog
import com.example.passly.features.settings.SettingsViewModel

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
