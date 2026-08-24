package com.aozijx.passly.presentation.ui.vault.list.component.dialog

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListScreenUiModel

/**
 * 快捷添加的宿主。PASSWORD/TOTP 走全屏编辑器；其余类型暂不支持快捷添加，
 * 选择后以硬编码 Toast 提示（Alert 表单添加已移除）。
 */
@Composable
fun AddDialogHost(
    uiState: VaultListScreenUiModel,
    onDismissAddType: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(uiState.addType) {
        val type = uiState.addType ?: return@LaunchedEffect
        Toast.makeText(context, "该类型暂不支持快捷添加，请使用完整编辑器", Toast.LENGTH_SHORT).show()
        onDismissAddType()
    }
}

@Composable
fun DeleteDialogHost(
    uiState: VaultListScreenUiModel,
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
    uiState: VaultListScreenUiModel,
    onDismissAddType: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    requestAuthentication: (onSuccess: () -> Unit) -> Unit
) {
    AddDialogHost(
        uiState = uiState,
        onDismissAddType = onDismissAddType
    )

    DeleteDialogHost(
        uiState = uiState,
        requestAuthentication = requestAuthentication,
        onConfirmDelete = onConfirmDelete,
        onDismissDelete = onDismissDelete
    )
}
