package com.example.poop.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.example.poop.MainViewModel
import com.example.poop.R
import com.example.poop.common.base.LoadingMask
import com.example.poop.components.dialog.BackupPasswordDialog
import com.example.poop.components.dialog.DeleteConfirmDialog
import com.example.poop.components.dialog.IconPickerDialog
import com.example.poop.core.AddType
import com.example.poop.types.autofill.AutoFillDetailDialog
import com.example.poop.types.password.AddPasswordDialog
import com.example.poop.types.password.PasswordDetailDialog
import com.example.poop.types.totp.AddTwoFADialog
import com.example.poop.types.totp.TwoFADetailDialog

@Composable
fun VaultDialogs(activity: FragmentActivity, viewModel: MainViewModel) {
    val categoryAutofill = stringResource(R.string.category_autofill)
    
    // --- Detail Dialogs ---
    viewModel.detailItem?.let { item ->
        // Icon Picker is a shared dialog, shown when a detail item is present
        if (viewModel.showIconPicker) {
            IconPickerDialog(
                onDismiss = { viewModel.showIconPicker = false },
                currentIconName = item.iconName,
                currentCustomPath = item.iconCustomPath,
                onIconSelected = { name -> viewModel.onIconSelected(name) },
                onCustomImageSelected = { uri ->
                    // 直接传递 uri 对象
                    viewModel.onIconSelected(null, uri)
                }
            )
        }

        when {
            item.totpSecret != null -> {
                TwoFADetailDialog(
                    activity = activity,
                    item = item,
                    viewModel = viewModel
                )
            }
            item.category == categoryAutofill || item.associatedDomain != null || item.associatedAppPackage != null -> {
                AutoFillDetailDialog(
                    activity = activity,
                    item = item,
                    viewModel = viewModel
                )
            }
            else -> {
                PasswordDetailDialog(
                    activity = activity,
                    item = item,
                    viewModel = viewModel
                )
            }
        }
    }

    // --- Add Dialogs ---
    when (viewModel.addType) {
        AddType.PASSWORD -> AddPasswordDialog(viewModel = viewModel)
        AddType.TOTP -> AddTwoFADialog(viewModel = viewModel)
        else -> {} // Other AddTypes are handled by the full-screen scanner or are not dialogs
    }

    // --- Other Global Dialogs ---
    viewModel.itemToDelete?.let { item ->
        DeleteConfirmDialog(activity = activity, item = item, viewModel = viewModel, onConfirm = { viewModel.confirmDelete() }, onDismiss = { viewModel.dismissDeleteDialog() })
    }
    if (viewModel.showBackupPasswordDialog) {
        BackupPasswordDialog(activity = activity, viewModel = viewModel)
    }
    if (viewModel.isBackupLoading) {
        LoadingMask(
            message = if (viewModel.isExporting) {
                stringResource(R.string.loading_export)
            } else {
                stringResource(R.string.loading_import)
            }
        )
    }
}
