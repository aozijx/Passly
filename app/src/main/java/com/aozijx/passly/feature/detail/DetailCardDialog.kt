package com.aozijx.passly.feature.detail

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.qr.QrCodeUtils
import com.aozijx.passly.core.util.TotpUtils
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.feature.detail.components.DetailHeader
import com.aozijx.passly.feature.detail.contract.DetailEffect
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.detail.internal.TotpEditState
import com.aozijx.passly.feature.detail.page.DetailLaunchMode
import com.aozijx.passly.feature.detail.sections.CredentialSection
import com.aozijx.passly.feature.detail.sections.TotpSection
import com.aozijx.passly.feature.detail.sections.dialogs.QrExportDialog
import com.aozijx.passly.feature.main.MainViewModel
import com.aozijx.passly.feature.vault.model.TotpState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DetailCardDialog(
    initialEntry: VaultEntry,
    launchMode: DetailLaunchMode = DetailLaunchMode.VIEW,
    mainViewModel: MainViewModel,
    totpState: TotpState? = null,
    onDismiss: () -> Unit,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onShowIconPicker: () -> Unit,
    onAutoUnlockTotp: (VaultEntry) -> Unit
) {
    val context = LocalContext.current
    val detailViewModel: DetailViewModel = hiltViewModel()
    val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialEntry.id) {
        detailViewModel.handleIntent(DetailIntent.Initialize(initialEntry))
    }

    LaunchedEffect(initialEntry) {
        detailViewModel.handleIntent(DetailIntent.SyncEntry(initialEntry))
    }

    val entry = detailUiState.entry ?: initialEntry
    val vaultType = entry.entryType
    val editState = remember(entry) { EntryEditState(entry) }

    val currentState = totpState
    val isSteam = remember(entry.credential.twoFactor?.otp?.algorithm ?: "SHA1") { (entry.credential.twoFactor?.otp?.algorithm ?: "SHA1").uppercase() == "STEAM" }
    val totpEditState = remember(entry, currentState?.decryptedSecret) {
        TotpEditState(entry, currentState?.decryptedSecret ?: "")
    }
    var showQrDialog by remember { mutableStateOf(false) }

    val hasTotp = !entry.credential.twoFactor?.otp?.secret.isNullOrBlank()

    LaunchedEffect(entry.id) {
        if (hasTotp) {
            onAutoUnlockTotp(entry)
        }
    }

    LaunchedEffect(detailViewModel) {
        detailViewModel.effects.collectLatest { effect ->
            when (effect) {
                is DetailEffect.EntryUpdated -> onUpdateVaultEntry(effect.entry)
                DetailEffect.IconPickerRequested -> onShowIconPicker()
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
            } else if (entry.credential.password?.isNotEmpty() == true) {
                editState.isEditingPassword = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ClipboardUtils.clear(context)
        }
    }

    Dialog(
        onDismissRequest = {
            detailViewModel.handleIntent(DetailIntent.ClearSensitiveState)
            ClipboardUtils.clear(context)
            onDismiss()
        },
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
                        onIconClick = { detailViewModel.handleIntent(DetailIntent.ShowIconPicker) },
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
                        hasTotp = hasTotp,
                        currentState = currentState,
                        isSteam = isSteam,
                        totpEditState = totpEditState,
                        editState = editState,
                        revealedUsername = detailUiState.revealed(RevealedFieldKey.USERNAME),
                        revealedPassword = detailUiState.revealed(RevealedFieldKey.PASSWORD),
                        onUsernameRevealed = {
                            detailViewModel.handleIntent(
                                DetailIntent.RevealField(
                                    RevealedFieldKey.USERNAME,
                                    it
                                )
                            )
                        },
                        onPasswordRevealed = {
                            detailViewModel.handleIntent(
                                DetailIntent.RevealField(
                                    RevealedFieldKey.PASSWORD,
                                    it
                                )
                            )
                        },
                        onShowQrDialog = {
                            totpEditState.isEditing = false
                            showQrDialog = true
                        },
                        mainViewModel = mainViewModel,
                        onUpdateVaultEntry = onUpdateVaultEntry,
                        onEvent = detailViewModel::handleIntent
                    )
                }
            }
        }
    }

    if (showQrDialog && hasTotp) {
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
    hasTotp: Boolean,
    currentState: TotpState?,
    isSteam: Boolean,
    totpEditState: TotpEditState,
    editState: EntryEditState,
    revealedUsername: String?,
    revealedPassword: String?,
    onUsernameRevealed: (String?) -> Unit,
    onPasswordRevealed: (String?) -> Unit,
    onShowQrDialog: () -> Unit,
    mainViewModel: MainViewModel,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onEvent: (DetailIntent) -> Unit
) {
    item {
        when (vaultType) {
            else -> {
                CredentialSection(
                    item = entry,
                    onAuthenticate = { onSuccess ->
                        mainViewModel.requestAuth(onSuccess = onSuccess)
                    },
                    editState = editState,
                    revealedUsername = revealedUsername,
                    revealedPassword = revealedPassword,
                    onUsernameRevealed = onUsernameRevealed,
                    onPasswordRevealed = onPasswordRevealed,
                    onEntryUpdated = { onEvent(DetailIntent.CommitEntryUpdate(it)) },
                    onEvent = onEvent
                )
            }
        }
    }

    if (hasTotp) {
        item {
            TotpSection(
                entry = entry,
                currentState = currentState,
                isSteam = isSteam,
                totpEditState = totpEditState,
                showQrDialog = onShowQrDialog,
                onUpdateVaultEntry = onUpdateVaultEntry,
                onEntryUpdated = { onEvent(DetailIntent.CommitEntryUpdate(it)) },
                onEvent = onEvent
            )
        }
    }
}
