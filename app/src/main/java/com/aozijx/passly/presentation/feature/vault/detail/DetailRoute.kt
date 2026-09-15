package com.aozijx.passly.presentation.feature.vault.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.presentation.feature.vault.detail.binding.DetailBodyBinding
import com.aozijx.passly.presentation.ui.vault.detail.DetailScreen

/**
 * 详情页 UI 组件 (Stateless)
 *
 * 采用状态平铺模式，不直接持有 ViewModel，方便测试和预览。
 */
@Composable
fun DetailRoute(
    initialEntry: Entry,
    uiState: DetailUiState,
    otpUiState: OtpCodeState?,
    otpQrUri: String?,
    launchMode: DetailLaunchMode = DetailLaunchMode.VIEW,
    onAction: (DetailUiAction) -> Unit,
    onBack: () -> Unit,
    onUpdateInteraction: () -> Unit,
    onAutoUnlockTotp: (Entry) -> Unit,
    onOtpQrDismiss: () -> Unit,
    onOpenRelatedEntry: (Entry) -> Unit
) {

    // 初始进入和交互更新
    LaunchedEffect(Unit) {
        onUpdateInteraction()
    }

    // 页面数据初始化（同 key 内串联首次 TOTP 自动解锁，避免重复 effect 触发）
    LaunchedEffect(initialEntry.id) {
        onAction(DetailUiAction.Initialize(initialEntry))
        if (initialEntry.shouldAutoActivateOtp()) {
            onAutoUnlockTotp(initialEntry)
        }
    }

    val entry = uiState.entry ?: initialEntry
    val editState = remember(entry) { EntryEditState(entry) }

    // 处理外部启动模式（如编辑 TOTP）
    LaunchedEffect(entry.id, launchMode) {
        if (launchMode == DetailLaunchMode.VIEW) return@LaunchedEffect

        if (entry.username.isNotEmpty()) {
            editState.isEditingUsername = true
        } else if (com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey.PASSWORD in
            uiState.sensitiveFieldKeys
        ) {
            editState.isEditingPassword = true
        }
    }

    DetailScreen(
        model = detailHeaderUiModel(entry, uiState),
        onBack = onBack,
        onInteraction = onUpdateInteraction,
        onTitleChanged = { onAction(DetailUiAction.UpdateEditedTitle(it)) },
        onTitleEditStarted = { onAction(DetailUiAction.StartTitleEdit) },
        onTitleSaved = { onAction(DetailUiAction.SaveTitle) },
        onFavoriteToggled = { onAction(DetailUiAction.ToggleFavorite) },
    ) { modifier ->
        DetailBodyBinding(
            modifier = modifier,
            uiState = uiState,
            editState = editState,
            otpUiState = otpUiState,
            otpQrUri = otpQrUri,
            onAction = onAction,
            onInteraction = onUpdateInteraction,
            onOtpQrDismiss = onOtpQrDismiss,
            onOpenRelatedEntry = onOpenRelatedEntry
        )
    }
}

internal fun Entry.shouldAutoActivateOtp(): Boolean = secret.otp != null
