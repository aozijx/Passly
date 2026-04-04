package com.aozijx.passly.features.detail.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.features.detail.components.InfoGroupCard
import com.aozijx.passly.features.detail.components.MetadataSection
import com.aozijx.passly.features.detail.sections.AssociatedInfoSection
import com.aozijx.passly.features.detail.sections.BankCardSection
import com.aozijx.passly.features.detail.sections.CredentialSection
import com.aozijx.passly.features.detail.sections.IdCardSection
import com.aozijx.passly.features.detail.sections.NotesSection
import com.aozijx.passly.features.detail.sections.PasskeySection
import com.aozijx.passly.features.detail.sections.RecoveryCodeSection
import com.aozijx.passly.features.detail.sections.SeedPhraseSection
import com.aozijx.passly.features.detail.sections.SshKeySection
import com.aozijx.passly.features.detail.sections.TotpSection
import com.aozijx.passly.features.detail.sections.WifiSection
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val detailViewModel: DetailViewModel = viewModel()
    val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        mainViewModel.updateInteraction()
    }

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

    val authRevealTitle = stringResource(R.string.vault_auth_decrypt_title)
    val authRevealSubtitle = stringResource(R.string.vault_auth_reveal_subtitle)
    val authQrTitle = stringResource(R.string.vault_auth_qr_title)
    val authQrSubtitle = stringResource(R.string.vault_auth_qr_subtitle)

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
                onClick = { mainViewModel.updateInteraction() }
            ),
        topBar = {
            LargeTopAppBar(
                title = {
                    if (detailUiState.isEditingTitle) {
                        OutlinedTextField(
                            value = detailUiState.editedTitle,
                            onValueChange = { detailViewModel.onEvent(DetailEvent.UpdateEditedTitle(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = entry.title, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.combinedClickable(
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    detailViewModel.onEvent(DetailEvent.StartTitleEdit)
                                },
                                onClick = { /* 不做任何事，只有长按触发 */ }
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        mainViewModel.updateInteraction()
                        if (detailUiState.isEditingTitle) {
                            detailViewModel.onEvent(DetailEvent.CancelTitleEdit)
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            if (detailUiState.isEditingTitle) Icons.Default.Edit else Icons.AutoMirrored.Filled.ArrowBack,
                            if (detailUiState.isEditingTitle) "取消" else "返回"
                        )
                    }
                },
                actions = {
                    if (detailUiState.isEditingTitle) {
                        TextButton(onClick = {
                            mainViewModel.updateInteraction()
                            detailViewModel.onEvent(DetailEvent.SaveTitle)?.let(onEntryUpdated)
                        }) {
                            Icon(Icons.Default.Check, "保存")
                            androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                            Text("保存")
                        }
                    } else {
                        IconButton(onClick = {
                            mainViewModel.updateInteraction()
                            detailViewModel.onEvent(DetailEvent.ToggleFavorite)?.let(onEntryUpdated)
                        }) {
                            Icon(
                                if (entry.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "收藏",
                                tint = if (entry.favorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { mainViewModel.updateInteraction() }
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
    TypeSpecificContent(
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
                }
            )
        },
        activity = activity,
        mainViewModel = mainViewModel,
        vaultViewModel = vaultViewModel,
        onEntryUpdated = onEntryUpdated
    )

            item {
                AssociatedInfoSection(
                    entry = entry,
                    editState = editState,
                    vaultViewModel = vaultViewModel,
                    onEntryUpdated = onEntryUpdated
                )
            }

            item {
                NotesSection(
                    entry = entry,
                    editState = editState,
                    viewModel = vaultViewModel,
                    onEntryUpdated = onEntryUpdated
                )
            }

            item {
                MetadataSection(entry)
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

private fun LazyListScope.TypeSpecificContent(
    entry: VaultEntry,
    vaultType: EntryType,
    currentState: com.aozijx.passly.core.designsystem.state.TotpState?,
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
    onEntryUpdated: (VaultEntry) -> Unit
) {
    when (vaultType) {
        EntryType.PASSWORD -> {
            item {
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
                    onEntryUpdated = onEntryUpdated
                )
            }
        }
        EntryType.TOTP -> {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                TotpSection(
                    entry = entry,
                    currentState = currentState,
                    isSteam = isSteam,
                    totpEditState = totpEditState,
                    showQrDialog = onShowQrDialog,
                    vaultViewModel = vaultViewModel,
                    onEntryUpdated = onEntryUpdated
                )
            }
        }
        EntryType.WIFI -> {
            item {
                WifiSection(
                    activity = activity,
                    entry = entry,
                    editState = editState,
                    revealedPassword = revealedPassword,
                    onPasswordRevealed = onPasswordRevealed,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel,
                    onEntryUpdated = onEntryUpdated
                )
            }
        }
        EntryType.BANK_CARD -> {
            item {
                BankCardSection(
                    activity = activity,
                    entry = entry,
                    editState = editState,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel,
                    onEntryUpdated = onEntryUpdated
                )
            }
        }
        EntryType.SEED_PHRASE -> {
            item {
                SeedPhraseSection(
                    activity = activity,
                    entry = entry,
                    editState = editState,
                    revealedPassword = revealedPassword,
                    onPasswordRevealed = onPasswordRevealed,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel,
                    onEntryUpdated = onEntryUpdated
                )
            }
        }
        EntryType.SSH_KEY -> {
            item {
                SshKeySection(
                    activity = activity,
                    entry = entry,
                    editState = editState,
                    revealedPassword = revealedPassword,
                    onPasswordRevealed = onPasswordRevealed,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel,
                    onEntryUpdated = onEntryUpdated
                )
            }
        }
        EntryType.PASSKEY -> {
            item {
                PasskeySection(
                    activity = activity,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel,
                    entry = entry
                )
            }
        }
        EntryType.RECOVERY_CODE -> {
            item {
                RecoveryCodeSection(
                    activity = activity,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel,
                    entry = entry
                )
            }
        }
        EntryType.ID_CARD -> {
            item {
                IdCardSection(
                    activity = activity,
                    vaultViewModel = vaultViewModel,
                    mainViewModel = mainViewModel,
                    entry = entry
                )
            }
        }
    }

    item {
        InfoGroupCard(title = stringResource(R.string.category)) {
            com.aozijx.passly.features.detail.sections.CategoryItem(
                viewModel = vaultViewModel,
                entry = entry,
                editState = editState,
                onEntryUpdated = onEntryUpdated
            )
        }
    }
}
