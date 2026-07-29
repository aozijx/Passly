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
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.DetailUiState
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.detail.sections.ActivityTimelineSection
import com.aozijx.passly.feature.detail.sections.AssociatedInfoSection
import com.aozijx.passly.feature.detail.sections.BankCardSection
import com.aozijx.passly.feature.detail.sections.CredentialSection
import com.aozijx.passly.feature.detail.sections.EntryTypeItem
import com.aozijx.passly.feature.detail.sections.IdCardSection
import com.aozijx.passly.feature.detail.sections.NotesSection
import com.aozijx.passly.feature.detail.sections.PasskeySection
import com.aozijx.passly.feature.detail.sections.SeedPhraseSection
import com.aozijx.passly.feature.detail.sections.RelatedEntriesSection
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
    onAuthenticate: DetailAuthenticate,
    onOpenRelatedEntry: (VaultEntry) -> Unit
) {
    val entry = uiState.entry ?: return
    val registeredSections = DetailSectionResolver.resolve(entry)

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
        if (DetailSectionKey.CREDENTIAL in registeredSections) {
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

        if (DetailSectionKey.OTP in registeredSections) {
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

        if (DetailSectionKey.BANK_CARD in registeredSections) {
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

        if (DetailSectionKey.IDENTITY in registeredSections) {
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

        if (DetailSectionKey.WIFI in registeredSections) {
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

        if (DetailSectionKey.SSH in registeredSections) {
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

        if (DetailSectionKey.SEED_PHRASE in registeredSections) {
            item {
                SeedPhraseSection(
                    entry = entry,
                    revealedSeedPhrase = uiState.revealed(RevealedFieldKey.SEED_PHRASE),
                    onSeedPhraseRevealed = {
                        revealField(RevealedFieldKey.SEED_PHRASE, it)
                    },
                    onAuthenticate = onAuthenticate,
                    onEvent = onEvent,
                )
            }
        }

        if (DetailSectionKey.PASSKEY in registeredSections) {
            item {
                PasskeySection(
                    entry = entry,
                    revealedPasskeyData = uiState.revealed(RevealedFieldKey.PASSKEY_DATA),
                    onRevealField = revealField,
                    onAuthenticate = onAuthenticate,
                    onEvent = onEvent,
                )
            }
        }

        if (uiState.relatedEntries.isNotEmpty()) {
            item {
                RelatedEntriesSection(
                    entries = uiState.relatedEntries,
                    onOpenEntry = onOpenRelatedEntry
                )
            }
        }

        item {
            InfoGroupCard(title = stringResource(R.string.entry_type)) {
                EntryTypeItem(entry.entryType)
            }
        }

        item {
            AssociatedInfoSection(
                entry = entry,
                editState = editState,
                isFaviconDownloading = uiState.isFaviconDownloading,
                onDownloadFavicon = { onEvent(DetailIntent.DownloadFavicon(it)) },
                onEntryUpdated = { onEvent(DetailIntent.CommitEntryUpdate(it)) }
            )
        }

        item {
            NotesSection(
                entry = entry,
                editState = editState,
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
