package com.aozijx.passly.presentation.ui.vault.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.packageinfo.rememberInstalledAppIconBitmap
import com.aozijx.passly.core.platform.qr.QrCodeEncoder
import com.aozijx.passly.presentation.ui.shared.components.AppPackagePickerBottomSheet
import com.aozijx.passly.presentation.ui.vault.detail.component.ActivityTimelineSection
import com.aozijx.passly.presentation.ui.vault.detail.component.AssociatedInfoSection
import com.aozijx.passly.presentation.ui.vault.detail.component.BankCardSection
import com.aozijx.passly.presentation.ui.vault.detail.component.CredentialSection
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailBankCardFieldUiModel
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailIconCard
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailScrollableContent
import com.aozijx.passly.presentation.ui.vault.detail.component.EntryTagsItem
import com.aozijx.passly.presentation.ui.vault.detail.component.IdCardSection
import com.aozijx.passly.presentation.ui.vault.detail.component.InfoGroupCard
import com.aozijx.passly.presentation.ui.vault.detail.component.MetadataSection
import com.aozijx.passly.presentation.ui.vault.detail.component.NotesSection
import com.aozijx.passly.presentation.ui.vault.detail.component.PasskeySection
import com.aozijx.passly.presentation.ui.vault.detail.component.RelatedEntriesSection
import com.aozijx.passly.presentation.ui.vault.detail.component.SeedPhraseSection
import com.aozijx.passly.presentation.ui.vault.detail.component.SshKeySection
import com.aozijx.passly.presentation.ui.vault.detail.component.TotpQrUiState
import com.aozijx.passly.presentation.ui.vault.detail.component.TotpSection
import com.aozijx.passly.presentation.ui.vault.detail.component.WifiSection
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialFieldUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.CredentialSectionEventHandler
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailBodyUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailContentEvent
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFieldUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSectionUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DetailContent(
    model: DetailBodyUiModel,
    otpQrUri: String?,
    onEvent: (DetailContentEvent) -> Unit,
    onOtpQrDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPackagePicker by remember { mutableStateOf(false) }
    val qrCode by produceState<TotpQrUiState>(
        initialValue = if (otpQrUri == null) TotpQrUiState.Hidden else TotpQrUiState.Loading,
        key1 = otpQrUri,
    ) {
        value = if (otpQrUri == null) {
            TotpQrUiState.Hidden
        } else {
            withContext(Dispatchers.Default) { QrCodeEncoder.encode(otpQrUri) }
                ?.let(TotpQrUiState::Ready) ?: TotpQrUiState.Failed
        }
    }

    DetailScrollableContent(modifier = modifier) {
        item {
            DetailIconCard(model = model.icon, onEdit = { onEvent(DetailContentEvent.EditIcon) })
        }
        if (DetailSectionUiModel.CREDENTIAL in model.sections) {
            model.credential?.let { credential ->
                item {
                    CredentialSection(
                        state = credential,
                        eventHandler = credentialEvents(credential, onEvent),
                    )
                }
            }
        }
        if (DetailSectionUiModel.OTP in model.sections) {
            item {
                TotpSection(
                    currentState = model.otp,
                    qrCode = qrCode,
                    onQrClick = { onEvent(DetailContentEvent.ExportOtpQr) },
                    onQrDismiss = onOtpQrDismiss,
                    onCodeClick = { onEvent(DetailContentEvent.CopyOtpCode) },
                )
            }
        }
        if (DetailSectionUiModel.BANK_CARD in model.sections) {
            model.bankCard?.let { card ->
                item {
                    BankCardSection(
                        model = card,
                        onEditChanged = { field, value ->
                            onEvent(DetailContentEvent.UpdateField(field.toDetailField(), value))
                        },
                        onEditStarted = { field, value ->
                            onEvent(DetailContentEvent.StartFieldEdit(field.toDetailField(), value))
                        },
                        onEditSaved = { field, value ->
                            onEvent(DetailContentEvent.SaveField(field.toDetailField(), value))
                        },
                        onCopy = { onEvent(DetailContentEvent.CopyField(it.toDetailField())) },
                        onReveal = {
                            onEvent(DetailContentEvent.ToggleFieldVisibility(it.toDetailField()))
                        },
                        onRevealAll = {
                            val fields = buildSet {
                                if (card.hasCardNumber && !card.cardNumberRevealed) add(DetailFieldUiModel.CARD_NUMBER)
                                if (card.hasCvv && !card.cvvRevealed) add(DetailFieldUiModel.CARD_CVV)
                                if (card.hasPaymentPin && !card.paymentPinRevealed) add(DetailFieldUiModel.PAYMENT_PIN)
                                if (!card.cardholderRevealed) add(DetailFieldUiModel.CARDHOLDER)
                            }
                            if (fields.isNotEmpty()) onEvent(DetailContentEvent.RevealFields(fields))
                        },
                    )
                }
            }
        }
        if (DetailSectionUiModel.IDENTITY in model.sections) {
            model.identity?.let { identity ->
                item {
                    IdCardSection(
                        model = identity,
                        onIdNumberCopy = { onEvent(DetailContentEvent.CopyField(DetailFieldUiModel.ID_NUMBER)) },
                        onIdNumberReveal = { onEvent(DetailContentEvent.ToggleFieldVisibility(DetailFieldUiModel.ID_NUMBER)) },
                        onUsernameCopy = { onEvent(DetailContentEvent.CopyField(DetailFieldUiModel.USERNAME)) },
                    )
                }
            }
        }
        if (DetailSectionUiModel.WIFI in model.sections) {
            model.wifi?.let { wifi ->
                item {
                    WifiSection(
                        model = wifi,
                        onSsidCopy = { onEvent(DetailContentEvent.CopyField(DetailFieldUiModel.WIFI_SSID)) },
                        onPasswordCopy = { onEvent(DetailContentEvent.CopyField(DetailFieldUiModel.PASSWORD)) },
                        onPasswordReveal = { onEvent(DetailContentEvent.ToggleFieldVisibility(DetailFieldUiModel.PASSWORD)) },
                        onPasswordEditStarted = { onEvent(DetailContentEvent.StartFieldEdit(DetailFieldUiModel.PASSWORD, wifi.password.orEmpty())) },
                        onPasswordChanged = { onEvent(DetailContentEvent.UpdateField(DetailFieldUiModel.PASSWORD, it)) },
                        onPasswordSaved = { if (it != wifi.password) onEvent(DetailContentEvent.SaveField(DetailFieldUiModel.PASSWORD, it)) },
                    )
                }
            }
        }
        if (DetailSectionUiModel.SSH in model.sections) {
            model.ssh?.let { ssh ->
                item {
                    SshKeySection(
                        model = ssh,
                        onFingerprintCopy = { onEvent(DetailContentEvent.CopyField(DetailFieldUiModel.USERNAME)) },
                        onPassphraseCopy = { onEvent(DetailContentEvent.CopyField(DetailFieldUiModel.SSH_PASSPHRASE)) },
                        onPassphraseReveal = { onEvent(DetailContentEvent.ToggleFieldVisibility(DetailFieldUiModel.SSH_PASSPHRASE)) },
                        onPassphraseEditStarted = { onEvent(DetailContentEvent.StartFieldEdit(DetailFieldUiModel.SSH_PASSPHRASE, ssh.passphrase.orEmpty())) },
                        onPassphraseChanged = { onEvent(DetailContentEvent.UpdateField(DetailFieldUiModel.SSH_PASSPHRASE, it)) },
                        onPassphraseSaved = { if (it != ssh.passphrase) onEvent(DetailContentEvent.SaveField(DetailFieldUiModel.SSH_PASSPHRASE, it)) },
                        onPrivateKeyClick = {
                            onEvent(
                                if (ssh.privateKeyRevealed) DetailContentEvent.CopyField(DetailFieldUiModel.SSH_PRIVATE_KEY)
                                else DetailContentEvent.ToggleFieldVisibility(DetailFieldUiModel.SSH_PRIVATE_KEY),
                            )
                        },
                        onRevealAll = {
                            val fields = buildSet {
                                if (ssh.hasPrivateKey && !ssh.privateKeyRevealed) add(DetailFieldUiModel.SSH_PRIVATE_KEY)
                                if (ssh.hasPassphrase && !ssh.passphraseRevealed) add(DetailFieldUiModel.SSH_PASSPHRASE)
                            }
                            if (fields.isNotEmpty()) onEvent(DetailContentEvent.RevealFields(fields))
                        },
                    )
                }
            }
        }
        if (DetailSectionUiModel.SEED_PHRASE in model.sections) {
            model.seedPhrase?.let { seed ->
                item {
                    SeedPhraseSection(
                        hasSeedPhrase = seed.present,
                        revealedSeedPhrase = seed.revealedValue,
                        onCopy = { onEvent(DetailContentEvent.CopyField(DetailFieldUiModel.SEED_PHRASE)) },
                        onReveal = { onEvent(DetailContentEvent.ToggleFieldVisibility(DetailFieldUiModel.SEED_PHRASE)) },
                    )
                }
            }
        }
        if (DetailSectionUiModel.PASSKEY in model.sections) {
            model.passkey?.let { passkey ->
                item {
                    PasskeySection(
                        hasPasskeyData = passkey.present,
                        revealedPasskeyData = passkey.revealedValue,
                        hardwareKeyInfo = passkey.hardwareKeyInfo,
                        onPasskeyCopy = { onEvent(DetailContentEvent.CopyField(DetailFieldUiModel.PASSKEY_DATA)) },
                        onPasskeyReveal = { onEvent(DetailContentEvent.ToggleFieldVisibility(DetailFieldUiModel.PASSKEY_DATA)) },
                        onHardwareKeyCopy = { onEvent(DetailContentEvent.CopyField(DetailFieldUiModel.HARDWARE_INFO)) },
                    )
                }
            }
        }
        if (model.relatedEntries.isNotEmpty()) {
            item {
                RelatedEntriesSection(
                    entries = model.relatedEntries,
                    onOpenEntry = { onEvent(DetailContentEvent.OpenRelatedEntry(it)) },
                )
            }
        }
        item {
            InfoGroupCard(title = stringResource(R.string.vault_detail_tags_title)) {
                EntryTagsItem(model.tags) { onEvent(DetailContentEvent.OpenTagEditor) }
            }
        }
        item {
            AssociatedInfoSection(
                model = model.associations,
                associatedApps = model.associatedApps,
                appIcon = { rememberInstalledAppIconBitmap(it) },
                onDomainEditStarted = { onEvent(DetailContentEvent.StartDomainEdit) },
                onDomainChanged = { onEvent(DetailContentEvent.UpdateDomain(it)) },
                onDomainSaved = { onEvent(DetailContentEvent.SaveDomain) },
                onPackagePickerRequested = {
                    showPackagePicker = true
                    onEvent(DetailContentEvent.OpenPackagePicker)
                },
            )
        }
        item {
            NotesSection(
                model = model.notes,
                onEditStarted = { onEvent(DetailContentEvent.StartNotesEdit) },
                onNotesChanged = { onEvent(DetailContentEvent.UpdateNotes(it)) },
                onNotesSaved = { onEvent(DetailContentEvent.SaveNotes) },
            )
        }
        item { MetadataSection(model.metadata) }
        item { ActivityTimelineSection(model.activities) }
    }

    if (showPackagePicker) {
        AppPackagePickerBottomSheet(
            apps = model.packagePickerApps,
            appIcon = { rememberInstalledAppIconBitmap(it) },
            onSelect = {
                showPackagePicker = false
                onEvent(DetailContentEvent.SelectAssociatedPackage(it.packageName))
            },
            onDismiss = { showPackagePicker = false },
        )
    }
}

private fun credentialEvents(
    state: com.aozijx.passly.presentation.ui.vault.detail.model.CredentialSectionUiState,
    onEvent: (DetailContentEvent) -> Unit,
): CredentialSectionEventHandler = object : CredentialSectionEventHandler {
    override fun onEditingChanged(field: CredentialFieldUiModel, editing: Boolean) {
        val uiField = field.toDetailField()
        onEvent(
            if (editing) DetailContentEvent.StartFieldEdit(
                uiField,
                if (field == CredentialFieldUiModel.USERNAME) state.username.valueForEditing else state.password.valueForEditing,
            )
            else DetailContentEvent.CancelFieldEdit(uiField),
        )
    }

    override fun onValueChanged(field: CredentialFieldUiModel, value: String) {
        onEvent(DetailContentEvent.UpdateField(field.toDetailField(), value))
    }

    override fun onRevealRequested(field: CredentialFieldUiModel) {
        onEvent(DetailContentEvent.ToggleFieldVisibility(field.toDetailField()))
    }

    override fun onCopyRequested(field: CredentialFieldUiModel) {
        onEvent(DetailContentEvent.CopyField(field.toDetailField()))
    }

    override fun onSaveRequested(field: CredentialFieldUiModel, value: String) {
        onEvent(DetailContentEvent.SaveField(field.toDetailField(), value))
    }
}

private fun CredentialFieldUiModel.toDetailField(): DetailFieldUiModel = when (this) {
    CredentialFieldUiModel.USERNAME -> DetailFieldUiModel.USERNAME
    CredentialFieldUiModel.PASSWORD -> DetailFieldUiModel.PASSWORD
}
private fun DetailBankCardFieldUiModel.toDetailField(): DetailFieldUiModel = when (this) {
    DetailBankCardFieldUiModel.CARDHOLDER -> DetailFieldUiModel.CARDHOLDER
    DetailBankCardFieldUiModel.CARD_NUMBER -> DetailFieldUiModel.CARD_NUMBER
    DetailBankCardFieldUiModel.CVV -> DetailFieldUiModel.CARD_CVV
    DetailBankCardFieldUiModel.EXPIRATION -> DetailFieldUiModel.CARD_EXPIRATION
    DetailBankCardFieldUiModel.PAYMENT_PIN -> DetailFieldUiModel.PAYMENT_PIN
}
