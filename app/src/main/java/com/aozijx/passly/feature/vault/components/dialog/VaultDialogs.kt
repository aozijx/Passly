package com.aozijx.passly.feature.vault.components.dialog

import androidx.compose.runtime.Composable
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.components.editor.AddEntryDialog
import com.aozijx.passly.feature.vault.contract.VaultIntent
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.model.AddType

// --- 添加对话框宿主 ---
@Composable
fun AddDialogHost(
    uiState: VaultUiState,
    vaultViewModel: VaultViewModel,
    onUpdateInteraction: () -> Unit
) {
    when (uiState.addType) {
        AddType.BANK_CARD,
        AddType.WIFI,
        AddType.SSH_KEY,
        AddType.ID_CARD,
        AddType.SEED_PHRASE,
        AddType.PASSKEY,
        AddType.RECOVERY_CODE -> {
            val type = uiState.addType
            AddEntryDialog(
                viewModel = vaultViewModel,
                addType = type,
                onUpdateInteraction = onUpdateInteraction
            )
        }

        AddType.PASSWORD,
        AddType.TOTP,
        null -> Unit
    }
}

// --- 删除确认对话框宿主 ---
@Composable
fun DeleteDialogHost(
    uiState: VaultUiState,
    vaultViewModel: VaultViewModel,
    requestAuthentication: (onSuccess: () -> Unit) -> Unit
) {
    uiState.pendingDelete?.let { item ->
        DeleteConfirmDialog(
            item = item,
            requestAuthentication = requestAuthentication,
            onConfirm = { vaultViewModel.onIntent(VaultIntent.ConfirmDelete) },
            onDismiss = { vaultViewModel.onIntent(VaultIntent.ItemToDeleteSelected(null)) }
        )
    }
}

@Composable
fun VaultDialogs(
    uiState: VaultUiState,
    vaultViewModel: VaultViewModel,
    requestAuthentication: (onSuccess: () -> Unit) -> Unit,
    onUpdateInteraction: () -> Unit
) {
    AddDialogHost(
        uiState = uiState,
        vaultViewModel = vaultViewModel,
        onUpdateInteraction = onUpdateInteraction
    )

    DeleteDialogHost(
        uiState = uiState,
        vaultViewModel = vaultViewModel,
        requestAuthentication = requestAuthentication
    )
}
