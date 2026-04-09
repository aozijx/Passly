package com.aozijx.passly.features.detail.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.MainViewModel
import com.aozijx.passly.R
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.designsystem.state.TotpEditState
import com.aozijx.passly.core.designsystem.state.VaultEditState
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.qr.QrCodeUtils
import com.aozijx.passly.core.security.otp.TotpUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.DetailViewModel
import com.aozijx.passly.features.detail.components.DetailScrollableContent
import com.aozijx.passly.features.detail.components.DetailTopBar
import com.aozijx.passly.features.detail.sections.dialogs.QrExportDialog
import com.aozijx.passly.features.vault.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    initialEntry: VaultEntry,
    onBack: () -> Unit,
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val detailViewModel: DetailViewModel = viewModel()
    val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        mainViewModel.updateInteraction()
    }

    // --- 解决闪烁：由 ViewModel 的同步重置逻辑和这里的 LaunchedEffect 共同保障 ---
    LaunchedEffect(initialEntry.id) {
        detailViewModel.onEvent(DetailEvent.Initialize(initialEntry))
    }

    val entry = detailUiState.entry ?: initialEntry
    val vaultType = detailUiState.vaultType
    val editState = remember(entry) { VaultEditState(entry) }

    val revealedUsernameState = remember { mutableStateOf<String?>(null) }
    val revealedPasswordState = remember { mutableStateOf<String?>(null) }
    val revealedUsername = revealedUsernameState.value
    val revealedPassword = revealedPasswordState.value

    val totpStates by vaultViewModel.totpStates.collectAsState()
    val currentState = totpStates[entry.id]
    val isSteam = remember(entry.totpAlgorithm) { entry.totpAlgorithm.uppercase() == "STEAM" }
    val totpEditState = remember(entry, currentState?.decryptedSecret) {
        TotpEditState(entry, currentState?.decryptedSecret ?: "")
    }
    var showQrDialog by remember { mutableStateOf(false) }

    LaunchedEffect(entry.id) {
        if (vaultType == EntryType.TOTP) {
            vaultViewModel.autoUnlockTotp(entry)
        }
    }

    val authQrTitle = stringResource(R.string.vault_auth_qr_title)
    val authQrSubtitle = stringResource(R.string.vault_auth_qr_subtitle)

    LaunchedEffect(
        entry.id, vaultViewModel.shouldStartDetailInEditMode, vaultViewModel.shouldStartTotpEdit
    ) {
        if (!vaultViewModel.shouldStartDetailInEditMode) return@LaunchedEffect

        if (vaultViewModel.shouldStartTotpEdit) {
            totpEditState.isEditing = true
        } else {
            if (entry.username.isNotEmpty()) {
                editState.isEditingUsername = true
            } else if (entry.password.isNotEmpty()) {
                editState.isEditingPassword = true
            }
        }

        vaultViewModel.consumeDetailLaunchState()
    }

    DisposableEffect(Unit) {
        onDispose {
            revealedUsernameState.value = null
            revealedPasswordState.value = null
            ClipboardUtils.clear(context)
        }
    }

    val onEntryUpdated: (VaultEntry) -> Unit = { updated ->
        detailViewModel.onEvent(DetailEvent.SyncEntry(updated))
        vaultViewModel.updateVaultEntry(updated)
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { mainViewModel.updateInteraction() }),
        topBar = {
            DetailTopBar(
                entry = entry,
                uiState = detailUiState,
                scrollBehavior = scrollBehavior,
                onEvent = detailViewModel::onEvent,
                onBack = onBack,
                onEntryUpdated = onEntryUpdated,
                onInteraction = { mainViewModel.updateInteraction() }
            )
        }
    ) { padding ->
        // --- 解耦后的滚动列表主体 ---
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
                mainViewModel.authenticate(
                    activity = activity,
                    title = authQrTitle,
                    subtitle = authQrSubtitle,
                    onSuccess = {
                        totpEditState.isEditing = false
                        showQrDialog = true
                    })
            },
            activity = activity,
            mainViewModel = mainViewModel,
            vaultViewModel = vaultViewModel,
            onEntryUpdated = onEntryUpdated,
            onInteraction = { mainViewModel.updateInteraction() }
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
