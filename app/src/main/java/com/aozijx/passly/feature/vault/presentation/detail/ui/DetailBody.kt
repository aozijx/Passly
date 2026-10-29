package com.aozijx.passly.feature.vault.presentation.detail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.packageinfo.rememberInstalledAppIconBitmap
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.common.ui.AppPackagePickerBottomSheet
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.ActivityTimelineSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.AssociatedInfoSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.BankCardSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.CredentialSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.DetailIconCard
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.DetailScrollableContent
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.EntryTagsItem
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.IdCardSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.InfoGroupCard
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.MetadataSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.NotesSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.PasskeySection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.RelatedEntriesSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.RevisionHistorySection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.SeedPhraseSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.SshKeySection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.TotpSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailBodyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailFieldUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailSectionUiModel

@Composable
fun DetailBody(
    model: DetailBodyUiModel,
    onAction: (DetailUiAction) -> Unit,
    onOpenRelatedEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPackagePicker by remember { mutableStateOf(false) }

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
        if (model.canShowWifiQr) {
            item {
                OutlinedButton(
                    onClick = { onAction(DetailUiAction.ShowEntryQr) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.vault_show_wifi_qr))
                }
            }
        }
        if (DetailSectionUiModel.OTP in model.sections) {
            item {
                TotpSection(
                    currentState = model.otp,
                    onQrClick = { onAction(DetailUiAction.ShowEntryQr) },
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
        item {
            RevisionHistorySection {
                onAction(DetailUiAction.OpenRevisionHistory)
            }
        }
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
