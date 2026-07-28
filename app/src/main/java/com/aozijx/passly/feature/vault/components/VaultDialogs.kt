package com.aozijx.passly.feature.vault.components

import androidx.compose.runtime.Composable
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.scanner.VaultScanner
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.dialogs.DeleteConfirmDialog
import com.aozijx.passly.feature.vault.model.AddType

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

        AddType.TOTP -> AddOtpDialog(
            viewModel = vaultViewModel,
            onUpdateInteraction = onUpdateInteraction
        )

        AddType.SCAN -> VaultScanner(
            onSaveOtp = vaultViewModel::addScannedOtp,
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

// --- 统一入口：保持向后兼容 ---
@Composable
fun VaultDialogs(
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    onUpdateInteraction: () -> Unit
) {
    AddDialogHost(
        vaultViewModel = vaultViewModel,
        onUpdateInteraction = onUpdateInteraction
    )

    DeleteDialogHost(
        vaultViewModel = vaultViewModel,
        mainViewModel = mainViewModel
    )
}
