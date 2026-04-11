package com.aozijx.passly.features.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.MainViewModel
import com.aozijx.passly.R
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.designsystem.state.TotpEditState
import com.aozijx.passly.core.designsystem.state.TotpState
import com.aozijx.passly.core.designsystem.state.VaultEditState
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.qr.QrCodeUtils
import com.aozijx.passly.core.security.otp.TotpUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.components.DetailHeader
import com.aozijx.passly.features.detail.contract.DetailEffect
import com.aozijx.passly.features.detail.contract.DetailEvent
import com.aozijx.passly.features.detail.page.DetailLaunchMode
import com.aozijx.passly.features.detail.sections.CredentialSection
import com.aozijx.passly.features.detail.sections.TotpSection
import com.aozijx.passly.features.detail.sections.dialogs.QrExportDialog
import com.aozijx.passly.features.vault.VaultViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DetailCardDialog(
    initialEntry: VaultEntry,
    launchMode: DetailLaunchMode = DetailLaunchMode.VIEW,
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val detailViewModel: DetailViewModel = viewModel()
    val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()

    // 核心修复：不要使用 remember(initialEntry) 锁死状态
    // 而是监听来自 ViewModel 的最新 detail 协调状态，确保图标保存后 UI 能感知到
    val currentEntry by remember {
        derivedStateOf { vaultViewModel.detailCoordinatorState.request?.entry ?: initialEntry }
    }

    LaunchedEffect(initialEntry.id) {
        detailViewModel.onEvent(DetailEvent.Initialize(initialEntry))
    }

    LaunchedEffect(currentEntry) {
        detailViewModel.onEvent(DetailEvent.SyncEntry(currentEntry))
    }

    val entry = detailUiState.entry ?: currentEntry
    val vaultType = remember(entry.entryType) { EntryType.fromValue(entry.entryType) }
    val editState = remember(entry) { VaultEditState(entry) }

    var revealedUsername by remember { mutableStateOf<String?>(null) }
    var revealedPassword by remember { mutableStateOf<String?>(null) }

    val totpStates by vaultViewModel.totpStates.collectAsStateWithLifecycle()
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

    LaunchedEffect(detailViewModel) {
        detailViewModel.effects.collectLatest { effect ->
            when (effect) {
                is DetailEffect.EntryUpdated -> vaultViewModel.updateVaultEntry(effect.entry)
                DetailEffect.IconPickerRequested -> vaultViewModel.showDetailIconPicker()
            }
        }
    }

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
            revealedUsername = null
            revealedPassword = null
            ClipboardUtils.clear(context)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp)
                .heightIn(max = 760.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DetailHeader(
                        item = entry,
                        onIconClick = { detailViewModel.onEvent(DetailEvent.ShowIconPicker) },
                        trailingText = entry.category
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    typeSpecificCardContent(
                        entry = entry,
                        vaultType = vaultType,
                        currentState = currentState,
                        isSteam = isSteam,
                        totpEditState = totpEditState,
                        editState = editState,
                        revealedUsername = revealedUsername,
                        revealedPassword = revealedPassword,
                        onUsernameRevealed = { revealedUsername = it },
                        onPasswordRevealed = { revealedPassword = it },
                        onShowQrDialog = {
                            mainViewModel.authenticate(
                                activity = activity,
                                title = authQrTitle,
                                subtitle = authQrSubtitle,
                                onSuccess = {
                                    totpEditState.isEditing = false
                                    showQrDialog = true
                                }
                            )
                        },
                        activity = activity,
                        mainViewModel = mainViewModel,
                        vaultViewModel = vaultViewModel,
                        onEvent = detailViewModel::onEvent
                    )
                }
            }
        }
    }

    if (showQrDialog && vaultType == EntryType.TOTP) {
        if (currentState?.decryptedSecret != null) {
            val qrContent = TotpUtils.constructOtpAuthUri(entry, currentState.decryptedSecret)
            val qrBitmap = remember(qrContent) { QrCodeUtils.generateQrCode(qrContent) }
            QrExportDialog(bitmap = qrBitmap, onDismiss = { showQrDialog = false })
        }
    }
}

private fun LazyListScope.typeSpecificCardContent(
    entry: VaultEntry,
    vaultType: EntryType,
    currentState: TotpState?,
    isSteam: Boolean,
    totpEditState: TotpEditState,
    editState: VaultEditState,
    revealedUsername: String?,
    revealedPassword: String?,
    onUsernameRevealed: (String?) -> Unit,
    onPasswordRevealed: (String?) -> Unit,
    onShowQrDialog: () -> Unit,
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    onEvent: (DetailEvent) -> Unit
) {
    item {
        when (vaultType) {
            EntryType.TOTP -> {
                TotpSection(
                    entry = entry,
                    currentState = currentState,
                    isSteam = isSteam,
                    totpEditState = totpEditState,
                    showQrDialog = onShowQrDialog,
                    vaultViewModel = vaultViewModel,
                    onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) }
                )
            }

            else -> {
                CredentialSection(
                    activity = activity,
                    item = entry,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel,
                    editState = editState,
                    revealedUsername = revealedUsername,
                    revealedPassword = revealedPassword,
                    onUsernameRevealed = onUsernameRevealed,
                    onPasswordRevealed = onPasswordRevealed,
                    onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) }
                )
            }
        }
    }
}