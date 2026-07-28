package com.aozijx.passly.feature.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.util.TotpUtils
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.DetailUiState
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.detail.sections.ActivityTimelineSection
import com.aozijx.passly.feature.detail.sections.AssociatedInfoSection
import com.aozijx.passly.feature.detail.sections.BankCardSection
import com.aozijx.passly.feature.detail.sections.CategoryItem
import com.aozijx.passly.feature.detail.sections.CredentialSection
import com.aozijx.passly.feature.detail.sections.IdCardSection
import com.aozijx.passly.feature.detail.sections.NotesSection
import com.aozijx.passly.feature.detail.sections.SshKeySection
import com.aozijx.passly.feature.detail.sections.TotpSection
import com.aozijx.passly.feature.detail.sections.WifiSection
import com.aozijx.passly.feature.vault.model.OtpUiState

@Composable
fun DetailScrollableContent(
    modifier: Modifier = Modifier,
    uiState: DetailUiState,
    editState: EntryEditState,
    otpUiState: OtpUiState?,
    onEvent: (DetailIntent) -> Unit,
    onInteraction: () -> Unit,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onAuthenticate: DetailAuthenticate
) {
    val entry = uiState.entry ?: return
    val vaultType = uiState.vaultType

    val revealField: (String, String?) -> Unit = { key, value ->
        onEvent(DetailIntent.RevealField(key, value))
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
        val isCredentialType = vaultType in listOf(
            EntryType.LOGIN, EntryType.OTP, EntryType.SEED_PHRASE, EntryType.RECOVERY_CODE,
            EntryType.PASSKEY, EntryType.NOTE, EntryType.DATABASE,
            EntryType.SERVER, EntryType.API_KEY, EntryType.CRYPTO_WALLET
        )
        if (entry.secret.login != null || isCredentialType) {
            item {
                CredentialSection(
                    item = entry,
                    onAuthenticate = onAuthenticate,
                    editState = editState,
                    revealedUsername = uiState.revealed(RevealedFieldKey.USERNAME),
                    revealedPassword = uiState.revealed(RevealedFieldKey.PASSWORD),
                    onUsernameRevealed = { revealField(RevealedFieldKey.USERNAME, it) },
                    onPasswordRevealed = { revealField(RevealedFieldKey.PASSWORD, it) },
                    onEntryUpdated = { onEvent(DetailIntent.CommitEntryUpdate(it)) },
                    onEvent = onEvent
                )
            }
        }

        if (entry.secret.otp != null || vaultType == EntryType.OTP) {
            item {
                val otpConfig = entry.secret.otp?.config
                val totpUri =
                    otpConfig?.let { TotpUtils.constructOtpAuthUri(it, entry.title) }
                TotpSection(
                    currentState = otpUiState,
                    totpUri = totpUri,
                    onEvent = onEvent
                )
            }
        }

        if (entry.secret.card != null || vaultType == EntryType.BANK_CARD || vaultType == EntryType.CARD) {
            item {
                BankCardSection(
                    entry = entry,
                    editState = editState,
                    revealedCardholder = uiState.revealed(RevealedFieldKey.CARDHOLDER),
                    revealedCardNumber = uiState.revealed(RevealedFieldKey.CARD_NUMBER),
                    revealedCvv = uiState.revealed(RevealedFieldKey.CVV),
                    revealedPaymentPin = uiState.revealed(RevealedFieldKey.PAYMENT_PIN),
                    onRevealField = revealField,
                    onAuthenticate = onAuthenticate,
                    onEntryUpdated = { onEvent(DetailIntent.CommitEntryUpdate(it)) },
                    onEvent = onEvent
                )
            }
        }

        if (entry.secret.identity != null || vaultType in listOf(
                EntryType.ID_CARD,
                EntryType.IDENTITY,
                EntryType.PASSPORT,
                EntryType.LICENSE
            )
        ) {
            item {
                IdCardSection(
                    entry = entry,
                    revealedIdNumber = uiState.revealed(RevealedFieldKey.ID_NUMBER),
                    onIdNumberRevealed = { revealField(RevealedFieldKey.ID_NUMBER, it) },
                    onAuthenticate = onAuthenticate,
                    onEvent = onEvent
                )
            }
        }

        if (entry.secret.wifi != null || vaultType == EntryType.WIFI) {
            item {
                WifiSection(
                    entry = entry,
                    editState = editState,
                    revealedPassword = uiState.revealed(RevealedFieldKey.PASSWORD),
                    onPasswordRevealed = { revealField(RevealedFieldKey.PASSWORD, it) },
                    onAuthenticate = onAuthenticate,
                    onEntryUpdated = { onEvent(DetailIntent.CommitEntryUpdate(it)) },
                    onEvent = onEvent
                )
            }
        }

        if (entry.secret.ssh != null || vaultType == EntryType.SSH_KEY) {
            item {
                SshKeySection(
                    entry = entry,
                    editState = editState,
                    revealedPassword = uiState.revealed(RevealedFieldKey.PASSWORD),
                    revealedSshPrivateKey = uiState.revealed(RevealedFieldKey.SSH_PRIVATE_KEY),
                    onPasswordRevealed = { revealField(RevealedFieldKey.PASSWORD, it) },
                    onSshPrivateKeyRevealed = { revealField(RevealedFieldKey.SSH_PRIVATE_KEY, it) },
                    onAuthenticate = onAuthenticate,
                    onEntryUpdated = { onEvent(DetailIntent.CommitEntryUpdate(it)) },
                    onEvent = onEvent
                )
            }
        }

        item {
            InfoGroupCard(title = stringResource(R.string.category)) {
                CategoryItem(
                    entry = entry,
                    editState = editState,
                    onUpdateVaultEntry = onUpdateVaultEntry,
                    onEntryUpdated = { onEvent(DetailIntent.CommitEntryUpdate(it)) }
                )
            }
        }

        item {
            AssociatedInfoSection(
                entry = entry,
                editState = editState,
                onUpdateVaultEntry = onUpdateVaultEntry,
                onEntryUpdated = { onEvent(DetailIntent.CommitEntryUpdate(it)) }
            )
        }

        item {
            NotesSection(
                entry = entry,
                editState = editState,
                onUpdateVaultEntry = onUpdateVaultEntry,
                onEntryUpdated = { onEvent(DetailIntent.CommitEntryUpdate(it)) }
            )
        }

        item {
            MetadataSection(entry)
        }

        item {
            ActivityTimelineSection(activityList = uiState.history)
        }
    }
}
