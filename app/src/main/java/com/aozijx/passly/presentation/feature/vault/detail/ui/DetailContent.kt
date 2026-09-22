package com.aozijx.passly.presentation.feature.vault.detail.ui

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
import com.aozijx.passly.presentation.shared.components.AppPackagePickerBottomSheet
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.ActivityTimelineSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.AssociatedInfoSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.BankCardSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.CredentialSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.DetailBankCardFieldUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.DetailIconCard
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.DetailScrollableContent
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.EntryTagsItem
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.IdCardSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.InfoGroupCard
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.MetadataSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.NotesSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.PasskeySection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.RelatedEntriesSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.SeedPhraseSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.SshKeySection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.TotpQrUiState
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.TotpSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.WifiSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.CredentialFieldUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.CredentialSectionEventHandler
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailBodyUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailContentCallbacks
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFieldUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailSectionUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DetailContent(
    model: DetailBodyUiModel,
    otpQrUri: String?,
    callbacks: DetailContentCallbacks,
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
            DetailIconCard(model = model.icon, onEdit = callbacks::onIconEditRequested)
        }
        if (DetailSectionUiModel.CREDENTIAL in model.sections) {
            model.credential?.let { credential ->
                item {
                    CredentialSection(
                        state = credential,
                        eventHandler = credentialEvents(credential, callbacks),
                    )
                }
            }
        }
        if (DetailSectionUiModel.OTP in model.sections) {
            item {
                TotpSection(
                    currentState = model.otp,
                    qrCode = qrCode,
                    onQrClick = callbacks::onOtpQrRequested,
                    onQrDismiss = callbacks::onOtpQrDismissed,
                    onCodeClick = callbacks::onOtpCopyRequested,
                )
            }
        }
        if (DetailSectionUiModel.BANK_CARD in model.sections) {
            model.bankCard?.let { card ->
                item {
                    BankCardSection(
                        model = card,
                        onEditChanged = { field, value ->
                            callbacks.onBankCardValueChanged(field, value)
                        },
                        onEditStarted = { field, value ->
                            callbacks.onBankCardEditStarted(field, value)
                        },
                        onEditSaved = { field, value ->
                            callbacks.onBankCardSaveRequested(field, value)
                        },
                        onCopy = callbacks::onBankCardCopyRequested,
                        onReveal = callbacks::onBankCardRevealRequested,
                        onRevealAll = callbacks::onBankCardRevealAllRequested,
                    )
                }
            }
        }
        if (DetailSectionUiModel.IDENTITY in model.sections) {
            model.identity?.let { identity ->
                item {
                    IdCardSection(
                        model = identity,
                        onIdNumberCopy = { callbacks.onFieldCopyRequested(DetailFieldUiModel.ID_NUMBER) },
                        onIdNumberReveal = { callbacks.onFieldRevealRequested(DetailFieldUiModel.ID_NUMBER) },
                        onUsernameCopy = { callbacks.onFieldCopyRequested(DetailFieldUiModel.USERNAME) },
                    )
                }
            }
        }
        if (DetailSectionUiModel.WIFI in model.sections) {
            model.wifi?.let { wifi ->
                item {
                    WifiSection(
                        model = wifi,
                        onSsidCopy = { callbacks.onFieldCopyRequested(DetailFieldUiModel.WIFI_SSID) },
                        onPasswordCopy = { callbacks.onFieldCopyRequested(DetailFieldUiModel.PASSWORD) },
                        onPasswordReveal = { callbacks.onFieldRevealRequested(DetailFieldUiModel.PASSWORD) },
                        onPasswordEditStarted = { callbacks.onFieldEditStarted(DetailFieldUiModel.PASSWORD, wifi.password.orEmpty()) },
                        onPasswordChanged = { callbacks.onFieldValueChanged(DetailFieldUiModel.PASSWORD, it) },
                        onPasswordSaved = { if (it != wifi.password) callbacks.onFieldSaveRequested(DetailFieldUiModel.PASSWORD, it) },
                    )
                }
            }
        }
        if (DetailSectionUiModel.SSH in model.sections) {
            model.ssh?.let { ssh ->
                item {
                    SshKeySection(
                        model = ssh,
                        onFingerprintCopy = { callbacks.onFieldCopyRequested(DetailFieldUiModel.USERNAME) },
                        onPassphraseCopy = { callbacks.onFieldCopyRequested(DetailFieldUiModel.SSH_PASSPHRASE) },
                        onPassphraseReveal = { callbacks.onFieldRevealRequested(DetailFieldUiModel.SSH_PASSPHRASE) },
                        onPassphraseEditStarted = { callbacks.onFieldEditStarted(DetailFieldUiModel.SSH_PASSPHRASE, ssh.passphrase.orEmpty()) },
                        onPassphraseChanged = { callbacks.onFieldValueChanged(DetailFieldUiModel.SSH_PASSPHRASE, it) },
                        onPassphraseSaved = { if (it != ssh.passphrase) callbacks.onFieldSaveRequested(DetailFieldUiModel.SSH_PASSPHRASE, it) },
                        onPrivateKeyClick = {
                            if (ssh.privateKeyRevealed) {
                                callbacks.onFieldCopyRequested(DetailFieldUiModel.SSH_PRIVATE_KEY)
                            } else {
                                callbacks.onFieldRevealRequested(DetailFieldUiModel.SSH_PRIVATE_KEY)
                            }
                        },
                        onRevealAll = callbacks::onSshRevealAllRequested,
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
                        onCopy = { callbacks.onFieldCopyRequested(DetailFieldUiModel.SEED_PHRASE) },
                        onReveal = { callbacks.onFieldRevealRequested(DetailFieldUiModel.SEED_PHRASE) },
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
                        onPasskeyCopy = { callbacks.onFieldCopyRequested(DetailFieldUiModel.PASSKEY_DATA) },
                        onPasskeyReveal = { callbacks.onFieldRevealRequested(DetailFieldUiModel.PASSKEY_DATA) },
                        onHardwareKeyCopy = { callbacks.onFieldCopyRequested(DetailFieldUiModel.HARDWARE_INFO) },
                    )
                }
            }
        }
        if (model.relatedEntries.isNotEmpty()) {
            item {
                RelatedEntriesSection(
                    entries = model.relatedEntries,
                    onOpenEntry = callbacks::onRelatedEntryRequested,
                )
            }
        }
        item {
            InfoGroupCard(title = stringResource(R.string.vault_detail_tags_title)) {
                EntryTagsItem(model.tags, callbacks::onTagEditorRequested)
            }
        }
        item {
            AssociatedInfoSection(
                model = model.associations,
                associatedApps = model.associatedApps,
                appIcon = { rememberInstalledAppIconBitmap(it) },
                onDomainEditStarted = callbacks::onDomainEditStarted,
                onDomainChanged = callbacks::onDomainChanged,
                onDomainSaved = callbacks::onDomainSaveRequested,
                onPackagePickerRequested = {
                    showPackagePicker = true
                    callbacks.onPackagePickerRequested()
                },
            )
        }
        item {
            NotesSection(
                model = model.notes,
                onEditStarted = callbacks::onNotesEditStarted,
                onNotesChanged = callbacks::onNotesChanged,
                onNotesSaved = callbacks::onNotesSaveRequested,
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
                callbacks.onPackageSelected(it.packageName)
            },
            onDismiss = { showPackagePicker = false },
        )
    }
}

private fun credentialEvents(
    state: com.aozijx.passly.presentation.feature.vault.detail.ui.model.CredentialSectionUiState,
    callbacks: DetailContentCallbacks,
): CredentialSectionEventHandler = object : CredentialSectionEventHandler {
    override fun onEditingChanged(field: CredentialFieldUiModel, editing: Boolean) {
        callbacks.onCredentialEditingChanged(
            field = field,
            editing = editing,
            initialValue = if (field == CredentialFieldUiModel.USERNAME) {
                state.username.valueForEditing
            } else {
                state.password.valueForEditing
            },
        )
    }

    override fun onValueChanged(field: CredentialFieldUiModel, value: String) {
        callbacks.onCredentialValueChanged(field, value)
    }

    override fun onRevealRequested(field: CredentialFieldUiModel) {
        callbacks.onCredentialRevealRequested(field)
    }

    override fun onCopyRequested(field: CredentialFieldUiModel) {
        callbacks.onCredentialCopyRequested(field)
    }

    override fun onSaveRequested(field: CredentialFieldUiModel, value: String) {
        callbacks.onCredentialSaveRequested(field, value)
    }
}
