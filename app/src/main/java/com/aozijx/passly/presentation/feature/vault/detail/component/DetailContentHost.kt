package com.aozijx.passly.presentation.feature.vault.detail.component

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.presentation.feature.vault.detail.DetailEntryPatch
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.ui.vault.detail.component.InfoGroupCard
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailIconCard
import com.aozijx.passly.presentation.ui.vault.detail.component.FaviconEditorSheet
import com.aozijx.passly.presentation.ui.vault.detail.component.FaviconCropScreen
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
import com.aozijx.passly.presentation.ui.vault.detail.component.EntryTagsItem
import com.aozijx.passly.presentation.ui.vault.detail.component.IdCardSection
import com.aozijx.passly.presentation.ui.vault.detail.component.NotesSection
import com.aozijx.passly.presentation.ui.vault.detail.component.PasskeySection
import com.aozijx.passly.presentation.ui.vault.detail.component.RelatedEntriesSection
import com.aozijx.passly.presentation.ui.vault.detail.component.SeedPhraseSection
import com.aozijx.passly.presentation.ui.vault.detail.component.SshKeySection
import com.aozijx.passly.presentation.ui.vault.detail.component.TotpSection
import com.aozijx.passly.presentation.ui.vault.detail.component.TagEditorSheet
import com.aozijx.passly.presentation.ui.vault.detail.component.WifiSection
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailAssociatedInfoUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailNotesUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailBankCardUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIdentityUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailWifiUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSshUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialFieldUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialFieldUiState
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialSectionEventHandler
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialSectionUiState
import com.aozijx.passly.presentation.feature.vault.detail.asScopedSensitiveText
import androidx.compose.ui.text.input.TextFieldValue
import com.aozijx.passly.presentation.ui.shared.media.ImageType
import com.aozijx.passly.presentation.ui.shared.media.rememberImagePicker
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIconCardUiModel

@Composable
fun DetailContentHost(
    modifier: Modifier = Modifier,
    uiState: DetailUiState,
    editState: EntryEditState,
    otpUiState: OtpCodeState?,
    otpQrUri: String?,
    onAction: (DetailUiAction) -> Unit,
    onInteraction: () -> Unit,
    onAuthenticate: DetailAuthenticate,
    onCopySensitive: (String) -> Unit,
    onOtpQrDismiss: () -> Unit,
    onOpenRelatedEntry: (Entry) -> Unit
) {
    val entry = uiState.entry ?: return
    val screenUiModel = detailScreenUiModel(entry, uiState, otpUiState)
    val registeredSections = DetailSectionResolver.resolve(entry)
    val pickFaviconImage = rememberImagePicker { uri, _ ->
        onAction(DetailUiAction.PickedFaviconImage(uri))
    }

    LaunchedEffect(uiState.saveCompletionId) {
        if (uiState.saveCompletionId == 0L) return@LaunchedEffect
        when (val completion = uiState.completedEdit) {
            DetailEditCompletion.Notes -> editState.isEditingNotes = false
            DetailEditCompletion.Associations -> {
                editState.isEditingDomain = false
                editState.isEditingPackage = false
            }
            is DetailEditCompletion.SensitiveField -> when (completion.key) {
                RevealedFieldKey.USERNAME,
                RevealedFieldKey.CARDHOLDER,
                -> editState.isEditingUsername = false

                RevealedFieldKey.PASSWORD,
                RevealedFieldKey.CARD_NUMBER,
                RevealedFieldKey.SSH_PASSPHRASE,
                -> editState.isEditingPassword = false

                RevealedFieldKey.CVV -> editState.isEditingTotp = false
                else -> Unit
            }
            else -> Unit
        }
    }

    val revealField: (String, SensitiveValue?) -> Unit = { key, value ->
        onAction(DetailUiAction.RevealField(key, value))
    }

    DetailScrollableContent(modifier = modifier, onInteraction = onInteraction) {
        item {
            DetailIconCard(
                model = DetailIconCardUiModel(
                    iconName = entry.icon.name,
                    iconCustomPath = entry.icon.customReference,
                    iconColor = entry.icon.color,
                    associatedAppPackage = entry.associations.applicationIds.firstOrNull(),
                    entryTypeKey = entry.type.name,
                    title = entry.title,
                    username = entry.username,
                    associatedDomain = entry.associatedDomain,
                ),
                onEdit = { onAction(DetailUiAction.OpenFaviconEditor) },
            )
        }

        if (DetailSectionKey.CREDENTIAL in registeredSections) {
            item {
                val actionHandler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
                val revealedUsername = uiState.revealed(RevealedFieldKey.USERNAME)
                val revealedPassword = uiState.revealed(RevealedFieldKey.PASSWORD)

                CredentialSection(
                    state = CredentialSectionUiState(
                        username = CredentialFieldUiState(
                            visible = entry.username.isNotBlank() || SensitiveFieldKey.PASSWORD !in uiState.sensitiveFieldKeys,
                            label = stringResource(R.string.field_username),
                            revealedValue = revealedUsername?.asScopedSensitiveText(),
                            isEditing = editState.isEditingUsername,
                            editedValue = editState.editedUsername,
                        ),
                        password = CredentialFieldUiState(
                            visible = SensitiveFieldKey.PASSWORD in uiState.sensitiveFieldKeys || entry.type != EntryType.LOGIN,
                            label = stringResource(R.string.password_label),
                            revealedValue = revealedPassword?.asScopedSensitiveText(),
                            isEditing = editState.isEditingPassword,
                            editedValue = editState.editedPassword,
                        ),
                    ),
                    eventHandler = object : CredentialSectionEventHandler {
                        override fun onEditingChanged(field: CredentialFieldUiModel, editing: Boolean) {
                            when (field) {
                                CredentialFieldUiModel.USERNAME -> editState.isEditingUsername = editing
                                CredentialFieldUiModel.PASSWORD -> editState.isEditingPassword = editing
                            }
                        }

                        override fun onValueChanged(field: CredentialFieldUiModel, value: String) {
                            when (field) {
                                CredentialFieldUiModel.USERNAME -> editState.editedUsername = value
                                CredentialFieldUiModel.PASSWORD -> editState.editedPassword = value
                            }
                        }

                        override fun onRevealRequested(field: CredentialFieldUiModel) {
                            onAction(DetailUiAction.ToggleVisibility(field.revealedFieldKey))
                        }

                        override fun onCopyRequested(field: CredentialFieldUiModel) {
                            when (field) {
                                CredentialFieldUiModel.USERNAME -> copySensitiveField(
                                    actionHandler, "username", revealedUsername, entry.username,
                                )
                                CredentialFieldUiModel.PASSWORD -> copySensitiveField(
                                    actionHandler, "password", revealedPassword, entry.secret.login?.password,
                                )
                            }
                        }

                        override fun onSaveRequested(field: CredentialFieldUiModel, value: String) {
                            onAction(DetailUiAction.SaveField(field.revealedFieldKey, value))
                        }
                    },
                )
            }
        }

        if (DetailSectionKey.OTP in registeredSections) {
            item {
                val context = LocalContext.current
                val copySuccessMessage = stringResource(
                    R.string.field_copy_success_message,
                    stringResource(R.string.vault_detail_totp_label),
                )
                TotpSection(
                    currentState = screenUiModel.otp,
                    totpUri = otpQrUri,
                    onQrClick = { onAction(DetailUiAction.ExportOtpQr) },
                    onQrDismiss = onOtpQrDismiss,
                    onCodeClick = {
                        screenUiModel.otp?.code?.takeIf { it.isNotEmpty() && !it.contains("-") }?.let { code ->
                            onCopySensitive(code)
                            Toast.makeText(
                                context,
                                copySuccessMessage,
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
                val actionHandler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
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
                        DetailBankCardFieldUiModel.CARDHOLDER -> onAction(
                            DetailUiAction.CommitPatch(
                                DetailEntryPatch.Username(value),
                                DetailEditCompletion.SensitiveField(RevealedFieldKey.CARDHOLDER),
                            ),
                        )
                        DetailBankCardFieldUiModel.CARD_NUMBER -> onAction(
                            DetailUiAction.CommitPatch(
                                DetailEntryPatch.CardNumber(value),
                                DetailEditCompletion.SensitiveField(RevealedFieldKey.CARD_NUMBER),
                            ),
                        )
                        DetailBankCardFieldUiModel.CVV -> onAction(
                            DetailUiAction.CommitPatch(
                                DetailEntryPatch.CardCvv(value),
                                DetailEditCompletion.SensitiveField(RevealedFieldKey.CVV),
                            ),
                        )
                        else -> Unit
                    } },
                    onCopy = { field ->
                        val (name, revealed, source) = when (field) {
                            DetailBankCardFieldUiModel.CARDHOLDER -> Triple("cardholder", cardholder?.let(OwnedChars::fromString), entry.username)
                            DetailBankCardFieldUiModel.CARD_NUMBER -> Triple("card number", cardNumber?.let(OwnedChars::fromString), null)
                            DetailBankCardFieldUiModel.CVV -> Triple("CVV", cvv?.let(OwnedChars::fromString), null)
                            DetailBankCardFieldUiModel.PAYMENT_PIN -> Triple("payment PIN", paymentPin?.let(OwnedChars::fromString), null)
                            DetailBankCardFieldUiModel.EXPIRATION -> { card?.cardExpiry?.let(actionHandler::copy); onAction(DetailUiAction.RecordAction("expiration", ActivityType.COPY_PASSWORD)); return@BankCardSection }
                        }
                        copySensitiveField(actionHandler, name, revealed, source)
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
                val actionHandler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
                val idNumber = uiState.revealed(RevealedFieldKey.ID_NUMBER)?.let { String(it.toCharArray()) }
                val hasIdNumber = SensitiveFieldKey.IDENTITY_NUMBER in uiState.sensitiveFieldKeys
                IdCardSection(
                    model = DetailIdentityUiModel(hasIdNumber, idNumber, idNumber != null, entry.username),
                    onIdNumberCopy = { copySensitiveField(actionHandler, "ID number", idNumber?.let(OwnedChars::fromString), null) },
                    onIdNumberReveal = { if (idNumber != null) revealField(RevealedFieldKey.ID_NUMBER, null) else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.ID_NUMBER)) },
                    onUsernameCopy = { actionHandler.copy(entry.username); actionHandler.record("username", ActivityType.COPY_PASSWORD) },
                )
            }
        }

        if (DetailSectionKey.WIFI in registeredSections) {
            item {
                val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
                val password = uiState.revealed(RevealedFieldKey.PASSWORD)?.let { String(it.toCharArray()) }
                WifiSection(
                    model = DetailWifiUiModel(entry.username, password, password != null,
                        editState.isEditingPassword, editState.editedPassword,
                        entry.secret.wifi?.securityType ?: "WPA", entry.secret.wifi?.isHidden ?: false),
                    onSsidCopy = { handler.copy(entry.username); handler.record("SSID", ActivityType.COPY_PASSWORD) },
                    onPasswordCopy = { copySensitiveField(handler, "wifi password", password?.let(OwnedChars::fromString), entry.secret.wifi?.password) },
                    onPasswordReveal = { onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.PASSWORD)) },
                    onPasswordEditStarted = { editState.editedPassword = password.orEmpty(); editState.isEditingPassword = true },
                    onPasswordChanged = { editState.editedPassword = it },
                    onPasswordSaved = {
                        if (it != password) {
                            onAction(
                                DetailUiAction.CommitPatch(
                                    DetailEntryPatch.WifiPassword(it),
                                    DetailEditCompletion.SensitiveField(RevealedFieldKey.PASSWORD),
                                ),
                            )
                        }
                    },
                )
            }
        }

        if (DetailSectionKey.SSH in registeredSections) {
            item {
                val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
                val passphrase = uiState.revealed(RevealedFieldKey.SSH_PASSPHRASE)?.let { String(it.toCharArray()) }
                val privateKey = uiState.revealed(RevealedFieldKey.SSH_PRIVATE_KEY)?.let { String(it.toCharArray()) }
                val hasPassphrase = SensitiveFieldKey.SSH_PASSPHRASE in uiState.sensitiveFieldKeys
                val hasPrivateKey = SensitiveFieldKey.SSH_PRIVATE_KEY in uiState.sensitiveFieldKeys
                SshKeySection(
                    model = DetailSshUiModel(entry.username, passphrase, passphrase != null,
                        privateKey, privateKey != null, editState.isEditingPassword,
                        editState.editedPassword, (hasPrivateKey && privateKey == null) || (hasPassphrase && passphrase == null)),
                    onFingerprintCopy = { handler.copy(entry.username); handler.record("fingerprint", ActivityType.COPY_PASSWORD) },
                    onPassphraseCopy = { copySensitiveField(handler, "passphrase", passphrase?.let(OwnedChars::fromString), null) },
                    onPassphraseReveal = { if (passphrase != null) revealField(RevealedFieldKey.SSH_PASSPHRASE, null) else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SSH_PASSPHRASE)) },
                    onPassphraseEditStarted = { editState.editedPassword = passphrase.orEmpty(); editState.isEditingPassword = true },
                    onPassphraseChanged = { editState.editedPassword = it },
                    onPassphraseSaved = {
                        if (it != passphrase) {
                            onAction(
                                DetailUiAction.CommitPatch(
                                    DetailEntryPatch.SshPassphrase(it),
                                    DetailEditCompletion.SensitiveField(RevealedFieldKey.SSH_PASSPHRASE),
                                ),
                            )
                        }
                    },
                    onPrivateKeyClick = { if (privateKey == null) onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SSH_PRIVATE_KEY)) else copySensitiveField(handler, "private key", OwnedChars.fromString(privateKey), null) },
                    onRevealAll = { val keys = buildSet { if (hasPrivateKey && privateKey == null) add(RevealedFieldKey.SSH_PRIVATE_KEY); if (hasPassphrase && passphrase == null) add(RevealedFieldKey.SSH_PASSPHRASE) }; if (keys.isNotEmpty()) onAction(DetailUiAction.RevealHighSensitivityFields(keys)) },
                )
            }
        }

        if (DetailSectionKey.SEED_PHRASE in registeredSections) {
            item {
                val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
                val seed = uiState.revealed(RevealedFieldKey.SEED_PHRASE)?.let { String(it.toCharArray()) }
                SeedPhraseSection(
                    hasSeedPhrase = SensitiveFieldKey.SEED_PHRASE in uiState.sensitiveFieldKeys,
                    revealedSeedPhrase = seed,
                    onCopy = { copySensitiveField(handler, "seed phrase", seed?.let(OwnedChars::fromString), null) },
                    onReveal = { if (seed != null) revealField(RevealedFieldKey.SEED_PHRASE, null) else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.SEED_PHRASE)) },
                )
            }
        }

        if (DetailSectionKey.PASSKEY in registeredSections) {
            item {
                val handler = DetailSectionActionHandler(onAuthenticate, onAction, onCopySensitive)
                val passkeyData = uiState.revealed(RevealedFieldKey.PASSKEY_DATA)?.let { String(it.toCharArray()) }
                val hardwareInfo = entry.secret.passkey?.hardwareKeyInfo
                PasskeySection(
                    hasPasskeyData = SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE in uiState.sensitiveFieldKeys,
                    revealedPasskeyData = passkeyData,
                    hardwareKeyInfo = hardwareInfo,
                    onPasskeyCopy = { copySensitiveField(handler, "passkey data", passkeyData?.let(OwnedChars::fromString), null) },
                    onPasskeyReveal = { if (passkeyData != null) revealField(RevealedFieldKey.PASSKEY_DATA, null) else onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.PASSKEY_DATA)) },
                    onHardwareKeyCopy = { hardwareInfo?.let { handler.copy(it); handler.record("hardware key info", ActivityType.COPY_PASSWORD) } },
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

        item {
            InfoGroupCard(title = stringResource(R.string.vault_detail_tags_title)) {
                EntryTagsItem(
                    tags = entry.tags,
                    onClick = { onAction(DetailUiAction.OpenTagEditor) },
                )
            }
        }

        item {
            AssociatedInfoSection(
                model = DetailAssociatedInfoUiModel(
                    domain = entry.associatedDomain,
                    applicationIds = entry.associations.applicationIds.sorted(),
                    isEditingDomain = editState.isEditingDomain,
                ),
                onDomainEditStarted = { editState.isEditingDomain = true },
                onDomainChanged = { editState.editedDomain = it },
                onDomainSaved = {
                    editState.editedDomain = it
                    onAction(
                        DetailUiAction.CommitPatch(
                            DetailEntryPatch.Associations(
                                primaryUrl = it.trim().ifBlank { null },
                                applicationIds = entry.associations.applicationIds,
                            ),
                            DetailEditCompletion.Associations,
                        ),
                    )
                },
                onPackageSelected = {
                    editState.editedPackage = it
                    onAction(
                        DetailUiAction.CommitPatch(
                            DetailEntryPatch.Associations(
                                primaryUrl = entry.associations.primaryUrl,
                                applicationIds = setOf(it),
                            ),
                            DetailEditCompletion.Associations,
                        ),
                    )
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
                    onAction(
                        DetailUiAction.CommitPatch(
                            DetailEntryPatch.Notes(it.ifBlank { null }),
                            DetailEditCompletion.Notes,
                        ),
                    )
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

    if (uiState.tagEditor.visible) {
        TagEditorSheet(
            state = uiState.tagEditor,
            isSaving = uiState.savingEdit == DetailEditCompletion.Tags,
            onInputChanged = { onAction(DetailUiAction.UpdateTagInput(it)) },
            onSubmit = { onAction(DetailUiAction.SubmitTag(it)) },
            onRemove = { onAction(DetailUiAction.RemoveTag(it)) },
            onSave = { onAction(DetailUiAction.SaveTags) },
            onDismiss = { onAction(DetailUiAction.DismissTagEditor) },
            onConfirmDiscard = { onAction(DetailUiAction.ConfirmDiscardTags) },
            onKeepEditing = { onAction(DetailUiAction.KeepEditingTags) },
        )
    }

    if (uiState.faviconEditor.visible) {
        key(uiState.faviconEditor.presentationId) {
            FaviconEditorSheet(
                state = uiState.faviconEditor,
                isSaving = uiState.savingEdit == DetailEditCompletion.Icon,
                onTabSelected = { onAction(DetailUiAction.SelectFaviconTab(it)) },
                onSearchChanged = { onAction(DetailUiAction.UpdateFaviconSearch(it)) },
                onSourceSelected = { onAction(DetailUiAction.SelectFaviconSource(it)) },
                onUploadRequested = { pickFaviconImage(ImageType.SCREEN) },
                onImageUrlChanged = { onAction(DetailUiAction.UpdateFaviconImageUrl(it)) },
                onDownloadRequested = { onAction(DetailUiAction.DownloadFaviconImage) },
                onSave = { onAction(DetailUiAction.SaveFavicon) },
                onDismiss = { onAction(DetailUiAction.DismissFaviconEditor) },
                onConfirmDiscard = { onAction(DetailUiAction.ConfirmDiscardFavicon) },
                onKeepEditing = { onAction(DetailUiAction.KeepEditingFavicon) },
            )
        }
    }

    uiState.faviconEditor.pendingInputPath?.let { path ->
        FaviconCropScreen(
            stagedPath = path,
            processing = uiState.faviconEditor.processing,
            onCrop = { zoom, x, y ->
                onAction(DetailUiAction.CropFaviconImage(zoom, x, y))
            },
            onUseWithoutCrop = { onAction(DetailUiAction.UseFaviconWithoutCrop) },
            onCancel = { onAction(DetailUiAction.CancelFaviconCrop) },
        )
    }
}

private val CredentialFieldUiModel.revealedFieldKey: String
    get() = when (this) {
        CredentialFieldUiModel.USERNAME -> RevealedFieldKey.USERNAME
        CredentialFieldUiModel.PASSWORD -> RevealedFieldKey.PASSWORD
    }
