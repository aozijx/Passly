package com.aozijx.passly.presentation.feature.vault.detail.component

import android.widget.Toast
import androidx.compose.runtime.Composable
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
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailHeader
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailScrollableContent
import com.aozijx.passly.presentation.feature.vault.detail.detailScreenUiModel
import com.aozijx.passly.presentation.ui.vault.detail.component.MetadataSection
import com.aozijx.passly.presentation.ui.vault.detail.component.ActivityTimelineSection
import com.aozijx.passly.presentation.ui.vault.detail.component.AssociatedInfoSection
import com.aozijx.passly.presentation.ui.vault.detail.component.BankCardSection
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailBankCardFieldUiModel
import com.aozijx.passly.presentation.ui.vault.detail.component.CredentialSection
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionKey
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionResolver
import com.aozijx.passly.presentation.ui.vault.detail.component.EntryCategoryItem
import com.aozijx.passly.presentation.ui.vault.detail.component.IdCardSection
import com.aozijx.passly.presentation.ui.vault.detail.component.NotesSection
import com.aozijx.passly.presentation.ui.vault.detail.component.PasskeySection
import com.aozijx.passly.presentation.ui.vault.detail.component.RelatedEntriesSection
import com.aozijx.passly.presentation.ui.vault.detail.component.SeedPhraseSection
import com.aozijx.passly.presentation.ui.vault.detail.component.SshKeySection
import com.aozijx.passly.presentation.ui.vault.detail.component.TotpSection
import com.aozijx.passly.presentation.ui.vault.detail.component.WifiSection
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailAssociatedInfoUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailNotesUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailBankCardUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIdentityUiModel
import com.aozijx.passly.presentation.feature.vault.detail.withCardCvv
import com.aozijx.passly.presentation.feature.vault.detail.withCardNumber
import com.aozijx.passly.presentation.feature.vault.detail.withDetailUsername
import com.aozijx.passly.presentation.feature.vault.detail.withWifiPassword
import com.aozijx.passly.presentation.feature.vault.detail.withSshPassphrase
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailWifiUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSshUiModel
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun DetailContentHost(
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

    DetailScrollableContent(modifier = modifier, onInteraction = onInteraction) {
        item {
            DetailHeader(
                iconCustomPath = entry.iconCustomPath,
                updatedAt = entry.updatedAt,
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
                val context = LocalContext.current
                val actionHandler = DetailSectionActionHandler(onAuthenticate, onAction)
                val cardholder = uiState.revealed(RevealedFieldKey.CARDHOLDER)?.let { String(it.toCharArray()) }
                val cardNumber = uiState.revealed(RevealedFieldKey.CARD_NUMBER)?.let { String(it.toCharArray()) }
                val cvv = uiState.revealed(RevealedFieldKey.CVV)?.let { String(it.toCharArray()) }
                val paymentPin = uiState.revealed(RevealedFieldKey.PAYMENT_PIN)?.let { String(it.toCharArray()) }
                val card = entry.secret.card
                val hasNumber = !card?.cardNumber.isNullOrBlank() || cardNumber != null
                val hasCvv = !card?.cardCvv.isNullOrBlank() || cvv != null || editState.isEditingTotp
                val hasPin = !card?.paymentPin.isNullOrBlank() || paymentPin != null
                BankCardSection(
                    model = DetailBankCardUiModel(
                        cardholder ?: entry.username, cardholder != null,
                        cardNumber, cardNumber != null, hasNumber,
                        cvv, cvv != null, hasCvv, card?.cardExpiry,
                        paymentPin, paymentPin != null, hasPin,
                        editState.isEditingUsername, editState.editedUsername,
                        editState.isEditingPassword, editState.editedPassword,
                        editState.isEditingTotp, editState.editedTotp,
                        (hasNumber && cardNumber == null) || (hasCvv && cvv == null) || (hasPin && paymentPin == null),
                    ),
                    onEditChanged = { field, value -> when (field) {
                        DetailBankCardFieldUiModel.CARDHOLDER -> editState.editedUsername = value
                        DetailBankCardFieldUiModel.CARD_NUMBER -> editState.editedPassword = value
                        DetailBankCardFieldUiModel.CVV -> editState.editedTotp = value
                        else -> Unit
                    } },
                    onEditStarted = { field, value -> when (field) {
                        DetailBankCardFieldUiModel.CARDHOLDER -> { editState.editedUsername = value; editState.isEditingUsername = true }
                        DetailBankCardFieldUiModel.CARD_NUMBER -> { editState.editedPassword = value; editState.isEditingPassword = true }
                        DetailBankCardFieldUiModel.CVV -> { editState.editedTotp = value; editState.isEditingTotp = true }
                        else -> Unit
                    } },
                    onEditSaved = { field, value -> when (field) {
                        DetailBankCardFieldUiModel.CARDHOLDER -> { onAction(DetailUiAction.CommitEntryUpdate(entry.withDetailUsername(value))); revealField(RevealedFieldKey.CARDHOLDER, OwnedChars.fromString(value)); editState.isEditingUsername = false }
                        DetailBankCardFieldUiModel.CARD_NUMBER -> { onAction(DetailUiAction.CommitEntryUpdate(entry.withCardNumber(value))); revealField(RevealedFieldKey.CARD_NUMBER, OwnedChars.fromString(value)); editState.isEditingPassword = false }
                        DetailBankCardFieldUiModel.CVV -> { onAction(DetailUiAction.CommitEntryUpdate(entry.withCardCvv(value))); revealField(RevealedFieldKey.CVV, OwnedChars.fromString(value)); editState.isEditingTotp = false }
                        else -> Unit
                    } },
                    onCopy = { field ->
                        val (name, revealed, source) = when (field) {
                            DetailBankCardFieldUiModel.CARDHOLDER -> Triple("cardholder", cardholder?.let(OwnedChars::fromString), entry.username)
                            DetailBankCardFieldUiModel.CARD_NUMBER -> Triple("card number", cardNumber?.let(OwnedChars::fromString), null)
                            DetailBankCardFieldUiModel.CVV -> Triple("CVV", cvv?.let(OwnedChars::fromString), null)
                            DetailBankCardFieldUiModel.PAYMENT_PIN -> Triple("payment PIN", paymentPin?.let(OwnedChars::fromString), null)
                            DetailBankCardFieldUiModel.EXPIRATION -> { card?.cardExpiry?.let { ClipboardUtils.copy(context, it) }; onAction(DetailUiAction.RecordAction("expiration", ActivityType.COPY_PASSWORD)); return@BankCardSection }
                        }
                        copySensitiveField(context, actionHandler, name, revealed, source)
                    },
                    onReveal = { field ->
                        val key = when (field) {
                            DetailBankCardFieldUiModel.CARD_NUMBER -> RevealedFieldKey.CARD_NUMBER
                            DetailBankCardFieldUiModel.CVV -> RevealedFieldKey.CVV
                            DetailBankCardFieldUiModel.PAYMENT_PIN -> RevealedFieldKey.PAYMENT_PIN
                            else -> return@BankCardSection
                        }
                        if (uiState.revealed(key) != null) revealField(key, null)
                        else onAction(DetailUiAction.RevealHighSensitivityField(key))
                    },
                    onRevealAll = {
                        val keys = buildSet { if (hasNumber && cardNumber == null) add(RevealedFieldKey.CARD_NUMBER); if (hasCvv && cvv == null) add(RevealedFieldKey.CVV); if (hasPin && paymentPin == null) add(RevealedFieldKey.PAYMENT_PIN) }
                        if (keys.isNotEmpty()) onAction(DetailUiAction.RevealHighSensitivityFields(keys))
                        if (cardholder == null) revealField(RevealedFieldKey.CARDHOLDER, OwnedChars.fromNullableString(entry.username))
                    },
                )
            }
        }

        if (DetailSectionKey.IDENTITY in registeredSections) {
            item {
                val context = LocalContext.current
                val actionHandler = DetailSectionActionHandler(onAuthenticate, onAction)
                val idNumber = uiState.revealed(RevealedFieldKey.ID_NUMBER)?.let { String(it.toCharArray()) }
                val hasIdNumber = SensitiveFieldKey.IDENTITY_NUMBER in uiState.sensitiveFieldKeys
                IdCardSection(
                    model = DetailIdentityUiModel(hasIdNumber, idNumber, idNumber != null, entry.username),
                    onIdNumberCopy = { copySensitiveField(context, actionHandler, "ID number", idNumber?.let(OwnedChars::fromString), null) },
                    onIdNumberReveal = { if (idNumber != null) revealField(RevealedFieldKey.ID_NUMBER, null) else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.ID_NUMBER)) },
                    onUsernameCopy = { ClipboardUtils.copy(context, entry.username); actionHandler.record("username", ActivityType.COPY_PASSWORD) },
                )
            }
        }

        if (DetailSectionKey.WIFI in registeredSections) {
            item {
                val context = LocalContext.current
                val handler = DetailSectionActionHandler(onAuthenticate, onAction)
                val password = uiState.revealed(RevealedFieldKey.PASSWORD)?.let { String(it.toCharArray()) }
                WifiSection(
                    model = DetailWifiUiModel(entry.username, password, password != null,
                        editState.isEditingPassword, editState.editedPassword,
                        entry.secret.wifi?.securityType ?: "WPA", entry.secret.wifi?.isHidden ?: false),
                    onSsidCopy = { ClipboardUtils.copy(context, entry.username); handler.record("SSID", ActivityType.COPY_PASSWORD) },
                    onPasswordCopy = { copySensitiveField(context, handler, "wifi password", password?.let(OwnedChars::fromString), entry.secret.wifi?.password) },
                    onPasswordReveal = { onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.PASSWORD)) },
                    onPasswordEditStarted = { editState.editedPassword = password.orEmpty(); editState.isEditingPassword = true },
                    onPasswordChanged = { editState.editedPassword = it },
                    onPasswordSaved = { if (it != password) { onAction(DetailUiAction.CommitEntryUpdate(entry.withWifiPassword(it))); revealField(RevealedFieldKey.PASSWORD, OwnedChars.fromString(it)) }; editState.isEditingPassword = false },
                )
            }
        }

        if (DetailSectionKey.SSH in registeredSections) {
            item {
                val context = LocalContext.current
                val handler = DetailSectionActionHandler(onAuthenticate, onAction)
                val passphrase = uiState.revealed(RevealedFieldKey.SSH_PASSPHRASE)?.let { String(it.toCharArray()) }
                val privateKey = uiState.revealed(RevealedFieldKey.SSH_PRIVATE_KEY)?.let { String(it.toCharArray()) }
                val hasPassphrase = SensitiveFieldKey.SSH_PASSPHRASE in uiState.sensitiveFieldKeys
                val hasPrivateKey = SensitiveFieldKey.SSH_PRIVATE_KEY in uiState.sensitiveFieldKeys
                SshKeySection(
                    model = DetailSshUiModel(entry.username, passphrase, passphrase != null,
                        privateKey, privateKey != null, editState.isEditingPassword,
                        editState.editedPassword, (hasPrivateKey && privateKey == null) || (hasPassphrase && passphrase == null)),
                    onFingerprintCopy = { ClipboardUtils.copy(context, entry.username); handler.record("fingerprint", ActivityType.COPY_PASSWORD) },
                    onPassphraseCopy = { copySensitiveField(context, handler, "passphrase", passphrase?.let(OwnedChars::fromString), null) },
                    onPassphraseReveal = { if (passphrase != null) revealField(RevealedFieldKey.SSH_PASSPHRASE, null) else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SSH_PASSPHRASE)) },
                    onPassphraseEditStarted = { editState.editedPassword = passphrase.orEmpty(); editState.isEditingPassword = true },
                    onPassphraseChanged = { editState.editedPassword = it },
                    onPassphraseSaved = { if (it != passphrase) { onAction(DetailUiAction.CommitEntryUpdate(entry.withSshPassphrase(it))); revealField(RevealedFieldKey.SSH_PASSPHRASE, OwnedChars.fromString(it)) }; editState.isEditingPassword = false },
                    onPrivateKeyClick = { if (privateKey == null) onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SSH_PRIVATE_KEY)) else copySensitiveField(context, handler, "private key", OwnedChars.fromString(privateKey), null) },
                    onRevealAll = { val keys = buildSet { if (hasPrivateKey && privateKey == null) add(RevealedFieldKey.SSH_PRIVATE_KEY); if (hasPassphrase && passphrase == null) add(RevealedFieldKey.SSH_PASSPHRASE) }; if (keys.isNotEmpty()) onAction(DetailUiAction.RevealHighSensitivityFields(keys)) },
                )
            }
        }

        if (DetailSectionKey.SEED_PHRASE in registeredSections) {
            item {
                val context = LocalContext.current
                val handler = DetailSectionActionHandler(onAuthenticate, onAction)
                val seed = uiState.revealed(RevealedFieldKey.SEED_PHRASE)?.let { String(it.toCharArray()) }
                SeedPhraseSection(
                    hasSeedPhrase = SensitiveFieldKey.SEED_PHRASE in uiState.sensitiveFieldKeys,
                    revealedSeedPhrase = seed,
                    onCopy = { copySensitiveField(context, handler, "seed phrase", seed?.let(OwnedChars::fromString), null) },
                    onReveal = { if (seed != null) revealField(RevealedFieldKey.SEED_PHRASE, null) else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SEED_PHRASE)) },
                )
            }
        }

        if (DetailSectionKey.PASSKEY in registeredSections) {
            item {
                val context = LocalContext.current
                val handler = DetailSectionActionHandler(onAuthenticate, onAction)
                val passkeyData = uiState.revealed(RevealedFieldKey.PASSKEY_DATA)?.let { String(it.toCharArray()) }
                val hardwareInfo = entry.secret.passkey?.hardwareKeyInfo
                PasskeySection(
                    hasPasskeyData = SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE in uiState.sensitiveFieldKeys,
                    revealedPasskeyData = passkeyData,
                    hardwareKeyInfo = hardwareInfo,
                    onPasskeyCopy = { copySensitiveField(context, handler, "passkey data", passkeyData?.let(OwnedChars::fromString), null) },
                    onPasskeyReveal = { if (passkeyData != null) revealField(RevealedFieldKey.PASSKEY_DATA, null) else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.PASSKEY_DATA)) },
                    onHardwareKeyCopy = { hardwareInfo?.let { ClipboardUtils.copy(context, it); handler.record("hardware key info", ActivityType.COPY_PASSWORD) } },
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
