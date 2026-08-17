package com.aozijx.passly.feature.detail.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.otp.OtpAuthUriCodec
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.DetailUiState
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.detail.internal.copySensitiveField
import com.aozijx.passly.feature.detail.ui.components.InfoGroupCard
import com.aozijx.passly.feature.detail.ui.components.MetadataSection
import com.aozijx.passly.feature.detail.ui.sections.ActivityTimelineSection
import com.aozijx.passly.feature.detail.ui.sections.AssociatedInfoSection
import com.aozijx.passly.feature.detail.ui.sections.BankCardSection
import com.aozijx.passly.feature.detail.ui.sections.CredentialSection
import com.aozijx.passly.feature.detail.ui.sections.DetailSectionKey
import com.aozijx.passly.feature.detail.ui.sections.DetailSectionResolver
import com.aozijx.passly.feature.detail.ui.sections.EntryCategoryItem
import com.aozijx.passly.feature.detail.ui.sections.IdCardSection
import com.aozijx.passly.feature.detail.ui.sections.NotesSection
import com.aozijx.passly.feature.detail.ui.sections.PasskeySection
import com.aozijx.passly.feature.detail.ui.sections.RelatedEntriesSection
import com.aozijx.passly.feature.detail.ui.sections.SeedPhraseSection
import com.aozijx.passly.feature.detail.ui.sections.SshKeySection
import com.aozijx.passly.feature.detail.ui.sections.TotpSection
import com.aozijx.passly.feature.detail.ui.sections.WifiSection
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
    onOpenRelatedEntry: (Entry) -> Unit
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
        item {
            DetailHeader(
                item = entry,
                onIconClick = {
                    entry.associatedDomain
                        ?.takeIf { it.isNotBlank() }
                        ?.let { onEvent(DetailIntent.DownloadFavicon(it)) }
                },
                onTitleLongClick = { onEvent(DetailIntent.StartTitleEdit) }
            )
        }

        if (DetailSectionKey.CREDENTIAL in registeredSections) {
            item {
                val context = LocalContext.current
                val actionHandler = DetailSectionActionHandler(onAuthenticate, onEvent)
                val revealedUsername = uiState.revealed(RevealedFieldKey.USERNAME)
                val revealedPassword = uiState.revealed(RevealedFieldKey.PASSWORD)

                CredentialSection(
                    showUsername = entry.username.isNotBlank() || (SensitiveFieldKey.PASSWORD !in uiState.sensitiveFieldKeys),
                    showPassword = (SensitiveFieldKey.PASSWORD in uiState.sensitiveFieldKeys) || entry.type != EntryType.LOGIN,
                    usernameLabel = stringResource(R.string.field_username),
                    passwordLabel = stringResource(R.string.password_label),
                    revealedUsername = revealedUsername,
                    revealedPassword = revealedPassword,
                    editState = editState,
                    onUsernameClick = { onEvent(DetailIntent.ToggleVisibility(RevealedFieldKey.USERNAME)) },
                    onPasswordClick = { onEvent(DetailIntent.ToggleVisibility(RevealedFieldKey.PASSWORD)) },
                    onUsernameCopy = {
                        copySensitiveField(
                            context = context,
                            handler = actionHandler,
                            fieldName = "username",
                            revealedValue = revealedUsername,
                            sourceValue = entry.username
                        )
                    },
                    onPasswordCopy = {
                        copySensitiveField(
                            context = context,
                            handler = actionHandler,
                            fieldName = "password",
                            revealedValue = revealedPassword,
                            sourceValue = entry.secret.login?.password
                        )
                    },
                    onUsernameSave = { onEvent(DetailIntent.SaveField(RevealedFieldKey.USERNAME, it)) },
                    onPasswordSave = { onEvent(DetailIntent.SaveField(RevealedFieldKey.PASSWORD, it)) }
                )
            }
        }

        if (DetailSectionKey.OTP in registeredSections) {
            item {
                val otpConfig = entry.secret.otp?.config
                val totpUri =
                    otpConfig?.takeIf { !it.secret.isNullOrBlank() }
                        ?.let { OtpAuthUriCodec.format(it, entry.title) }
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
                    hasIdNumber = SensitiveFieldKey.IDENTITY_NUMBER in uiState.sensitiveFieldKeys,
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
                    hasPassphrase = SensitiveFieldKey.SSH_PASSPHRASE in uiState.sensitiveFieldKeys,
                    hasPrivateKey = SensitiveFieldKey.SSH_PRIVATE_KEY in uiState.sensitiveFieldKeys,
                    revealedPassword = uiState.revealed(RevealedFieldKey.SSH_PASSPHRASE),
                    revealedSshPrivateKey = uiState.revealed(RevealedFieldKey.SSH_PRIVATE_KEY),
                    onPasswordRevealed = { revealField(RevealedFieldKey.SSH_PASSPHRASE, it) },
                    onAuthenticate = onAuthenticate,
                    onEntryUpdated = { onEvent(DetailIntent.CommitEntryUpdate(it)) },
                    onEvent = onEvent
                )
            }
        }

        if (DetailSectionKey.SEED_PHRASE in registeredSections) {
            item {
                SeedPhraseSection(
                    hasSeedPhrase = SensitiveFieldKey.SEED_PHRASE in uiState.sensitiveFieldKeys,
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
                    hasPasskeyData = SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE in uiState.sensitiveFieldKeys,
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

        entry.tags.firstOrNull { it.isNotBlank() }?.trim()?.let { category ->
            item {
                InfoGroupCard(title = stringResource(R.string.field_category)) {
                    EntryCategoryItem(category)
                }
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
