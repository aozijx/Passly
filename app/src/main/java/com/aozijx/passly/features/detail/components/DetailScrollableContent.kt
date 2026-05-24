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
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.TotpState
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.features.detail.contract.DetailEvent
import com.aozijx.passly.features.detail.contract.DetailUiState
import com.aozijx.passly.features.detail.contract.RevealedFieldKey
import com.aozijx.passly.features.detail.internal.EntryEditState
import com.aozijx.passly.features.detail.internal.TotpEditState
import com.aozijx.passly.features.detail.sections.AssociatedInfoSection
import com.aozijx.passly.features.detail.sections.BankCardSection
import com.aozijx.passly.features.detail.sections.CategoryItem
import com.aozijx.passly.features.detail.sections.CredentialSection
import com.aozijx.passly.features.detail.sections.HistoryTimelineSection
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

    val revealField: (String, String?) -> Unit = { key, value ->
        onEvent(DetailEvent.RevealField(key, value))
    }

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
                        onAuthenticate = onAuthenticate,
                        editState = editState,
                        revealedUsername = uiState.revealed(RevealedFieldKey.USERNAME),
                        revealedPassword = uiState.revealed(RevealedFieldKey.PASSWORD),
                        onUsernameRevealed = { revealField(RevealedFieldKey.USERNAME, it) },
                        onPasswordRevealed = { revealField(RevealedFieldKey.PASSWORD, it) },
                        onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) },
                        onEvent = onEvent
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
                        onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) },
                        onEvent = onEvent
                    )
                }
            }

            EntryType.WIFI -> {
                item {
                    WifiSection(
                        activity = activity,
                        entry = entry,
                        editState = editState,
                        revealedPassword = uiState.revealed(RevealedFieldKey.PASSWORD),
                        onPasswordRevealed = { revealField(RevealedFieldKey.PASSWORD, it) },
                        onAuthenticate = onAuthenticate,
                        onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) },
                        onEvent = onEvent
                    )
                }
            }

            EntryType.BANK_CARD -> {
                item {
                    BankCardSection(
                        activity = activity,
                        entry = entry,
                        editState = editState,
                        revealedCardholder = uiState.revealed(RevealedFieldKey.CARDHOLDER),
                        revealedCardNumber = uiState.revealed(RevealedFieldKey.CARD_NUMBER),
                        revealedCvv = uiState.revealed(RevealedFieldKey.CVV),
                        revealedPaymentPin = uiState.revealed(RevealedFieldKey.PAYMENT_PIN),
                        onRevealField = revealField,
                        onAuthenticate = onAuthenticate,
                        onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) },
                        onEvent = onEvent
                    )
                }
            }

            EntryType.SEED_PHRASE -> {
                item {
                    SeedPhraseSection(
                        activity = activity,
                        entry = entry,
                        revealedSeedPhrase = uiState.revealed(RevealedFieldKey.SEED_PHRASE),
                        onSeedPhraseRevealed = { revealField(RevealedFieldKey.SEED_PHRASE, it) },
                        onAuthenticate = onAuthenticate,
                        onEvent = onEvent
                    )
                }
            }

            EntryType.SSH_KEY -> {
                item {
                    SshKeySection(
                        activity = activity,
                        entry = entry,
                        editState = editState,
                        revealedPassword = uiState.revealed(RevealedFieldKey.PASSWORD),
                        revealedSshPrivateKey = uiState.revealed(RevealedFieldKey.SSH_PRIVATE_KEY),
                        onPasswordRevealed = { revealField(RevealedFieldKey.PASSWORD, it) },
                        onSshPrivateKeyRevealed = {
                            revealField(
                                RevealedFieldKey.SSH_PRIVATE_KEY,
                                it
                            )
                        },
                        onAuthenticate = onAuthenticate,
                        onEntryUpdated = { onEvent(DetailEvent.CommitEntryUpdate(it)) },
                        onEvent = onEvent
                    )
                }
            }

            EntryType.PASSKEY -> {
                item {
                    PasskeySection(
                        activity = activity,
                        entry = entry,
                        revealedPasskeyData = uiState.revealed(RevealedFieldKey.PASSKEY_DATA),
                        revealedRecoveryCodes = uiState.revealed(RevealedFieldKey.RECOVERY_CODES),
                        onRevealField = revealField,
                        onAuthenticate = onAuthenticate,
                        onEvent = onEvent
                    )
                }
            }

            EntryType.RECOVERY_CODE -> {
                item {
                    RecoveryCodeSection(
                        activity = activity,
                        entry = entry,
                        revealedRecoveryCodes = uiState.revealed(RevealedFieldKey.RECOVERY_CODES),
                        onRecoveryCodesRevealed = {
                            revealField(
                                RevealedFieldKey.RECOVERY_CODES,
                                it
                            )
                        },
                        onAuthenticate = onAuthenticate,
                        onEvent = onEvent
                    )
                }
            }

            EntryType.ID_CARD -> {
                item {
                    IdCardSection(
                        activity = activity,
                        entry = entry,
                        revealedIdNumber = uiState.revealed(RevealedFieldKey.ID_NUMBER),
                        onIdNumberRevealed = { revealField(RevealedFieldKey.ID_NUMBER, it) },
                        onAuthenticate = onAuthenticate,
                        onEvent = onEvent
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

        item {
            HistoryTimelineSection(historyList = uiState.history)
        }
    }
}