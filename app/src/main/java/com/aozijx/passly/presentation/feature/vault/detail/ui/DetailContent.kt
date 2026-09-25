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
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.shared.components.AppPackagePickerBottomSheet
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.ActivityTimelineSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.AssociatedInfoSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.BankCardSection
import com.aozijx.passly.presentation.feature.vault.detail.ui.component.CredentialSection
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
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailBodyUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFieldUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailSectionUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DetailContent(
    model: DetailBodyUiModel,
    otpQrUri: String?,
    onAction: (DetailUiAction) -> Unit,
    onOpenRelatedEntry: (String) -> Unit,
    onOtpQrDismissed: () -> Unit,
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
            DetailIconCard(
                model = model.icon,
                onEdit = { onAction(DetailUiAction.OpenFaviconEditor) },
            )
        }
        if (DetailSectionUiModel.CREDENTIAL in model.sections) {
            model.credential?.let { credential ->
                item {
                    CredentialSection(
                        state = credential,
                        onAction = onAction,
                    )
                }
            }
        }
        if (DetailSectionUiModel.OTP in model.sections) {
            item {
                TotpSection(
                    currentState = model.otp,
                    qrCode = qrCode,
                    onQrClick = { onAction(DetailUiAction.ExportOtpQr) },
                    onQrDismiss = onOtpQrDismissed,
                    onCodeClick = { onAction(DetailUiAction.CopyOtpCode) },
                )
            }
        }
        if (DetailSectionUiModel.BANK_CARD in model.sections) {
            model.bankCard?.let { card ->
                item {
                    BankCardSection(
                        model = card,
                        onAction = onAction,
                    )
                }
            }
        }
        if (DetailSectionUiModel.IDENTITY in model.sections) {
            model.identity?.let { identity ->
                item {
                    IdCardSection(
                        model = identity,
                        onIdNumberCopy = { onAction(DetailUiAction.CopyField(DetailFieldUiModel.ID_NUMBER)) },
                        onIdNumberReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailFieldUiModel.ID_NUMBER)) },
                        onUsernameCopy = { onAction(DetailUiAction.CopyField(DetailFieldUiModel.USERNAME)) },
                    )
                }
            }
        }
        if (DetailSectionUiModel.WIFI in model.sections) {
            model.wifi?.let { wifi ->
                item {
                    WifiSection(
                        model = wifi,
                        onSsidCopy = { onAction(DetailUiAction.CopyField(DetailFieldUiModel.WIFI_SSID)) },
                        onPasswordCopy = { onAction(DetailUiAction.CopyField(DetailFieldUiModel.PASSWORD)) },
                        onPasswordReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailFieldUiModel.PASSWORD)) },
                        onPasswordEditStarted = {
                            onAction(DetailUiAction.StartFieldEdit(DetailFieldUiModel.PASSWORD, wifi.password.orEmpty()))
                        },
                        onPasswordChanged = { onAction(DetailUiAction.UpdateFieldDraft(DetailFieldUiModel.PASSWORD, it)) },
                        onPasswordSaved = {
                            if (it != wifi.password) onAction(DetailUiAction.SaveField(DetailFieldUiModel.PASSWORD, it))
                        },
                    )
                }
            }
        }
        if (DetailSectionUiModel.SSH in model.sections) {
            model.ssh?.let { ssh ->
                item {
                    SshKeySection(
                        model = ssh,
                        onFingerprintCopy = { onAction(DetailUiAction.CopyField(DetailFieldUiModel.USERNAME)) },
                        onPassphraseCopy = { onAction(DetailUiAction.CopyField(DetailFieldUiModel.SSH_PASSPHRASE)) },
                        onPassphraseReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailFieldUiModel.SSH_PASSPHRASE)) },
                        onPassphraseEditStarted = {
                            onAction(DetailUiAction.StartFieldEdit(DetailFieldUiModel.SSH_PASSPHRASE, ssh.passphrase.orEmpty()))
                        },
                        onPassphraseChanged = { onAction(DetailUiAction.UpdateFieldDraft(DetailFieldUiModel.SSH_PASSPHRASE, it)) },
                        onPassphraseSaved = {
                            if (it != ssh.passphrase) onAction(DetailUiAction.SaveField(DetailFieldUiModel.SSH_PASSPHRASE, it))
                        },
                        onPrivateKeyClick = {
                            if (ssh.privateKeyRevealed) {
                                onAction(DetailUiAction.CopyField(DetailFieldUiModel.SSH_PRIVATE_KEY))
                            } else {
                                onAction(DetailUiAction.ToggleFieldVisibility(DetailFieldUiModel.SSH_PRIVATE_KEY))
                            }
                        },
                        onRevealAll = { onAction(DetailUiAction.RevealSshFields) },
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
                        onCopy = { onAction(DetailUiAction.CopyField(DetailFieldUiModel.SEED_PHRASE)) },
                        onReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailFieldUiModel.SEED_PHRASE)) },
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
                        onPasskeyCopy = { onAction(DetailUiAction.CopyField(DetailFieldUiModel.PASSKEY_DATA)) },
                        onPasskeyReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailFieldUiModel.PASSKEY_DATA)) },
                        onHardwareKeyCopy = { onAction(DetailUiAction.CopyField(DetailFieldUiModel.HARDWARE_INFO)) },
                    )
                }
            }
        }
        if (model.relatedEntries.isNotEmpty()) {
            item {
                RelatedEntriesSection(
                    entries = model.relatedEntries,
                    onOpenEntry = onOpenRelatedEntry,
                )
            }
        }
        item {
            InfoGroupCard(title = stringResource(R.string.vault_detail_tags_title)) {
                EntryTagsItem(model.tags) { onAction(DetailUiAction.OpenTagEditor) }
            }
        }
        item {
            AssociatedInfoSection(
                model = model.associations,
                associatedApps = model.associatedApps,
                appIcon = { rememberInstalledAppIconBitmap(it) },
                onDomainEditStarted = { onAction(DetailUiAction.StartDomainEdit) },
                onDomainChanged = { onAction(DetailUiAction.UpdateDomainDraft(it)) },
                onDomainSaved = { onAction(DetailUiAction.SaveDomain) },
                onPackagePickerRequested = {
                    showPackagePicker = true
                    onAction(DetailUiAction.LoadPackagePickerApps)
                },
            )
        }
        item {
            NotesSection(
                model = model.notes,
                onEditStarted = { onAction(DetailUiAction.StartNotesEdit) },
                onNotesChanged = { onAction(DetailUiAction.UpdateNotesDraft(it)) },
                onNotesSaved = { onAction(DetailUiAction.SaveNotes) },
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
                onAction(DetailUiAction.SelectAssociatedPackage(it.packageName))
            },
            onDismiss = { showPackagePicker = false },
        )
    }
}
