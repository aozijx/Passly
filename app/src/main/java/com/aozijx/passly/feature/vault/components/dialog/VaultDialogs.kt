package com.aozijx.passly.feature.vault.components.dialog

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.feature.vault.components.editor.AddEntryDialog
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.model.AddType

// --- 添加对话框宿主 ---
@Composable
fun AddDialogHost(
    uiState: VaultUiState,
    onAddItem: (EntryAggregate) -> Unit,
    onDismissAddType: () -> Unit,
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
                addType = type,
                onAddItem = onAddItem,
                onDismiss = onDismissAddType,
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
    requestAuthentication: (onSuccess: () -> Unit) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit
) {
    uiState.pendingDelete?.let { item ->
        DeleteConfirmDialog(
            item = item,
            requestAuthentication = requestAuthentication,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete
        )
    }
}

@Composable
fun VaultDialogs(
    uiState: VaultUiState,
    onAddItem: (EntryAggregate) -> Unit,
    onDismissAddType: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    requestAuthentication: (onSuccess: () -> Unit) -> Unit,
    onUpdateInteraction: () -> Unit
) {
    AddDialogHost(
        uiState = uiState,
        onAddItem = onAddItem,
        onDismissAddType = onDismissAddType,
        onUpdateInteraction = onUpdateInteraction
    )

    DeleteDialogHost(
        uiState = uiState,
        requestAuthentication = requestAuthentication,
        onConfirmDelete = onConfirmDelete,
        onDismissDelete = onDismissDelete
    )
}
