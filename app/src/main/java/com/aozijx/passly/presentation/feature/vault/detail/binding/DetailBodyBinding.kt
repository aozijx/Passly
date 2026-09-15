package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.detailContentUiModel
import com.aozijx.passly.presentation.feature.vault.detail.detailOtpUiModel
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionKey
import com.aozijx.passly.presentation.feature.vault.detail.section.DetailSectionResolver
import com.aozijx.passly.presentation.ui.vault.detail.component.ActivityTimelineSection
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailIconCard
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailScrollableContent
import com.aozijx.passly.presentation.ui.vault.detail.component.MetadataSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailIconCardUiModel

@Composable
fun DetailBodyBinding(
    modifier: Modifier = Modifier,
    uiState: DetailUiState,
    editState: EntryEditState,
    otpUiState: OtpCodeState?,
    otpQrUri: String?,
    onAction: (DetailUiAction) -> Unit,
    onInteraction: () -> Unit,
    onOtpQrDismiss: () -> Unit,
    onOpenRelatedEntry: (Entry) -> Unit,
) {
    val entry = uiState.entry ?: return
    val contentUiModel = remember(entry, uiState.relatedEntries, uiState.history) {
        detailContentUiModel(entry, uiState)
    }
    val otpModel = detailOtpUiModel(otpUiState)
    val registeredSections = DetailSectionResolver.resolve(entry)

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
            item { DetailCredentialBinding(entry, uiState, editState, onAction) }
        }
        if (DetailSectionKey.OTP in registeredSections) {
            item { DetailOtpBinding(otpModel, otpQrUri, onAction, onOtpQrDismiss) }
        }
        if (DetailSectionKey.BANK_CARD in registeredSections) {
            item { DetailBankCardBinding(entry, uiState, editState, onAction) }
        }
        if (DetailSectionKey.IDENTITY in registeredSections) {
            item { DetailIdentityBinding(entry, uiState, onAction) }
        }
        if (DetailSectionKey.WIFI in registeredSections) {
            item { DetailWifiBinding(entry, uiState, editState, onAction) }
        }
        if (DetailSectionKey.SSH in registeredSections) {
            item { DetailSshBinding(entry, uiState, editState, onAction) }
        }
        if (DetailSectionKey.SEED_PHRASE in registeredSections) {
            item { DetailSeedPhraseBinding(uiState, onAction) }
        }
        if (DetailSectionKey.PASSKEY in registeredSections) {
            item { DetailPasskeyBinding(entry, uiState, onAction) }
        }
        if (uiState.relatedEntries.isNotEmpty()) {
            item {
                DetailRelatedEntriesBinding(
                    uiState.relatedEntries,
                    contentUiModel.relatedEntries,
                    onOpenRelatedEntry
                )
            }
        }
        item { DetailTagsBinding(entry, onAction) }
        item { DetailAssociationsBinding(entry, editState, onAction) }
        item { DetailNotesBinding(entry, editState, onAction) }
        item { MetadataSection(contentUiModel.metadata) }
        item { ActivityTimelineSection(activityList = contentUiModel.activities) }
    }

    DetailEditorOverlayBinding(uiState = uiState, onAction = onAction)
}
