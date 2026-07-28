package com.aozijx.passly.feature.vault.components.dialog

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.feature.vault.VaultViewModel
import com.aozijx.passly.feature.vault.components.editor.AddGenericEntryDialog
import com.aozijx.passly.feature.vault.components.editor.AddOtpDialog
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.model.AddType

// --- 添加对话框宿主 ---
@Composable
fun AddDialogHost(
    uiState: VaultUiState,
    vaultViewModel: VaultViewModel,
    onUpdateInteraction: () -> Unit,
    scannerContent: @Composable ((OtpConfig) -> Unit, () -> Unit) -> Unit
) {
    when (uiState.addType) {
        AddType.TOTP -> AddOtpDialog(
            viewModel = vaultViewModel,
            onUpdateInteraction = onUpdateInteraction
        )

        AddType.SCAN -> scannerContent(
            vaultViewModel::addScannedOtp,
            { vaultViewModel.setAddType(null) }
        )

        AddType.BANK_CARD,
        AddType.WIFI,
        AddType.SSH_KEY,
        AddType.ID_CARD,
        AddType.SEED_PHRASE,
        AddType.PASSKEY,
        AddType.RECOVERY_CODE -> {
            val type = uiState.addType
            AddGenericEntryDialog(
                viewModel = vaultViewModel,
                addType = type,
                onUpdateInteraction = onUpdateInteraction
            )
        }

        AddType.PASSWORD,
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
            onConfirm = { vaultViewModel.confirmDelete() },
            onDismiss = { vaultViewModel.setItemToDelete(null) }
        )
    }
}

@Composable
fun VaultDialogs(
    uiState: VaultUiState,
    vaultViewModel: VaultViewModel,
    requestAuthentication: (onSuccess: () -> Unit) -> Unit,
    onUpdateInteraction: () -> Unit,
    scannerContent: @Composable ((OtpConfig) -> Unit, () -> Unit) -> Unit
) {
    AddDialogHost(
        uiState = uiState,
        vaultViewModel = vaultViewModel,
        onUpdateInteraction = onUpdateInteraction,
        scannerContent = scannerContent
    )

    DeleteDialogHost(
        uiState = uiState,
        vaultViewModel = vaultViewModel,
        requestAuthentication = requestAuthentication
    )
}
