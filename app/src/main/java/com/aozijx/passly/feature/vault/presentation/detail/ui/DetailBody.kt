package com.aozijx.passly.feature.vault.presentation.detail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.packageinfo.rememberInstalledAppIconBitmap
import com.aozijx.passly.feature.vault.presentation.common.ui.AppPackagePickerBottomSheet
import com.aozijx.passly.feature.vault.presentation.detail.DetailSnapshotState
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
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
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.RelatedPasswordPickerSheet
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.RevisionHistorySection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.SeedPhraseSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.SshKeySection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.TotpSection
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBodyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.DetailField
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey

@Composable
fun DetailBody(
    model: DetailBodyUiModel,
    onAction: (DetailUiAction) -> Unit,
    onOpenRelatedEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPackagePicker by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        when (model.snapshotState) {
            DetailSnapshotState.RefreshingAfterRestore -> {
                Text(
                    text = stringResource(R.string.vault_detail_refreshing_after_restore),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DetailSnapshotState.RefreshFailedAfterRestore -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.vault_detail_refresh_failed_after_restore),
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = { onAction(DetailUiAction.RetrySnapshotRefresh) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            else -> {
                Unit
            }
        }

        DetailScrollableContent(modifier = Modifier.weight(1f).fillMaxWidth()) {
            item {
                DetailIconCard(
                    model = model.icon,
                    onEdit = { onAction(DetailUiAction.OpenFaviconEditor) },
                )
            }
            if (DetailSectionKey.CREDENTIAL in model.sections) {
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
            if (DetailSectionKey.OTP in model.sections) {
                item {
                    TotpSection(
                        currentState = model.otp,
                        onQrClick = { onAction(DetailUiAction.ShowEntryQr) },
                        onCodeClick = { onAction(DetailUiAction.CopyOtpCode) },
                    )
                }
            }
            if (DetailSectionKey.BANK_CARD in model.sections) {
                model.bankCard?.let { card ->
                    item {
                        BankCardSection(
                            model = card,
                            onAction = onAction,
                        )
                    }
                }
            }
            if (DetailSectionKey.IDENTITY in model.sections) {
                model.identity?.let { identity ->
                    item {
                        IdCardSection(
                            model = identity,
                            onIdNumberCopy = { onAction(DetailUiAction.CopyField(DetailField.ID_NUMBER)) },
                            onIdNumberReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailField.ID_NUMBER)) },
                            onUsernameCopy = { onAction(DetailUiAction.CopyField(DetailField.USERNAME)) },
                        )
                    }
                }
            }
            if (DetailSectionKey.SSH in model.sections) {
                model.ssh?.let { ssh ->
                    item {
                        SshKeySection(
                            model = ssh,
                            onFingerprintCopy = { onAction(DetailUiAction.CopyField(DetailField.USERNAME)) },
                            onPassphraseCopy = { onAction(DetailUiAction.CopyField(DetailField.SSH_PASSPHRASE)) },
                            onPassphraseReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailField.SSH_PASSPHRASE)) },
                            onPassphraseEditStarted = {
                                onAction(
                                    DetailUiAction.StartFieldEdit(
                                        DetailField.SSH_PASSPHRASE,
                                        ssh.passphrase.revealedValue.orEmpty(),
                                    ),
                                )
                            },
                            onPassphraseChanged = { onAction(DetailUiAction.UpdateFieldDraft(DetailField.SSH_PASSPHRASE, it)) },
                            onPassphraseSaved = {
                                if (it != ssh.passphrase.revealedValue) {
                                    onAction(DetailUiAction.SaveField(DetailField.SSH_PASSPHRASE, it))
                                }
                            },
                            onPrivateKeyClick = {
                                if (ssh.privateKey.isRevealed) {
                                    onAction(DetailUiAction.CopyField(DetailField.SSH_PRIVATE_KEY))
                                } else {
                                    onAction(DetailUiAction.ToggleFieldVisibility(DetailField.SSH_PRIVATE_KEY))
                                }
                            },
                            onRevealAll = { onAction(DetailUiAction.RevealSshFields) },
                        )
                    }
                }
            }
            if (DetailSectionKey.SEED_PHRASE in model.sections) {
                model.seedPhrase?.let { seed ->
                    item {
                        SeedPhraseSection(
                            state = seed,
                            onCopy = { onAction(DetailUiAction.CopyField(DetailField.SEED_PHRASE)) },
                            onReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailField.SEED_PHRASE)) },
                        )
                    }
                }
            }
            if (DetailSectionKey.PASSKEY in model.sections) {
                model.passkey?.let { passkey ->
                    item {
                        PasskeySection(
                            model = passkey,
                            onPasskeyCopy = { onAction(DetailUiAction.CopyField(DetailField.PASSKEY_DATA)) },
                            onPasskeyReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailField.PASSKEY_DATA)) },
                            onHardwareKeyCopy = { onAction(DetailUiAction.CopyField(DetailField.HARDWARE_INFO)) },
                        )
                    }
                }
            }
            item {
                RelatedEntriesSection(
                    state = model.relatedSections,
                    unlinkState = model.passwordUnlink,
                    onAction = onAction,
                    onOpenEntry = onOpenRelatedEntry,
                )
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
            item {
                ActivityTimelineSection(
                    selectedFilter = model.activityFilter,
                    onFilterChanged = { onAction(DetailUiAction.ActivityFilterChanged(it)) },
                    state = model.activityHistory,
                    onRetry = { onAction(DetailUiAction.RetryActivityHistory) },
                )
            }
        }
    }

    if (showPackagePicker) {
        AppPackagePickerBottomSheet(
            state = model.packagePickerApps,
            onQueryChanged = { onAction(DetailUiAction.PackagePickerSearchChanged(it)) },
            onRetry = { onAction(DetailUiAction.LoadPackagePickerApps) },
            appIcon = { rememberInstalledAppIconBitmap(it) },
            onSelect = {
                showPackagePicker = false
                onAction(DetailUiAction.SelectAssociatedPackage(it.packageName))
            },
            onDismiss = { showPackagePicker = false },
        )
    }

    RelatedPasswordPickerSheet(
        state = model.passwordBinding,
        onAction = onAction,
    )
}
