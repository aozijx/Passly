package com.aozijx.passly.feature.vault.presentation.detail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.packageinfo.rememberInstalledAppIconBitmap
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.presentation.common.ui.AppPackagePickerBottomSheet
import com.aozijx.passly.feature.vault.presentation.detail.DetailField
import com.aozijx.passly.feature.vault.presentation.detail.DetailSnapshotRefreshPhase
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBodySectionUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBodyUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.AssociatedInfoSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.BankCardSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.CredentialSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.DetailHistorySection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.DetailIconCard
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.DetailScrollableContent
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.EntryTagsItem
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.IdCardSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.InfoGroupCard
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.MetadataSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.NotesSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.OtpSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.PasskeySection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.RelatedEntriesSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.RelatedPasswordPickerSheet
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.SeedPhraseSection
import com.aozijx.passly.feature.vault.presentation.detail.ui.component.SshKeySection

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailBody(
    model: DetailBodyUiModel,
    otpState: OtpUiState,
    onAction: (DetailUiAction) -> Unit,
    onOpenRelatedEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPackagePicker by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        when (model.snapshotRefreshPhase) {
            DetailSnapshotRefreshPhase.Current -> {
                Unit
            }

            DetailSnapshotRefreshPhase.Refreshing -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LoadingIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.vault_detail_refreshing_after_restore),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            DetailSnapshotRefreshPhase.Failed -> {
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
        }

        DetailScrollableContent(modifier = Modifier.weight(1f).fillMaxWidth()) {
            item {
                DetailIconCard(
                    model = model.icon,
                    onEdit = { onAction(DetailUiAction.OpenFaviconEditor) },
                )
            }
            model.sections.forEach { section ->
                when (section) {
                    is DetailBodySectionUiModel.Credential -> {
                        item {
                            CredentialSection(fields = section.fields, onAction = onAction)
                        }
                    }

                    DetailBodySectionUiModel.WifiQr -> {
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

                    is DetailBodySectionUiModel.Otp -> {
                        item {
                            OtpSection(
                                type = section.type,
                                currentState = otpState,
                                onQrClick = { onAction(DetailUiAction.ShowEntryQr) },
                                onCodeClick = { onAction(DetailUiAction.CopyOtpCode) },
                                onPreviousClick = { onAction(DetailUiAction.RollbackHotpCounter) },
                            )
                        }
                    }

                    is DetailBodySectionUiModel.BankCard -> {
                        item { BankCardSection(model = section.state, onAction = onAction) }
                    }

                    is DetailBodySectionUiModel.Identity -> {
                        item {
                            IdCardSection(
                                model = section.state,
                                onIdNumberCopy = { onAction(DetailUiAction.CopyField(DetailField.ID_NUMBER)) },
                                onIdNumberReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailField.ID_NUMBER)) },
                                onUsernameCopy = { onAction(DetailUiAction.CopyField(DetailField.USERNAME)) },
                            )
                        }
                    }

                    is DetailBodySectionUiModel.Ssh -> {
                        item {
                            val ssh = section.state
                            SshKeySection(
                                model = ssh,
                                onFingerprintCopy = { onAction(DetailUiAction.CopyField(DetailField.USERNAME)) },
                                onPassphraseCopy = { onAction(DetailUiAction.CopyField(DetailField.SSH_PASSPHRASE)) },
                                onPassphraseReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailField.SSH_PASSPHRASE)) },
                                onPassphraseEditStarted = { onAction(DetailUiAction.StartFieldEdit(DetailField.SSH_PASSPHRASE)) },
                                onPassphraseChanged = { onAction(DetailUiAction.UpdateFieldDraft(DetailField.SSH_PASSPHRASE, it)) },
                                onPassphraseSaved = { onAction(DetailUiAction.SaveField(DetailField.SSH_PASSPHRASE, it)) },
                                onPrivateKeyClick = { onAction(DetailUiAction.ActivateSshPrivateKey) },
                                onRevealAll = { onAction(DetailUiAction.RevealSshFields) },
                            )
                        }
                    }

                    is DetailBodySectionUiModel.SeedPhrase -> {
                        item {
                            SeedPhraseSection(
                                state = section.state,
                                onCopy = { onAction(DetailUiAction.CopyField(DetailField.SEED_PHRASE)) },
                                onReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailField.SEED_PHRASE)) },
                            )
                        }
                    }

                    is DetailBodySectionUiModel.Passkey -> {
                        item {
                            PasskeySection(
                                model = section.state,
                                onPasskeyCopy = { onAction(DetailUiAction.CopyField(DetailField.PASSKEY_DATA)) },
                                onPasskeyReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailField.PASSKEY_DATA)) },
                                onHardwareKeyCopy = { onAction(DetailUiAction.CopyField(DetailField.HARDWARE_INFO)) },
                            )
                        }
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
                    state = model.notes,
                    onEditStarted = { onAction(DetailUiAction.StartNotesEdit) },
                    onNotesChanged = { onAction(DetailUiAction.UpdateNotesDraft(it)) },
                    onNotesSaved = { onAction(DetailUiAction.SaveNotes) },
                )
            }
            item { MetadataSection(model.metadata) }
            item {
                DetailHistorySection {
                    onAction(DetailUiAction.OpenRevisionHistory)
                }
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
