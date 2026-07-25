package com.aozijx.passly.feature.vault.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.feature.detail.DetailCardDialog
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.scanner.VaultScanner
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.dialogs.DeleteConfirmDialog
import com.aozijx.passly.feature.vault.internal.VaultDetailCoordinatorState
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.model.OtpUiState

// --- 详情对话框宿主 ---
@Composable
fun DetailDialogHost(
    detailCoordinatorState: VaultDetailCoordinatorState,
    totpStates: Map<String, OtpUiState>,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel
) {
    detailCoordinatorState.request?.let { request ->
        val item = request.entry
        if (detailCoordinatorState.isIconPickerVisible) {
            IconPickerDialog(
                onDismiss = { vaultViewModel.hideDetailIconPicker() },
                currentIconName = item.iconName,
                currentCustomPath = item.iconCustomPath,
                onIconSelected = { name ->
                    vaultViewModel.updateVaultEntry(
                        item.copy(
                            summary = item.summary.copy(icon = name)
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
            otpState = totpStates[item.id],
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

// --- 统一入口：保持向后兼容 ---
@Composable
fun VaultDialogs(
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    onUpdateInteraction: () -> Unit
) {
    val totpStates by vaultViewModel.totpStatesFlow.collectAsStateWithLifecycle()
    val detailState by vaultViewModel.detailStateFlow.collectAsStateWithLifecycle()

    DetailDialogHost(
        detailCoordinatorState = detailState,
        totpStates = totpStates,
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

}
