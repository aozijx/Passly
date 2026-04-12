package com.aozijx.passly.features.detail.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.designsystem.model.TotpState
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.qr.QrCodeUtils
import com.aozijx.passly.core.security.otp.TotpUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.components.DetailScrollableContent
import com.aozijx.passly.features.detail.components.DetailTopBar
import com.aozijx.passly.features.detail.contract.DetailEvent
import com.aozijx.passly.features.detail.contract.DetailUiState
import com.aozijx.passly.features.detail.internal.EntryEditState
import com.aozijx.passly.features.detail.internal.TotpEditState
import com.aozijx.passly.features.detail.sections.dialogs.QrExportDialog

/**
 * 详情页 UI 组件 (Stateless)
 *
 * 采用状态平铺模式，不直接持有 ViewModel，方便测试和预览。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    initialEntry: VaultEntry,
    uiState: DetailUiState,
    totpStates: Map<Int, TotpState>,
    launchMode: DetailLaunchMode = DetailLaunchMode.VIEW,
    onBack: () -> Unit,
    onEvent: (DetailEvent) -> Unit,
    onUpdateInteraction: () -> Unit,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onShowIconPicker: () -> Unit,
    onAutoUnlockTotp: (VaultEntry) -> Unit,
    onAuthenticate: (activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    activity: FragmentActivity
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // 初始进入和交互更新
    LaunchedEffect(Unit) {
        onUpdateInteraction()
    }

    // 页面数据初始化
    LaunchedEffect(initialEntry.id) {
        onEvent(DetailEvent.Initialize(initialEntry))
    }

    val entry = uiState.entry ?: initialEntry
    val vaultType = uiState.vaultType
    val editState = remember(entry) { EntryEditState(entry) }

    val revealedUsernameState = remember { mutableStateOf<String?>(null) }
    val revealedPasswordState = remember { mutableStateOf<String?>(null) }
    val revealedUsername = revealedUsernameState.value
    val revealedPassword = revealedPasswordState.value

    val currentState = totpStates[entry.id]
    val isSteam = remember(entry.totpAlgorithm) { entry.totpAlgorithm.uppercase() == "STEAM" }
    val totpEditState = remember(entry, currentState?.decryptedSecret) {
        TotpEditState(entry, currentState?.decryptedSecret ?: "")
    }
    var showQrDialog by remember { mutableStateOf(false) }

    // TOTP 自动解锁
    LaunchedEffect(entry.id) {
        if (vaultType == EntryType.TOTP) {
            onAutoUnlockTotp(entry)
        }
    }

    val authQrTitle = stringResource(R.string.vault_auth_qr_title)
    val authQrSubtitle = stringResource(R.string.vault_auth_qr_subtitle)

    // 处理外部启动模式（如编辑 TOTP）
    LaunchedEffect(entry.id, launchMode) {
        if (launchMode == DetailLaunchMode.VIEW) return@LaunchedEffect

        if (launchMode == DetailLaunchMode.EDIT_TOTP) {
            totpEditState.isEditing = true
        } else {
            if (entry.username.isNotEmpty()) {
                editState.isEditingUsername = true
            } else if (entry.password.isNotEmpty()) {
                editState.isEditingPassword = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            revealedUsernameState.value = null
            revealedPasswordState.value = null
            ClipboardUtils.clear(context)
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onUpdateInteraction
            ),
        topBar = {
            DetailTopBar(
                entry = entry,
                uiState = uiState,
                scrollBehavior = scrollBehavior,
                onEvent = onEvent,
                onBack = onBack,
                onInteraction = onUpdateInteraction
            )
        }
    ) { padding ->
        DetailScrollableContent(
            padding = padding,
            entry = entry,
            vaultType = vaultType,
            currentState = currentState,
            isSteam = isSteam,
            totpEditState = totpEditState,
            editState = editState,
            revealedUsername = revealedUsername,
            revealedPassword = revealedPassword,
            onUsernameRevealed = { revealedUsernameState.value = it },
            onPasswordRevealed = { revealedPasswordState.value = it },
            onShowQrDialog = {
                onAuthenticate(
                    activity,
                    authQrTitle,
                    authQrSubtitle
                ) {
                    totpEditState.isEditing = false
                    showQrDialog = true
                }
            },
            onEvent = onEvent,
            onInteraction = onUpdateInteraction,
            onUpdateVaultEntry = onUpdateVaultEntry,
            onShowIconPicker = onShowIconPicker,
            onAuthenticate = onAuthenticate,
            activity = activity
        )
    }

    if (showQrDialog && vaultType == EntryType.TOTP) {
        if (currentState?.decryptedSecret != null) {
            val qrContent = TotpUtils.constructOtpAuthUri(entry, currentState.decryptedSecret)
            val qrBitmap = remember(qrContent) { QrCodeUtils.generateQrCode(qrContent) }
            QrExportDialog(bitmap = qrBitmap, onDismiss = { showQrDialog = false })
        }
    }
}