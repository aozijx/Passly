package com.aozijx.passly.presentation.feature.vault.detail.component

import android.widget.Toast
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
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.ui.vault.detail.component.InfoGroupCard
import com.aozijx.passly.presentation.feature.vault.detail.detailScreenUiModel
import com.aozijx.passly.presentation.ui.vault.detail.component.MetadataSection
import com.aozijx.passly.presentation.ui.vault.detail.component.ActivityTimelineSection
import com.aozijx.passly.presentation.ui.vault.detail.component.AssociatedInfoSection
import com.aozijx.passly.presentation.feature.vault.detail.section.BankCardSection
import com.aozijx.passly.presentation.ui.vault.detail.component.CredentialSection
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionKey
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionResolver
import com.aozijx.passly.presentation.ui.vault.detail.component.EntryCategoryItem
import com.aozijx.passly.presentation.feature.vault.detail.section.IdCardSection
import com.aozijx.passly.presentation.ui.vault.detail.component.NotesSection
import com.aozijx.passly.presentation.feature.vault.detail.section.PasskeySection
import com.aozijx.passly.presentation.ui.vault.detail.component.RelatedEntriesSection
import com.aozijx.passly.presentation.feature.vault.detail.section.SeedPhraseSection
import com.aozijx.passly.presentation.feature.vault.detail.section.SshKeySection
import com.aozijx.passly.presentation.ui.vault.detail.component.TotpSection
import com.aozijx.passly.presentation.feature.vault.detail.section.WifiSection
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailAssociatedInfoUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailNotesUiModel
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun DetailScrollableContent(
    modifier: Modifier = Modifier,
    uiState: DetailUiState,
    editState: EntryEditState,
    otpUiState: OtpCodeState?,
    onAction: (DetailUiAction) -> Unit,
    onInteraction: () -> Unit,
    onAuthenticate: DetailAuthenticate,
    onOpenRelatedEntry: (Entry) -> Unit
) {
    val entry = uiState.entry ?: return
    val screenUiModel = detailScreenUiModel(entry, uiState, otpUiState)
    val registeredSections = DetailSectionResolver.resolve(entry)

    val revealField: (String, SensitiveValue?) -> Unit = { key, value ->
        onAction(DetailUiAction.RevealField(key, value))
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
                        ?.let { onAction(DetailUiAction.DownloadFavicon(it)) }
                },
                onTitleLongClick = { onAction(DetailUiAction.StartTitleEdit) }
            )
        }

        if (DetailSectionKey.CREDENTIAL in registeredSections) {
            item {
                val context = LocalContext.current
                val actionHandler = DetailSectionActionHandler(onAuthenticate, onAction)
                val revealedUsername = uiState.revealed(RevealedFieldKey.USERNAME)
                val revealedPassword = uiState.revealed(RevealedFieldKey.PASSWORD)

                CredentialSection(
                    showUsername = entry.username.isNotBlank() || (SensitiveFieldKey.PASSWORD !in uiState.sensitiveFieldKeys),
                    showPassword = (SensitiveFieldKey.PASSWORD in uiState.sensitiveFieldKeys) || entry.type != EntryType.LOGIN,
                    usernameLabel = stringResource(R.string.field_username),
                    passwordLabel = stringResource(R.string.password_label),
                    revealedUsername = revealedUsername?.let { String(it.toCharArray()) },
                    revealedPassword = revealedPassword?.let { String(it.toCharArray()) },
                    isEditingUsername = editState.isEditingUsername,
                    editedUsername = editState.editedUsername,
                    isEditingPassword = editState.isEditingPassword,
                    editedPassword = editState.editedPassword,
                    onUsernameEditToggled = { editState.isEditingUsername = it },
                    onPasswordEditToggled = { editState.isEditingPassword = it },
                    onUsernameChanged = { editState.editedUsername = it },
                    onPasswordChanged = { editState.editedPassword = it },
                    onUsernameClick = { onAction(DetailUiAction.ToggleVisibility(RevealedFieldKey.USERNAME)) },
                    onPasswordClick = { onAction(DetailUiAction.ToggleVisibility(RevealedFieldKey.PASSWORD)) },
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
                    onUsernameSave = { onAction(DetailUiAction.SaveField(RevealedFieldKey.USERNAME, it)) },
                    onPasswordSave = { onAction(DetailUiAction.SaveField(RevealedFieldKey.PASSWORD, it)) }
                )
            }
        }

        if (DetailSectionKey.OTP in registeredSections) {
            item {
                val context = LocalContext.current
                val otpConfig = entry.secret.otp?.config
                val totpUri =
                    otpConfig?.takeIf { !it.secret.isNullOrBlank() }
                        ?.let { OtpAuthUriCodec.format(it, entry.title) }
                TotpSection(
                    currentState = screenUiModel.otp,
                    totpUri = totpUri,
                    onCodeClick = {
                        screenUiModel.otp?.code?.takeIf { it.isNotEmpty() && !it.contains("-") }?.let { code ->
                            ClipboardUtils.copy(context, code)
                            Toast.makeText(
                                context,
                                context.getString(R.string.field_copy_success_message)
                                    .format(context.getString(R.string.vault_detail_totp_label)),
                                Toast.LENGTH_SHORT,
                            ).show()
                            onAction(DetailUiAction.RecordAction("totp", ActivityType.COPY_PASSWORD))
                        }
                    },
                )
            }
        }

        if (DetailSectionKey.BANK_CARD in registeredSections) {
            item {
                BankCardSection(
                    entry = entry,
                    editState = editState,
                    revealedCardholder = uiState.revealed(RevealedFieldKey.CARDHOLDER)?.let { String(it.toCharArray()) },
                    revealedCardNumber = uiState.revealed(RevealedFieldKey.CARD_NUMBER)?.let { String(it.toCharArray()) },
                    revealedCvv = uiState.revealed(RevealedFieldKey.CVV)?.let { String(it.toCharArray()) },
                    revealedPaymentPin = uiState.revealed(RevealedFieldKey.PAYMENT_PIN)?.let { String(it.toCharArray()) },
                    onRevealField = revealField,
                    onAuthenticate = onAuthenticate,
                    onEntryUpdated = { onAction(DetailUiAction.CommitEntryUpdate(it)) },
                    onAction = onAction
                )
            }
        }

        if (DetailSectionKey.IDENTITY in registeredSections) {
            item {
                IdCardSection(
                    entry = entry,
                    hasIdNumber = SensitiveFieldKey.IDENTITY_NUMBER in uiState.sensitiveFieldKeys,
                    revealedIdNumber = uiState.revealed(RevealedFieldKey.ID_NUMBER)?.let { String(it.toCharArray()) },
                    onIdNumberRevealed = { revealField(RevealedFieldKey.ID_NUMBER, OwnedChars.fromNullableString(it)) },
                    onAuthenticate = onAuthenticate,
                    onAction = onAction
                )
            }
        }

        if (DetailSectionKey.WIFI in registeredSections) {
            item {
                WifiSection(
                    entry = entry,
                    editState = editState,
                    revealedPassword = uiState.revealed(RevealedFieldKey.PASSWORD)?.let { String(it.toCharArray()) },
                    onPasswordRevealed = { revealField(RevealedFieldKey.PASSWORD, OwnedChars.fromNullableString(it)) },
                    onAuthenticate = onAuthenticate,
                    onEntryUpdated = { onAction(DetailUiAction.CommitEntryUpdate(it)) },
                    onAction = onAction
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
                    revealedPassword = uiState.revealed(RevealedFieldKey.SSH_PASSPHRASE)?.let { String(it.toCharArray()) },
                    revealedSshPrivateKey = uiState.revealed(RevealedFieldKey.SSH_PRIVATE_KEY)?.let { String(it.toCharArray()) },
                    onPasswordRevealed = { revealField(RevealedFieldKey.SSH_PASSPHRASE, OwnedChars.fromNullableString(it)) },
                    onAuthenticate = onAuthenticate,
                    onEntryUpdated = { onAction(DetailUiAction.CommitEntryUpdate(it)) },
                    onAction = onAction
                )
            }
        }

        if (DetailSectionKey.SEED_PHRASE in registeredSections) {
            item {
                SeedPhraseSection(
                    hasSeedPhrase = SensitiveFieldKey.SEED_PHRASE in uiState.sensitiveFieldKeys,
                    revealedSeedPhrase = uiState.revealed(RevealedFieldKey.SEED_PHRASE)?.let { String(it.toCharArray()) },
                    onSeedPhraseRevealed = {
                        revealField(RevealedFieldKey.SEED_PHRASE, OwnedChars.fromNullableString(it))
                    },
                    onAuthenticate = onAuthenticate,
                    onAction = onAction,
                )
            }
        }

        if (DetailSectionKey.PASSKEY in registeredSections) {
            item {
                PasskeySection(
                    entry = entry,
                    hasPasskeyData = SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE in uiState.sensitiveFieldKeys,
                    revealedPasskeyData = uiState.revealed(RevealedFieldKey.PASSKEY_DATA)?.let { String(it.toCharArray()) },
                    onRevealField = { key, value -> revealField(key, value?.let { OwnedChars.fromString(it) }) },
                    onAuthenticate = onAuthenticate,
                    onAction = onAction,
                )
            }
        }

        if (uiState.relatedEntries.isNotEmpty()) {
            item {
                RelatedEntriesSection(
                    entries = screenUiModel.relatedEntries,
                    onOpenEntry = { id ->
                        uiState.relatedEntries.firstOrNull { it.id.value == id }?.let(onOpenRelatedEntry)
                    }
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
                model = DetailAssociatedInfoUiModel(
                    domain = entry.associatedDomain,
                    applicationIds = entry.associations.applicationIds.sorted(),
                    isEditingDomain = editState.isEditingDomain,
                    isFaviconDownloading = uiState.isFaviconDownloading,
                ),
                onDownloadFavicon = { onAction(DetailUiAction.DownloadFavicon(it)) },
                onDomainEditStarted = { editState.isEditingDomain = true },
                onDomainChanged = { editState.editedDomain = it },
                onDomainSaved = {
                    editState.editedDomain = it
                    onAction(DetailUiAction.CommitEntryUpdate(editState.applyAssociatedOnly(entry)))
                    editState.isEditingDomain = false
                },
                onPackageSelected = {
                    editState.editedPackage = it
                    onAction(DetailUiAction.CommitEntryUpdate(editState.applyAssociatedOnly(entry)))
                    editState.isEditingPackage = false
                },
            )
        }

        item {
            NotesSection(
                model = DetailNotesUiModel(
                    notes = entry.secret.notes,
                    editedNotes = editState.editedNotes.text,
                    isEditing = editState.isEditingNotes,
                ),
                onEditStarted = { editState.startNotesEditing(entry.secret.notes) },
                onNotesChanged = { editState.editedNotes = TextFieldValue(it) },
                onNotesSaved = {
                    editState.editedNotes = TextFieldValue(it)
                    onAction(DetailUiAction.CommitEntryUpdate(editState.applyNotesOnly(entry)))
                    editState.isEditingNotes = false
                },
            )
        }

        item {
            MetadataSection(screenUiModel.metadata)
        }

        item {
            ActivityTimelineSection(activityList = screenUiModel.activities)
        }
    }
}
