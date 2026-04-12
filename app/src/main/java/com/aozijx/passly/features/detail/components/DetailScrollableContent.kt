package com.aozijx.passly.features.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.designsystem.model.TotpState
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.contract.DetailEvent
import com.aozijx.passly.features.detail.contract.DetailUiState
import com.aozijx.passly.features.detail.internal.EntryEditState
import com.aozijx.passly.features.detail.internal.TotpEditState
import com.aozijx.passly.features.detail.sections.AssociatedInfoSection
import com.aozijx.passly.features.detail.sections.BankCardSection
import com.aozijx.passly.features.detail.sections.CategoryItem
import com.aozijx.passly.features.detail.sections.CredentialSection
import com.aozijx.passly.features.detail.sections.IdCardSection
import com.aozijx.passly.features.detail.sections.NotesSection
import com.aozijx.passly.features.detail.sections.PasskeySection
import com.aozijx.passly.features.detail.sections.RecoveryCodeSection
import com.aozijx.passly.features.detail.sections.SeedPhraseSection
import com.aozijx.passly.features.detail.sections.SshKeySection
import com.aozijx.passly.features.detail.sections.TotpSection
import com.aozijx.passly.features.detail.sections.WifiSection

@Composable
fun DetailScrollableContent(
    modifier: Modifier = Modifier,
    uiState: DetailUiState,
    currentState: TotpState?,
    isSteam: Boolean,
    totpEditState: TotpEditState,
    editState: EntryEditState,
    onShowQrDialog: () -> Unit,
    onEvent: (DetailEvent) -> Unit,
    onInteraction: () -> Unit,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onShowIconPicker: () -> Unit,
    onAuthenticate: (activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    activity: FragmentActivity
) {
    val entry = uiState.entry ?: return
    val vaultType = uiState.vaultType

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onInteraction
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (vaultType) {
            EntryType.PASSWORD -> {
                item {
                    CredentialSection(
                        activity = activity,
                        item = entry,
                        onUpdateVaultEntry = onUpdateVaultEntry,
                        onAuthenticate = onAuthenticate,
                        editState = editState,
                        revealedUsername = uiState.revealedUsername,
                        revealedPassword = uiState.revealedPassword,
                        onUsernameRevealed = { onEvent(DetailEvent.SetRevealedUsername(it)) },
                        onPasswordRevealed = { onEvent(DetailEvent.SetRevealedPassword(it)) },
                        onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) }
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
                        onUpdateVaultEntry = onUpdateVaultEntry,
                        onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) }
                    )
                }
            }

            EntryType.WIFI -> {
                item {
                    WifiSection(
                        activity = activity,
                        entry = entry,
                        editState = editState,
                        revealedPassword = uiState.revealedPassword,
                        onPasswordRevealed = { onEvent(DetailEvent.SetRevealedPassword(it)) },
                        onUpdateVaultEntry = onUpdateVaultEntry,
                        onAuthenticate = onAuthenticate,
                        onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) }
                    )
                }
            }

            EntryType.BANK_CARD -> {
                item {
                    BankCardSection(
                        activity = activity,
                        entry = entry,
                        editState = editState,
                        onUpdateVaultEntry = onUpdateVaultEntry,
                        onAuthenticate = onAuthenticate,
                        onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) }
                    )
                }
            }

            EntryType.SEED_PHRASE -> {
                item {
                    SeedPhraseSection(
                        activity = activity,
                        entry = entry,
                        editState = editState,
                        revealedPassword = uiState.revealedPassword,
                        onPasswordRevealed = { onEvent(DetailEvent.SetRevealedPassword(it)) },
                        onUpdateVaultEntry = onUpdateVaultEntry,
                        onAuthenticate = onAuthenticate,
                        onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) }
                    )
                }
            }

            EntryType.SSH_KEY -> {
                item {
                    SshKeySection(
                        activity = activity,
                        entry = entry,
                        editState = editState,
                        revealedPassword = uiState.revealedPassword,
                        onPasswordRevealed = { onEvent(DetailEvent.SetRevealedPassword(it)) },
                        onUpdateVaultEntry = onUpdateVaultEntry,
                        onAuthenticate = onAuthenticate,
                        onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) }
                    )
                }
            }

            EntryType.PASSKEY -> {
                item {
                    PasskeySection(
                        activity = activity,
                        onUpdateVaultEntry = onUpdateVaultEntry,
                        onAuthenticate = onAuthenticate,
                        entry = entry
                    )
                }
            }

            EntryType.RECOVERY_CODE -> {
                item {
                    RecoveryCodeSection(
                        activity = activity,
                        onUpdateVaultEntry = onUpdateVaultEntry,
                        onAuthenticate = onAuthenticate,
                        entry = entry
                    )
                }
            }

            EntryType.ID_CARD -> {
                item {
                    IdCardSection(
                        activity = activity,
                        onUpdateVaultEntry = onUpdateVaultEntry,
                        onAuthenticate = onAuthenticate,
                        entry = entry
                    )
                }
            }
        }

        item {
            InfoGroupCard(title = stringResource(R.string.category)) {
                CategoryItem(
                    entry = entry,
                    editState = editState,
                    onUpdateVaultEntry = onUpdateVaultEntry,
                    onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) }
                )
            }
        }

        item {
            AssociatedInfoSection(
                entry = entry,
                editState = editState,
                onUpdateVaultEntry = onUpdateVaultEntry,
                onShowIconPicker = onShowIconPicker,
                onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) }
            )
        }

        item {
            NotesSection(
                entry = entry,
                editState = editState,
                onUpdateVaultEntry = onUpdateVaultEntry,
                onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) }
            )
        }

        item {
            MetadataSection(entry)
        }
    }
}